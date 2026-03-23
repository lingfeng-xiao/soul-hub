package com.lingfeng.sprite.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.OwnerModel.ProactiveFeedback;
import com.lingfeng.sprite.OwnerModel.ProactiveFeedback.ResponseType;

/**
 * 反馈追踪服务 - S2-1: 主人响应追踪
 *
 * 功能：
 * 1. 记录发送的主动消息
 * 2. 检测主人是否对主动消息做出响应
 * 3. 判断响应类型（正向/负向/无响应）
 * 4. 存储反馈历史
 */
@Service
public class FeedbackTrackerService {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackTrackerService.class);

    // 主动消息等待回复的超时时间 (5分钟)
    private static final Duration RESPONSE_TIMEOUT = Duration.ofMinutes(5);

    // 反馈过期时间 (24小时)，超过后删除旧反馈
    private static final Duration FEEDBACK_EXPIRY = Duration.ofHours(24);

    // 检查无响应消息的间隔 (1分钟)
    private static final long CHECK_INTERVAL_SECONDS = 60;

    private final UnifiedContextService unifiedContextService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // 待追踪的主动消息 (messageId -> PendingMessage)
    private final Map<String, PendingMessage> pendingMessages = new ConcurrentHashMap<>();

    // 已完成的反馈历史
    private final Map<String, ProactiveFeedback> feedbackHistory = new ConcurrentHashMap<>();

    // 最后检测到主人活动的时间
    private volatile Instant lastOwnerActivityTime = Instant.now();

    public FeedbackTrackerService(
            @Autowired UnifiedContextService unifiedContextService
    ) {
        this.unifiedContextService = unifiedContextService;

        // 启动超时检查和主人活动检测
        startMonitoring();

        logger.info("FeedbackTrackerService started - response timeout: {} min", RESPONSE_TIMEOUT.toMinutes());
    }

    /**
     * 待追踪的消息
     */
    private static class PendingMessage {
        String messageId;
        Instant sentTime;
        String triggerType;
        String content;
        ResponseType response;
        Instant responseTime;
        String responseContent;
        float sentiment;

        PendingMessage(String messageId, String triggerType, String content) {
            this.messageId = messageId;
            this.sentTime = Instant.now();
            this.triggerType = triggerType;
            this.content = content;
            this.response = ResponseType.IGNORE;
        }

        ProactiveFeedback toProactiveFeedback() {
            return new ProactiveFeedback(
                messageId,
                sentTime,
                triggerType,
                content,
                response,
                responseTime,
                responseContent,
                sentiment
            );
        }
    }

    /**
     * 记录发送的主动消息
     */
    public void recordProactiveMessage(String messageId, String triggerType, String content) {
        PendingMessage pending = new PendingMessage(messageId, triggerType, content);
        pendingMessages.put(messageId, pending);
        logger.debug("Recorded proactive message: id={}, trigger={}", messageId, triggerType);

        // 清理过期反馈
        cleanupExpiredFeedback();
    }

    /**
     * 记录主人对主动消息的回复
     */
    public void recordResponse(String messageId, String responseContent, float sentiment) {
        PendingMessage pending = pendingMessages.remove(messageId);
        if (pending == null) {
            logger.debug("No pending message found for response: messageId={}", messageId);
            return;
        }

        pending.response = classifyResponse(sentiment);
        pending.responseTime = Instant.now();
        pending.responseContent = responseContent;
        pending.sentiment = sentiment;

        ProactiveFeedback feedback = pending.toProactiveFeedback();
        feedbackHistory.put(messageId, feedback);

        logger.info("Recorded proactive response: messageId={}, type={}, sentiment={}",
                messageId, pending.response, sentiment);

        // 更新主人交互历史
        updateInteractionHistory(feedback);
    }

    /**
     * 通知有新的主人活动（对话）
     * 由ConversationService调用，当检测到主人发起新对话时
     */
    public void notifyOwnerActivity() {
        lastOwnerActivityTime = Instant.now();

        // 检查是否有待追踪的主动消息
        if (!pendingMessages.isEmpty()) {
            // 认为主人在最近5分钟内的活动是对主动消息的回应
            Instant fiveMinutesAgo = Instant.now().minus(Duration.ofMinutes(5));
            for (PendingMessage pending : pendingMessages.values()) {
                if (pending.sentTime.isAfter(fiveMinutesAgo)) {
                    // 主人在主动消息后的5分钟内有了活动，认为是正向响应
                    pending.response = ResponseType.POSITIVE;
                    pending.responseTime = Instant.now();
                    pending.sentiment = 0.7f; // 默认积极

                    ProactiveFeedback feedback = pending.toProactiveFeedback();
                    feedbackHistory.put(pending.messageId, feedback);
                    pendingMessages.remove(pending.messageId);

                    logger.info("Owner responded to proactive message: id={}, type={}",
                            pending.messageId, pending.response);

                    updateInteractionHistory(feedback);
                    break; // 只处理最早的一条
                }
            }
        }
    }

    /**
     * 根据情感分类响应类型
     */
    private ResponseType classifyResponse(float sentiment) {
        if (sentiment >= 0.6f) {
            return ResponseType.POSITIVE;
        } else if (sentiment >= 0.4f) {
            return ResponseType.NEUTRAL;
        } else if (sentiment >= 0.2f) {
            return ResponseType.REPLY;
        } else {
            return ResponseType.REJECT;
        }
    }

    /**
     * 更新主人的交互历史
     */
    private void updateInteractionHistory(ProactiveFeedback feedback) {
        try {
            OwnerModel.InteractionType interactionType = switch (feedback.response()) {
                case POSITIVE -> OwnerModel.InteractionType.PROACTIVE_REPLY;
                case NEUTRAL, REPLY -> OwnerModel.InteractionType.FEEDBACK;
                case REJECT -> OwnerModel.InteractionType.PROACTIVE_REJECT;
                case IGNORE -> OwnerModel.InteractionType.PROACTIVE_IGNORE;
            };

            OwnerModel.Interaction interaction = new OwnerModel.Interaction(
                    feedback.sentTime(),
                    interactionType,
                    "主动消息: " + feedback.content(),
                    feedback.sentiment(),
                    feedback.triggerType(),
                    feedback.responseContent(),
                    null
            );

            // 这里可以调用WorldModel来更新主人的交互历史
            // 目前只是记录日志
            logger.debug("Would update interaction history with: type={}, sentiment={}",
                    interactionType, feedback.sentiment());

        } catch (Exception e) {
            logger.warn("Failed to update interaction history: {}", e.getMessage());
        }
    }

    /**
     * 获取反馈统计
     */
    public FeedbackStats getStats() {
        int total = feedbackHistory.size();
        int positive = 0;
        int negative = 0;
        int ignored = 0;
        int neutral = 0;

        for (ProactiveFeedback fb : feedbackHistory.values()) {
            switch (fb.response()) {
                case POSITIVE -> positive++;
                case REJECT -> negative++;
                case IGNORE -> ignored++;
                case NEUTRAL, REPLY -> neutral++;
            }
        }

        return new FeedbackStats(total, positive, negative, ignored, neutral);
    }

    /**
     * 获取最近的反馈历史
     */
    public List<ProactiveFeedback> getRecentFeedback(int limit) {
        return feedbackHistory.values().stream()
                .sorted((a, b) -> b.sentTime().compareTo(a.sentTime()))
                .limit(limit)
                .toList();
    }

    /**
     * 获取待追踪的消息数量
     */
    public int getPendingCount() {
        return pendingMessages.size();
    }

    /**
     * 启动监控
     */
    private void startMonitoring() {
        // 超时检查
        scheduler.scheduleAtFixedRate(
                this::checkTimeouts,
                CHECK_INTERVAL_SECONDS,
                CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * 检查超时的消息
     */
    private void checkTimeouts() {
        Instant now = Instant.now();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, PendingMessage> entry : pendingMessages.entrySet()) {
            PendingMessage pending = entry.getValue();
            Duration elapsed = Duration.between(pending.sentTime, now);
            if (elapsed.compareTo(RESPONSE_TIMEOUT) > 0) {
                // 超时，标记为忽略
                pending.response = ResponseType.IGNORE;
                pending.responseTime = now;

                ProactiveFeedback feedback = pending.toProactiveFeedback();
                feedbackHistory.put(entry.getKey(), feedback);
                toRemove.add(entry.getKey());

                logger.debug("Proactive message ignored (timeout): messageId={}", entry.getKey());
            }
        }

        for (String key : toRemove) {
            pendingMessages.remove(key);
        }
    }

    /**
     * 清理过期的反馈
     */
    private void cleanupExpiredFeedback() {
        Instant expiryThreshold = Instant.now().minus(FEEDBACK_EXPIRY);
        feedbackHistory.entrySet().removeIf(entry ->
                entry.getValue().sentTime().isBefore(expiryThreshold));
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        scheduler.shutdown();
        logger.info("FeedbackTrackerService shutdown complete");
    }

    /**
     * 反馈统计
     */
    public record FeedbackStats(
            int total,
            int positive,
            int negative,
            int ignored,
            int neutral
    ) {
        public float getPositiveRate() {
            return total > 0 ? (float) positive / total : 0f;
        }

        public float getIgnoredRate() {
            return total > 0 ? (float) ignored / total : 0f;
        }
    }
}
