package com.lingfeng.sprite.skill.builtin;

import com.lingfeng.sprite.skill.Skill;
import com.lingfeng.sprite.skill.SkillContext;
import com.lingfeng.sprite.skill.SkillResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Skill for making HTTP requests
 */
public class HttpRequestSkill implements Skill {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestSkill.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    @Override
    public String id() {
        return "http-request-v1";
    }

    @Override
    public String name() {
        return "HTTP Requests";
    }

    @Override
    public String description() {
        return "Make HTTP requests to external APIs";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public List<String> triggers() {
        return List.of("http", "fetch", "api", "request", "curl", "web");
    }

    @Override
    public List<SkillParameter> parameters() {
        return List.of(
            new SkillParameter("url", "string", true, "URL to request", null),
            new SkillParameter("method", "enum", false, "GET|POST|PUT|DELETE", "GET"),
            new SkillParameter("headers", "string", false, "JSON headers", null),
            new SkillParameter("body", "string", false, "Request body", null),
            new SkillParameter("timeout", "integer", false, "Timeout in seconds", "30")
        );
    }

    @Override
    public SkillResult execute(SkillContext context) {
        long start = System.currentTimeMillis();

        String url = (String) context.parameters().get("url");
        if (url == null || url.isBlank()) {
            return SkillResult.failure("URL is required");
        }

        String method = (String) context.parameters().getOrDefault("method", "GET");
        String headersStr = (String) context.parameters().get("headers");
        String body = (String) context.parameters().get("body");

        // Parse timeout
        int timeoutSec = 30;
        Object timeoutParam = context.parameters().get("timeout");
        if (timeoutParam instanceof Number) {
            timeoutSec = ((Number) timeoutParam).intValue();
        } else if (timeoutParam instanceof String) {
            try {
                timeoutSec = Integer.parseInt((String) timeoutParam);
            } catch (NumberFormatException ignored) {}
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSec, 120)))
                .build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_TIMEOUT);

            // Set method
            switch (method.toUpperCase()) {
                case "GET" -> requestBuilder.GET();
                case "POST" -> {
                    if (body != null) {
                        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
                    } else {
                        requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
                    }
                }
                case "PUT" -> {
                    if (body != null) {
                        requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
                    } else {
                        requestBuilder.PUT(HttpRequest.BodyPublishers.noBody());
                    }
                }
                case "DELETE" -> requestBuilder.DELETE();
                default -> {
                    long duration = System.currentTimeMillis() - start;
                    return SkillResult.failure("Unsupported HTTP method: " + method).withDuration(duration);
                }
            }

            // Parse and set headers
            if (headersStr != null && !headersStr.isBlank()) {
                try {
                    Map<String, String> headers = parseHeaders(headersStr);
                    headers.forEach(requestBuilder::header);
                } catch (Exception e) {
                    logger.warn("Failed to parse headers: {}", headersStr);
                }
            }

            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            long duration = System.currentTimeMillis() - start;

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("statusCode", response.statusCode());
            resultData.put("statusText", statusText(response.statusCode()));
            resultData.put("body", response.body());
            resultData.put("headers", response.headers().map());

            return SkillResult.success(
                "HTTP " + method + " " + response.statusCode(),
                resultData
            ).withDuration(duration)
             .withMetadata("method", method)
             .withMetadata("status", String.valueOf(response.statusCode()));

        } catch (IOException e) {
            long duration = System.currentTimeMillis() - start;
            logger.error("IO error for URL {}: {}", url, e.getMessage());
            return SkillResult.failure("IO Error: " + e.getMessage()).withDuration(duration);
        } catch (InterruptedException e) {
            long duration = System.currentTimeMillis() - start;
            Thread.currentThread().interrupt();
            return SkillResult.failure("Request interrupted").withDuration(duration);
        } catch (IllegalArgumentException e) {
            long duration = System.currentTimeMillis() - start;
            return SkillResult.failure("Invalid URL: " + e.getMessage()).withDuration(duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            logger.error("Error for URL {}: {}", url, e.getMessage());
            return SkillResult.failure("Error: " + e.getMessage()).withDuration(duration);
        }
    }

    private Map<String, String> parseHeaders(String headersStr) {
        Map<String, String> headers = new HashMap<>();
        try {
            // Simple JSON parsing for headers
            String json = headersStr.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);
                String[] pairs = json.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        String key = kv[0].trim().replace("\"", "");
                        String value = kv[1].trim().replace("\"", "");
                        headers.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse headers JSON: {}", headersStr);
        }
        return headers;
    }

    private String statusText(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "Unknown";
        };
    }
}
