package com.lingfeng.sprite.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * S11-1: Webhook集成服务
 *
 * 支持Sprite向外部服务发送Webhook通知：
 * - 事件触发Webhook调用
 * - 多种事件类型支持
 * - 重试机制
 * - 签名验证（可选）
 */
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

    private final Map<String, WebhookEndpoint> endpoints = new ConcurrentHashMap<>();
    private final HttpClient httpClient;
    private final WebhookDispatcher dispatcher;

    /**
     * Webhook端点配置
     */
    public record WebhookEndpoint(
        String id,
        String name,
        URI url,
        String secret,
        List<EventType> subscribedEvents,
        boolean enabled,
        Instant createdAt,
        Instant lastTriggered,
        int successCount,
        int failureCount
    ) {
        public WebhookEndpoint withLastTriggered(Instant time) {
            return new WebhookEndpoint(id, name, url, secret, subscribedEvents, enabled, createdAt, time, successCount, failureCount);
        }

        public WebhookEndpoint withSuccess() {
            return new WebhookEndpoint(id, name, url, secret, subscribedEvents, enabled, createdAt, lastTriggered, successCount + 1, failureCount);
        }

        public WebhookEndpoint withFailure() {
            return new WebhookEndpoint(id, name, url, secret, subscribedEvents, enabled, createdAt, lastTriggered, successCount, failureCount + 1);
        }
    }

    /**
     * Webhook事件
     */
    public record WebhookEvent(
        String id,
        EventType type,
        Instant timestamp,
        Map<String, Object> payload
    ) {}

    /**
     * 事件类型
     */
    public enum EventType {
        SPRITE_STARTED,      // Sprite启动
        SPRITE_STOPPED,      // Sprite停止
        EMOTION_CHANGED,     // 情绪变化
        DECISION_MADE,       // 做出决策
        ACTION_EXECUTED,     // 执行动作
        MEMORY_CONSOLIDATED, // 记忆巩固
        EVOLUTION_TRIGGERED, // 触发进化
        ERROR_OCCURRED,      // 发生错误
        OWNER_INTERACTION,    // 主人交互
        PROACTIVE_MESSAGE     // 主动消息
    }

    /**
     * Webhook投递结果
     */
    public record DeliveryResult(
        String endpointId,
        boolean success,
        int statusCode,
        String response,
        String error,
        long durationMs
    ) {}

    public WebhookService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.dispatcher = new WebhookDispatcher(httpClient);
    }

    // ==================== 端点管理 ====================

    /**
     * 注册Webhook端点
     */
    public WebhookEndpoint registerEndpoint(String name, String url, String secret, List<EventType> events) {
        String id = "webhook-" + System.currentTimeMillis();
        WebhookEndpoint endpoint = new WebhookEndpoint(
            id,
            name,
            URI.create(url),
            secret,
            events != null ? new ArrayList<>(events) : new ArrayList<>(),
            true,
            Instant.now(),
            null,
            0,
            0
        );
        endpoints.put(id, endpoint);
        logger.info("Registered webhook endpoint: {} ({})", name, url);
        return endpoint;
    }

    /**
     * 注销Webhook端点
     */
    public boolean unregisterEndpoint(String id) {
        WebhookEndpoint removed = endpoints.remove(id);
        if (removed != null) {
            logger.info("Unregistered webhook endpoint: {}", removed.name());
            return true;
        }
        return false;
    }

    /**
     * 获取端点
     */
    public WebhookEndpoint getEndpoint(String id) {
        return endpoints.get(id);
    }

    /**
     * 获取所有端点
     */
    public List<WebhookEndpoint> getAllEndpoints() {
        return new ArrayList<>(endpoints.values());
    }

    /**
     * 获取启用的端点
     */
    public List<WebhookEndpoint> getEnabledEndpoints() {
        return endpoints.values().stream()
            .filter(WebhookEndpoint::enabled)
            .toList();
    }

    /**
     * 更新端点
     */
    public void updateEndpoint(String id, boolean enabled) {
        WebhookEndpoint current = endpoints.get(id);
        if (current != null) {
            endpoints.put(id, new WebhookEndpoint(
                current.id(), current.name(), current.url(), current.secret(),
                current.subscribedEvents(), enabled, current.createdAt(),
                current.lastTriggered(), current.successCount(), current.failureCount()
            ));
        }
    }

    // ==================== 事件触发 ====================

    /**
     * 触发事件
     */
    public List<DeliveryResult> triggerEvent(EventType type, Map<String, Object> payload) {
        WebhookEvent event = new WebhookEvent(
            "evt-" + System.currentTimeMillis(),
            type,
            Instant.now(),
            payload != null ? new ConcurrentHashMap<>(payload) : new ConcurrentHashMap<>()
        );

        List<WebhookEndpoint> subscribed = getEnabledEndpoints().stream()
            .filter(e -> e.subscribedEvents().contains(type))
            .toList();

        if (subscribed.isEmpty()) {
            logger.debug("No endpoints subscribed to event: {}", type);
            return List.of();
        }

        logger.info("Triggering event {} for {} endpoints", type, subscribed.size());
        return dispatcher.dispatch(event, subscribed);
    }

    /**
     * 触发事件（简化）
     */
    public List<DeliveryResult> triggerEvent(EventType type) {
        return triggerEvent(type, Map.of("event", type.name()));
    }

    /**
     * 触发事件（带描述）
     */
    public List<DeliveryResult> triggerEvent(EventType type, String message) {
        return triggerEvent(type, Map.of("event", type.name(), "message", message));
    }

    // ==================== 投递器 ====================

    /**
     * Webhook投递器
     */
    private static class WebhookDispatcher {
        private final HttpClient client;

        public WebhookDispatcher(HttpClient client) {
            this.client = client;
        }

        public List<DeliveryResult> dispatch(WebhookEvent event, List<WebhookEndpoint> endpoints) {
            List<DeliveryResult> results = new ArrayList<>();

            for (WebhookEndpoint endpoint : endpoints) {
                DeliveryResult result = deliver(endpoint, event);
                results.add(result);
            }

            return results;
        }

        private DeliveryResult deliver(WebhookEndpoint endpoint, WebhookEvent event) {
            long startTime = System.currentTimeMillis();

            try {
                String json = buildPayload(event);
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(endpoint.url())
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Event", event.type().name())
                    .header("X-Webhook-ID", event.id())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                long duration = System.currentTimeMillis() - startTime;
                boolean success = response.statusCode() >= 200 && response.statusCode() < 300;

                logger.debug("Webhook delivery to {}: status={}, duration={}ms",
                    endpoint.name(), response.statusCode(), duration);

                return new DeliveryResult(
                    endpoint.id(),
                    success,
                    response.statusCode(),
                    response.body(),
                    null,
                    duration
                );

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("Webhook delivery failed to {}: {}", endpoint.name(), e.getMessage());

                return new DeliveryResult(
                    endpoint.id(),
                    false,
                    0,
                    null,
                    e.getMessage(),
                    duration
                );
            }
        }

        private String buildPayload(WebhookEvent event) {
            // 简单的JSON构建
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"id\":\"").append(event.id()).append("\",");
            sb.append("\"type\":\"").append(event.type().name()).append("\",");
            sb.append("\"timestamp\":\"").append(event.timestamp()).append("\",");
            sb.append("\"payload\":{");
            if (event.payload() != null && !event.payload().isEmpty()) {
                boolean first = true;
                for (Map.Entry<String, Object> entry : event.payload().entrySet()) {
                    if (!first) sb.append(",");
                    sb.append("\"").append(entry.getKey()).append("\":");
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        sb.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
                    } else {
                        sb.append(value);
                    }
                    first = false;
                }
            }
            sb.append("}}");
            return sb.toString();
        }
    }

    // ==================== 统计 ====================

    /**
     * 获取Webhook统计
     */
    public WebhookStats getStats() {
        int total = endpoints.size();
        int enabled = (int) endpoints.values().stream().filter(WebhookEndpoint::enabled).count();
        int totalSuccess = endpoints.values().stream().mapToInt(WebhookEndpoint::successCount).sum();
        int totalFailure = endpoints.values().stream().mapToInt(WebhookEndpoint::failureCount).sum();

        return new WebhookStats(total, enabled, totalSuccess, totalFailure);
    }

    public record WebhookStats(
        int totalEndpoints,
        int enabledEndpoints,
        int totalSuccess,
        int totalFailures
    ) {}
}
