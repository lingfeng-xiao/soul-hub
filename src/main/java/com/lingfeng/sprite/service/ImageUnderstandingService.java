package com.lingfeng.sprite.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingfeng.sprite.llm.MinMaxConfig;

/**
 * S23-2: 图像理解服务
 *
 * 提供图像分析和描述生成能力：
 * - 使用 MinMax LLM API 进行视觉理解
 * - 支持多种图像格式（JPEG, PNG, GIF, WebP）
 * - 提供图像元数据提取
 * - 智能缓存相似图像的分析结果
 * - 支持降级处理，当API不可用时使用启发式分析
 */
@Service
public class ImageUnderstandingService {

    private static final Logger logger = LoggerFactory.getLogger(ImageUnderstandingService.class);

    // 连续失败阈值，超过此值认为API不可用
    private static final int FAILURE_THRESHOLD = 3;
    // 恢复检查间隔（5分钟）
    private static final long RECOVERY_CHECK_INTERVAL_MINUTES = 5;
    // 缓存过期时间（1小时）
    private static final long CACHE_EXPIRY_MINUTES = 60;
    // 默认最大描述长度
    private static final int DEFAULT_MAX_DESCRIPTION_LENGTH = 500;
    // 支持的图像格式
    private static final List<String> SUPPORTED_FORMATS = List.of("JPEG", "PNG", "GIF", "WEBP");

    private final MinMaxConfig minMaxConfig;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final Map<String, CachedAnalysis> analysisCache;

    // 失败追踪
    private int consecutiveFailures = 0;
    private volatile boolean isDegraded = false;
    private Instant lastFailureTime = Instant.now();
    private final AtomicBoolean isAvailable = new AtomicBoolean(true);

    public ImageUnderstandingService(MinMaxConfig minMaxConfig) {
        this.minMaxConfig = minMaxConfig;
        this.objectMapper = new ObjectMapper();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(5);
        cm.setMaxPerRoute(new HttpRoute(new HttpHost("localhost", 8080)), 3);
        this.httpClient = HttpClients.custom().setConnectionManager(cm).build();
        this.analysisCache = new ConcurrentHashMap<>();
        logger.info("ImageUnderstandingService initialized with MinMax API: {}", minMaxConfig.baseUrl());
    }

    // ==================== Records ====================

    /**
     * S23-2: 图像分析选项
     */
    public record ImageAnalysisOptions(
        int maxDescriptionLength,
        boolean includeObjects,
        boolean includeScene
    ) {
        public ImageAnalysisOptions {
            if (maxDescriptionLength <= 0) maxDescriptionLength = DEFAULT_MAX_DESCRIPTION_LENGTH;
        }

        public static ImageAnalysisOptions defaultOptions() {
            return new ImageAnalysisOptions(DEFAULT_MAX_DESCRIPTION_LENGTH, true, true);
        }
    }

    /**
     * S23-2: 图像描述结果
     */
    public record ImageDescription(
        String description,
        float confidence,
        List<DetectedObject> detectedObjects,
        SceneType sceneType,
        String textContent
    ) {
        public ImageDescription {
            if (description == null) description = "";
            if (detectedObjects == null) detectedObjects = List.of();
            if (sceneType == null) sceneType = SceneType.UNKNOWN;
            if (textContent == null) textContent = "";
        }

        public static ImageDescription fallback(String message) {
            return new ImageDescription(message, 0f, List.of(), SceneType.UNKNOWN, "");
        }
    }

    /**
     * S23-2: 检测到的对象
     */
    public record DetectedObject(
        String label,
        float confidence,
        BoundingBox boundingBox
    ) {
        public DetectedObject {
            if (label == null) label = "unknown";
        }
    }

    /**
     * S23-2: 边界框
     */
    public record BoundingBox(
        float x,
        float y,
        float width,
        float height
    ) {}

    /**
     * S23-2: 场景类型
     */
    public enum SceneType {
        UNKNOWN,
        INDOOR,
        OUTDOOR,
        OFFICE,
        NATURE,
        URBAN,
        PERSON,
        ANIMAL,
        FOOD,
        TEXT,
        SCREEN,
        DOCUMENT
    }

    /**
     * S23-2: 图像元数据
     */
    public record ImageMetadata(
        int width,
        int height,
        String format,
        long size,
        Instant timestamp
    ) {
        public ImageMetadata {
            if (timestamp == null) timestamp = Instant.now();
        }
    }

    /**
     * 缓存的分析结果
     */
    private record CachedAnalysis(
        ImageDescription description,
        ImageMetadata metadata,
        Instant expiresAt
    ) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    // ==================== 主接口 ====================

    /**
     * S23-2: 分析图像
     *
     * @param imageData 图像数据（字节数组）
     * @param options 分析选项
     * @return 图像描述结果
     */
    public ImageDescription analyzeImage(byte[] imageData, ImageAnalysisOptions options) {
        if (imageData == null || imageData.length == 0) {
            logger.warn("Empty image data provided for analysis");
            return ImageDescription.fallback("无法分析空图像数据");
        }

        // 检查缓存
        String cacheKey = generateCacheKey(imageData);
        CachedAnalysis cached = analysisCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            logger.debug("Cache hit for image analysis");
            return cached.description();
        }

        // 提取元数据
        ImageMetadata metadata = extractMetadata(imageData);

        try {
            // 尝试使用MinMax API进行分析
            ImageDescription description = analyzeWithMinMaxApi(imageData, options);

            // 缓存结果
            analysisCache.put(cacheKey, new CachedAnalysis(
                description,
                metadata,
                Instant.now().plusSeconds(CACHE_EXPIRY_MINUTES * 60)
            ));

            onApiSuccess();
            return description;
        } catch (Exception e) {
            logger.warn("MinMax API analysis failed, using fallback: {}", e.getMessage());
            onApiFailure();

            // 使用降级分析
            ImageDescription fallbackDescription = analyzeWithHeuristics(imageData, options, metadata);
            return fallbackDescription;
        }
    }

    /**
     * S23-2: 生成图像描述（便捷方法）
     *
     * @param imageData 图像数据
     * @return 图像的文字描述
     */
    public String generateDescription(byte[] imageData) {
        ImageDescription description = analyzeImage(imageData, ImageAnalysisOptions.defaultOptions());
        return description.description();
    }

    /**
     * S23-2: 提取图像元数据
     *
     * @param imageData 图像数据
     * @return 图像元数据
     */
    public ImageMetadata extractMetadata(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return new ImageMetadata(0, 0, "UNKNOWN", 0, Instant.now());
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) {
                return new ImageMetadata(0, 0, "UNKNOWN", imageData.length, Instant.now());
            }

            String format = detectImageFormat(imageData);
            return new ImageMetadata(
                image.getWidth(),
                image.getHeight(),
                format,
                imageData.length,
                Instant.now()
            );
        } catch (IOException e) {
            logger.warn("Failed to extract image metadata: {}", e.getMessage());
            return new ImageMetadata(0, 0, "UNKNOWN", imageData.length, Instant.now());
        }
    }

    /**
     * 检查服务可用性
     *
     * @return true if service is available
     */
    public boolean isAvailable() {
        checkForRecovery();
        return isAvailable.get() && !isDegraded;
    }

    // ==================== MinMax API 分析 ====================

    /**
     * 使用 MinMax API 进行图像分析
     */
    private ImageDescription analyzeWithMinMaxApi(byte[] imageData, ImageAnalysisOptions options) throws Exception {
        String base64Image = Base64.getEncoder().encodeToString(imageData);

        String prompt = buildVisionPrompt(options);

        HttpPost request = new HttpPost(minMaxConfig.baseUrl() + "/text/chatcompletion_v2");
        request.setHeader("Authorization", "Bearer " + minMaxConfig.apiKey());
        request.setHeader("Content-Type", "application/json");

        // 构建多模态消息
        String jsonBody = buildMultimodalRequest(base64Image, prompt);
        request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String body = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);

            JsonNode jsonResponse = objectMapper.readTree(body);
            JsonNode choices = jsonResponse.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                String content = message.path("content").asText("");

                return parseApiResponse(content, options);
            }
        }

        throw new RuntimeException("Invalid API response structure");
    }

    /**
     * 构建多模态请求
     */
    private String buildMultimodalRequest(String base64Image, String prompt) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "model", minMaxConfig.model(),
                "messages", List.of(
                    Map.of(
                        "role", "user",
                        "content", List.of(
                            Map.of("type", "text", "text", prompt),
                            Map.of("type", "image_url", "image_url",
                                Map.of("url", "data:image/jpeg;base64," + base64Image))
                        )
                    )
                )
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build multimodal request", e);
        }
    }

    /**
     * 构建视觉分析提示词
     */
    private String buildVisionPrompt(ImageAnalysisOptions options) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的图像分析助手。请分析这张图片并提供详细描述。\n\n");

        if (options.includeScene()) {
            prompt.append("1. 场景类型：判断图片是室内还是室外，什么样的场景（办公室、自然、城市等）\n");
        }
        if (options.includeObjects()) {
            prompt.append("2. 主要物体：列出图片中主要的对象和它们的位置\n");
        }
        prompt.append("3. 详细描述：用").append(options.maxDescriptionLength()).append("字以内描述图片内容\n");
        prompt.append("4. 文字内容：如果图片中有文字，请识别出来\n");
        prompt.append("5. 置信度：评估你分析的置信度（0-1）\n\n");
        prompt.append("请以JSON格式返回，字段包括：description, confidence(0-1), detected_objects[{label, confidence, bbox{x,y,w,h}}], scene_type, text_content");

        return prompt.toString();
    }

    /**
     * 解析API响应
     */
    private ImageDescription parseApiResponse(String content, ImageAnalysisOptions options) {
        try {
            String jsonStr = extractJson(content);
            JsonNode jsonResponse = objectMapper.readTree(jsonStr);

            String description = jsonResponse.path("description").asText("无法生成描述");
            float confidence = (float) jsonResponse.path("confidence").asDouble(0.5);

            List<DetectedObject> objects = new ArrayList<>();
            JsonNode objectsNode = jsonResponse.path("detected_objects");
            if (objectsNode.isArray()) {
                for (JsonNode obj : objectsNode) {
                    String label = obj.path("label").asText("unknown");
                    float objConf = (float) obj.path("confidence").asDouble(0.5);
                    JsonNode bbox = obj.path("bbox");
                    BoundingBox box = new BoundingBox(
                        (float) bbox.path("x").asDouble(0),
                        (float) bbox.path("y").asDouble(0),
                        (float) bbox.path("w").asDouble(0),
                        (float) bbox.path("h").asDouble(0)
                    );
                    objects.add(new DetectedObject(label, objConf, box));
                }
            }

            SceneType sceneType = parseSceneType(jsonResponse.path("scene_type").asText("UNKNOWN"));
            String textContent = jsonResponse.path("text_content").asText("");

            // 截断描述到指定长度
            if (description.length() > options.maxDescriptionLength()) {
                description = description.substring(0, options.maxDescriptionLength()) + "...";
            }

            return new ImageDescription(description, confidence, objects, sceneType, textContent);
        } catch (Exception e) {
            logger.warn("Failed to parse API response: {}", e.getMessage());
            return new ImageDescription(content, 0.3f, List.of(), SceneType.UNKNOWN, "");
        }
    }

    /**
     * 解析场景类型
     */
    private SceneType parseSceneType(String type) {
        if (type == null) return SceneType.UNKNOWN;
        return switch (type.toUpperCase()) {
            case "INDOOR" -> SceneType.INDOOR;
            case "OUTDOOR" -> SceneType.OUTDOOR;
            case "OFFICE" -> SceneType.OFFICE;
            case "NATURE" -> SceneType.NATURE;
            case "URBAN" -> SceneType.URBAN;
            case "PERSON" -> SceneType.PERSON;
            case "ANIMAL" -> SceneType.ANIMAL;
            case "FOOD" -> SceneType.FOOD;
            case "TEXT", "DOCUMENT" -> SceneType.DOCUMENT;
            case "SCREEN" -> SceneType.SCREEN;
            default -> SceneType.UNKNOWN;
        };
    }

    // ==================== 降级分析（启发式） ====================

    /**
     * 使用启发式方法分析图像（当API不可用时）
     */
    private ImageDescription analyzeWithHeuristics(byte[] imageData, ImageAnalysisOptions options, ImageMetadata metadata) {
        logger.info("Using heuristic analysis for image");

        // 基于图像特征的启发式分析
        StringBuilder description = new StringBuilder();

        // 分析图像尺寸
        if (metadata.width() > 0 && metadata.height() > 0) {
            float aspectRatio = (float) metadata.width() / metadata.height();
            if (aspectRatio > 1.5f) {
                description.append("这是一张横向图像（宽高比 ").append(String.format("%.2f", aspectRatio)).append("）");
            } else if (aspectRatio < 0.7f) {
                description.append("这是一张纵向图像");
            } else {
                description.append("这是一张接近正方形的图像");
            }

            // 基于尺寸的推断
            if (metadata.width() > 1920 || metadata.height() > 1080) {
                description.append("，分辨率较高");
            } else if (metadata.width() < 320 || metadata.height() < 320) {
                description.append("，分辨率较低或缩略图");
            }
        }

        // 分析文件大小
        if (metadata.size() > 1024 * 1024) {
            description.append("，文件较大（").append(String.format("%.1f", metadata.size() / 1024.0 / 1024.0)).append("MB）");
        } else if (metadata.size() < 10 * 1024) {
            description.append("，文件很小");
        }

        // 分析格式
        description.append("，格式为").append(metadata.format());

        // 添加场景类型推断（基于尺寸和格式的启发式规则）
        SceneType inferredScene = inferSceneFromMetadata(metadata);
        description.append("。场景类型：").append(inferredScene);

        String textContent = "";
        if ("PNG".equals(metadata.format()) || "GIF".equals(metadata.format())) {
            // PNG和GIF更可能有文字或UI元素
            textContent = "[启发式分析：无法识别具体文字内容]";
        }

        return new ImageDescription(
            description.toString(),
            0.3f,
            List.of(),
            inferredScene,
            textContent
        );
    }

    /**
     * 根据元数据推断场景类型
     */
    private SceneType inferSceneFromMetadata(ImageMetadata metadata) {
        // 基于图像尺寸的启发式推断
        if (metadata.width() > 1920 && metadata.height() > 1080) {
            // 高分辨率可能是照片或屏幕截图
            return SceneType.SCREEN;
        } else if (metadata.width() <= 800 && metadata.height() <= 800) {
            // 小图像可能是图标或缩略图
            return SceneType.DOCUMENT;
        }
        return SceneType.UNKNOWN;
    }

    // ==================== 工具方法 ====================

    /**
     * 检测图像格式
     */
    private String detectImageFormat(byte[] data) {
        if (data == null || data.length < 4) {
            return "UNKNOWN";
        }

        // JPEG: FF D8 FF
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF) {
            return "JPEG";
        }
        // PNG: 89 50 4E 47
        if (data[0] == (byte) 0x89 && data[1] == (byte) 0x50 && data[2] == (byte) 0x4E && data[3] == (byte) 0x47) {
            return "PNG";
        }
        // GIF: 47 49 46 38
        if (data[0] == (byte) 0x47 && data[1] == (byte) 0x49 && data[2] == (byte) 0x46 && data[3] == (byte) 0x38) {
            return "GIF";
        }
        // WebP: RIFF....WEBP
        if (data[0] == (byte) 0x52 && data[1] == (byte) 0x49 && data[2] == (byte) 0x46 && data[3] == (byte) 0x46) {
            if (data.length >= 12 && data[8] == (byte) 0x57 && data[9] == (byte) 0x45 && data[10] == (byte) 0x42 && data[11] == (byte) 0x50) {
                return "WEBP";
            }
        }

        return "UNKNOWN";
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(byte[] imageData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(imageData);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(imageData.hashCode());
        }
    }

    /**
     * 从响应中提取JSON
     */
    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        } else {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                trimmed = trimmed.substring(start, end + 1);
            }
        }
        return trimmed.trim();
    }

    /**
     * API调用成功时调用
     */
    private void onApiSuccess() {
        consecutiveFailures = 0;
        if (isDegraded) {
            logger.info("ImageUnderstanding API recovered from degraded state");
            isDegraded = false;
        }
        isAvailable.set(true);
    }

    /**
     * API调用失败时调用
     */
    private void onApiFailure() {
        consecutiveFailures++;
        lastFailureTime = Instant.now();

        if (consecutiveFailures >= FAILURE_THRESHOLD && !isDegraded) {
            isDegraded = true;
            isAvailable.set(false);
            logger.warn("ImageUnderstanding API entered degraded state after {} consecutive failures", consecutiveFailures);
        }
    }

    /**
     * 检查是否需要恢复
     */
    private void checkForRecovery() {
        if (!isDegraded) {
            return;
        }

        Duration sinceLastFailure = Duration.between(lastFailureTime, Instant.now());
        if (sinceLastFailure.toMinutes() >= RECOVERY_CHECK_INTERVAL_MINUTES) {
            logger.info("Attempting to recover from degraded state...");
            isDegraded = false;
            isAvailable.set(true);
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        analysisCache.clear();
        logger.info("Image analysis cache cleared");
    }

    /**
     * 获取缓存统计
     */
    public CacheStats getCacheStats() {
        int totalEntries = analysisCache.size();
        int validEntries = (int) analysisCache.values().stream().filter(c -> !c.isExpired()).count();
        return new CacheStats(totalEntries, validEntries);
    }

    public record CacheStats(
        int totalEntries,
        int validEntries
    ) {}

    /**
     * 关闭服务（清理资源）
     */
    public void close() {
        try {
            httpClient.close();
        } catch (Exception e) {
            logger.warn("Error closing HTTP client: {}", e.getMessage());
        }
    }
}
