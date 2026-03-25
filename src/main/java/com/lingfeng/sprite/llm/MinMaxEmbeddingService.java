package com.lingfeng.sprite.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingfeng.sprite.config.AppConfig;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * MinMax Embedding Service implementation.
 * Calls MinMax API to generate text embeddings.
 *
 * API Endpoint: https://api.minimax.chat/v1/text/embeddings
 * Request Body: {"model":"embo-01","texts":["hello"]}
 * Response: {"model":"embo-01","texts":[{"text":"hello","embedding":[0.1,0.2,...]}]}
 */
@Service
public class MinMaxEmbeddingService implements LlmEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(MinMaxEmbeddingService.class);

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    public MinMaxEmbeddingService(AppConfig appConfig, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    public EmbeddingResult embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new EmbeddingResult("embo-01", List.of());
        }

        String apiKey = appConfig.getLlm().getMinmax().getApiKey();
        String baseUrl = appConfig.getLlm().getMinmax().getBaseUrl();
        String endpoint = baseUrl + "/text/embeddings";

        try {
            // Build request
            HttpPost request = new HttpPost(endpoint);
            request.setHeader("Authorization", "Bearer " + apiKey);
            request.setHeader("Content-Type", "application/json");

            // Build request body
            String requestBody = buildRequestBody(texts);
            request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody;
                try {
                    responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                } catch (org.apache.hc.core5.http.ParseException e) {
                    throw new RuntimeException("Failed to parse response: " + e.getMessage(), e);
                }

                if (response.getCode() != 200) {
                    log.error("MinMax API error: {} - {}", response.getCode(), responseBody);
                    throw new RuntimeException("MinMax API error: " + response.getCode());
                }

                return parseResponse(responseBody);
            }

        } catch (IOException e) {
            log.error("Failed to call MinMax embedding API", e);
            throw new RuntimeException("Failed to call MinMax embedding API", e);
        }
    }

    /**
     * Build request body for MinMax embedding API.
     */
    private String buildRequestBody(List<String> texts) throws IOException {
        String model = "embo-01";

        // Build JSON manually for correct formatting
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(model).append("\",\"texts\":[");
        for (int i = 0; i < texts.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(texts.get(i))).append("\"");
        }
        sb.append("]}");

        return sb.toString();
    }

    /**
     * Parse MinMax API response.
     */
    private EmbeddingResult parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        String model = root.has("model") ? root.get("model").asText() : "embo-01";
        JsonNode textsNode = root.get("texts");

        List<TextEmbedding> embeddings = new ArrayList<>();
        if (textsNode != null && textsNode.isArray()) {
            for (JsonNode item : textsNode) {
                String text = item.has("text") ? item.get("text").asText() : "";
                List<Double> embedding = new ArrayList<>();

                JsonNode embeddingNode = item.get("embedding");
                if (embeddingNode != null && embeddingNode.isArray()) {
                    for (JsonNode value : embeddingNode) {
                        embedding.add(value.asDouble());
                    }
                }

                embeddings.add(new TextEmbedding(text, embedding));
            }
        }

        return new EmbeddingResult(model, embeddings);
    }

    /**
     * Escape special characters for JSON string.
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
