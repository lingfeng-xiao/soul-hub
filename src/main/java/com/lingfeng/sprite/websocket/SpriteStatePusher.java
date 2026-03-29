package com.lingfeng.sprite.websocket;

import com.lingfeng.sprite.Sprite;
import com.lingfeng.sprite.service.SpriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Sprite State Pusher - 定期推送 Sprite 状态到所有订阅的 WebSocket 会话
 *
 * 提供实时状态更新：
 * - 认知状态
 * - 记忆状态
 * - 进化状态
 * - 情绪状态
 */
@Service
public class SpriteStatePusher {
    private static final Logger logger = LoggerFactory.getLogger(SpriteStatePusher.class);

    private final SpriteService spriteService;
    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> subscribedSessions = new CopyOnWriteArraySet<>();

    public SpriteStatePusher(SpriteService spriteService, ObjectMapper objectMapper) {
        this.spriteService = spriteService;
        this.objectMapper = objectMapper;
    }

    /**
     * 订阅状态更新
     */
    public void subscribe(WebSocketSession session) {
        subscribedSessions.add(session);
        logger.info("Session subscribed to state push: {}", session.getId());
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(WebSocketSession session) {
        subscribedSessions.remove(session);
        logger.info("Session unsubscribed from state push: {}", session.getId());
    }

    /**
     * 获取订阅会话数
     */
    public int getSubscriberCount() {
        return subscribedSessions.size();
    }

    /**
     * 定期推送状态更新 (每秒一次)
     */
    @Scheduled(fixedRate = 1000)
    public void pushStateUpdates() {
        if (subscribedSessions.isEmpty()) {
            return;
        }

        try {
            Sprite.State state = spriteService.getState();

            int longTermCount = state.memoryStatus().longTermStats().episodicCount +
                    state.memoryStatus().longTermStats().semanticCount +
                    state.memoryStatus().longTermStats().proceduralCount +
                    state.memoryStatus().longTermStats().perceptiveCount;

            Map<String, Object> stateData = Map.of(
                "identity", Map.of(
                    "evolutionLevel", state.identity().evolutionLevel(),
                    "evolutionCount", state.identity().evolutionCount(),
                    "isRunning", state.isRunning(),
                    "hasLlmSupport", state.hasLlmSupport()
                ),
                "platform", state.platform().name(),
                "memoryStatus", Map.of(
                    "sensoryCount", state.memoryStatus().sensoryStimuliCount(),
                    "workingMemoryUsed", state.memoryStatus().workingMemoryItems(),
                    "workingMemoryMax", 7,
                    "longTermCount", longTermCount
                ),
                "lastCycleTime", state.lastCycleTime() != null ?
                        state.lastCycleTime().toString() : "never",
                "timestamp", Instant.now().toString()
            );

            WebSocketMessage message = new WebSocketMessage("state_update", stateData);
            String json = objectMapper.writeValueAsString(message);

            for (WebSocketSession session : subscribedSessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(json));
                    } catch (Exception e) {
                        logger.warn("Failed to send to session {}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error pushing state updates: {}", e.getMessage());
        }
    }

    /**
     * 推送认知循环更新
     */
    @Scheduled(fixedRate = 5000)
    public void pushCognitionUpdates() {
        if (subscribedSessions.isEmpty()) {
            return;
        }

        try {
            var cognitionStats = spriteService.getCognitionStats();

            Map<String, Object> cognitionData = Map.of(
                "totalCycles", cognitionStats.totalCycles(),
                "avgSalience", cognitionStats.avgSalience(),
                "reflectionCount", cognitionStats.reflectionCount(),
                "worldModelFacts", cognitionStats.worldModelFacts(),
                "longTermMemories", cognitionStats.longTermMemories(),
                "timestamp", Instant.now().toString()
            );

            WebSocketMessage message = new WebSocketMessage("cognition_update", cognitionData);
            String json = objectMapper.writeValueAsString(message);

            for (WebSocketSession session : subscribedSessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(json));
                    } catch (Exception e) {
                        logger.warn("Failed to send cognition update to session {}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error pushing cognition updates: {}", e.getMessage());
        }
    }

    public record WebSocketMessage(String type, Object data) {}
}