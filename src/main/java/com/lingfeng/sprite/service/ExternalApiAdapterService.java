package com.lingfeng.sprite.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * S11-2: 外部API适配器服务 - S13-2: 添加Spring服务注解
 *
 * 提供统一的外部API调用接口：
 * - 天气API
 * - 新闻API
 * - 日历API
 * - 提醒/闹钟API
 * - 搜索API
 *
 * 支持重试、超时、缓存
 */
@Service
public class ExternalApiAdapterService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalApiAdapterService.class);

    private final HttpClient httpClient;
    private final Map<String, ApiEndpoint> endpoints;
    private final Map<String, CachedResponse> cache;

    /**
     * API端点配置
     */
    public record ApiEndpoint(
        String id,
        String name,
        ApiType type,
        String baseUrl,
        String apiKey,
        Duration timeout,
        boolean enabled
    ) {}

    /**
     * API类型
     */
    public enum ApiType {
        WEATHER,
        NEWS,
        CALENDAR,
        REMINDER,
        SEARCH,
        TRANSLATION,
        CUSTOM
    }

    /**
     * API响应
     */
    public record ApiResponse(
        boolean success,
        int statusCode,
        String body,
        String error,
        long responseTimeMs,
        boolean fromCache
    ) {}

    /**
     * 缓存响应
     */
    private record CachedResponse(
        String data,
        Instant expiresAt
    ) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public ExternalApiAdapterService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.endpoints = new java.util.concurrent.ConcurrentHashMap<>();
        this.cache = new java.util.concurrent.ConcurrentHashMap<>();
        initializeDefaultEndpoints();
    }

    /**
     * 初始化默认端点
     */
    private void initializeDefaultEndpoints() {
        // 可以添加默认API端点配置
        logger.info("ExternalApiAdapterService initialized with {} endpoints", endpoints.size());
    }

    // ==================== 端点管理 ====================

    /**
     * 注册API端点
     */
    public void registerEndpoint(ApiEndpoint endpoint) {
        endpoints.put(endpoint.id(), endpoint);
        logger.info("Registered API endpoint: {} ({})", endpoint.name(), endpoint.type());
    }

    /**
     * 注销API端点
     */
    public void unregisterEndpoint(String id) {
        endpoints.remove(id);
    }

    /**
     * 获取端点
     */
    public ApiEndpoint getEndpoint(String id) {
        return endpoints.get(id);
    }

    // ==================== API调用 ====================

    /**
     * 调用天气API
     */
    public ApiResponse getWeather(String city) {
        return callApi(ApiType.WEATHER, "/current", Map.of("city", city));
    }

    /**
     * 调用新闻API
     */
    public ApiResponse getNews(String topic, int page, int pageSize) {
        return callApi(ApiType.NEWS, "/top-headlines",
            Map.of("topic", topic, "page", page, "pageSize", pageSize));
    }

    /**
     * 调用搜索API
     */
    public ApiResponse search(String query, int numResults) {
        return callApi(ApiType.SEARCH, "/search",
            Map.of("q", query, "num", numResults));
    }

    /**
     * 调用翻译API
     */
    public ApiResponse translate(String text, String targetLang) {
        return callApi(ApiType.TRANSLATION, "/translate",
            Map.of("text", text, "target", targetLang));
    }

    /**
     * 通用API调用
     */
    public ApiResponse callApi(ApiType type, String path, Map<String, Object> params) {
        // 查找对应类型的端点
        ApiEndpoint endpoint = endpoints.values().stream()
            .filter(e -> e.type() == type && e.enabled())
            .findFirst()
            .orElse(null);

        if (endpoint == null) {
            logger.warn("No endpoint configured for API type: {}", type);
            return new ApiResponse(false, 0, null, "No endpoint configured for " + type, 0, false);
        }

        // 检查缓存
        String cacheKey = type.name() + ":" + path + ":" + params.hashCode();
        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            logger.debug("Cache hit for: {}", cacheKey);
            return new ApiResponse(true, 200, cached.data(), null, 0, true);
        }

        // 构建URL
        String url = buildUrl(endpoint.baseUrl(), path, params);

        // 发送请求
        long startTime = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(endpoint.timeout() != null ? endpoint.timeout() : Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + endpoint.apiKey())
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long responseTime = System.currentTimeMillis() - startTime;

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;

            if (success) {
                // 缓存响应（默认5分钟）
                cache.put(cacheKey, new CachedResponse(response.body(), Instant.now().plusSeconds(300)));
            }

            return new ApiResponse(success, response.statusCode(), response.body(), null, responseTime, false);

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("API call failed for {}: {}", type, e.getMessage());
            return new ApiResponse(false, 0, null, e.getMessage(), responseTime, false);
        }
    }

    /**
     * 异步API调用
     */
    public CompletableFuture<ApiResponse> callApiAsync(ApiType type, String path, Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> callApi(type, path, params));
    }

    // ==================== 工具方法 ====================

    /**
     * 构建URL
     */
    private String buildUrl(String baseUrl, String path, Map<String, Object> params) {
        StringBuilder url = new StringBuilder(baseUrl);
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            url.append("/");
        }
        url.append(path);

        if (params != null && !params.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (!first) url.append("&");
                url.append(entry.getKey()).append("=").append(entry.getValue().toString());
                first = false;
            }
        }

        return url.toString();
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cache.clear();
        logger.info("API cache cleared");
    }

    /**
     * 清除指定类型的缓存
     */
    public void clearCache(ApiType type) {
        cache.entrySet().removeIf(entry -> entry.getKey().startsWith(type.name()));
    }

    /**
     * 获取统计信息
     */
    public ApiStats getStats() {
        int totalEndpoints = endpoints.size();
        int enabledEndpoints = (int) endpoints.values().stream().filter(ApiEndpoint::enabled).count();
        int cachedResponses = (int) cache.values().stream().filter(c -> !c.isExpired()).count();

        return new ApiStats(totalEndpoints, enabledEndpoints, cache.size(), cachedResponses);
    }

    public record ApiStats(
        int totalEndpoints,
        int enabledEndpoints,
        int totalCached,
        int validCached
    ) {}
}
