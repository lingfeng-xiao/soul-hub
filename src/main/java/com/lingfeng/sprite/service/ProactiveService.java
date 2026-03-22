package com.lingfeng.sprite.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.PerceptionSystem.PresenceStatus;
import com.lingfeng.sprite.PerceptionSystem.UserPerception;
import com.lingfeng.sprite.WorldModel;
import com.lingfeng.sprite.cognition.CognitionController;

/**
 * 主动对话服务
 *
 * 监控主人状态，在合适时机主动发起对话
 *
 * 触发条件：
 * - 长时间无操作（>30分钟）
 * - 检测到主人情绪变化
 * - 重要日程提醒
 * - 系统异常告警
 */
@Service
public class ProactiveService {

    private static final Logger logger = LoggerFactory.getLogger(ProactiveService.class);

    // 配置
    private static final long IDLE_CHECK_INTERVAL_SECONDS = 60;
    private static final long IDLE_THRESHOLD_MINUTES = 30;
    private static final long MOOD_CHECK_INTERVAL_SECONDS = 120;
    private static final float NEGATIVE_MOOD_THRESHOLD = -0.3f;

    private final UnifiedContextService unifiedContextService;
    private final ConversationService conversationService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // 状态跟踪
    private final ConcurrentHashMap<String, Instant> lastActivityTime = new ConcurrentHashMap<>();
    private String lastMood = "平静";
    private Instant lastProactiveTime = Instant.now();
    private static final Duration PROACTIVE_COOLDOWN = Duration.ofMinutes(15);

    public ProactiveService(
            @Autowired UnifiedContextService unifiedContextService,
            @Autowired ConversationService conversationService
    ) {
        this.unifiedContextService = unifiedContextService;
        this.conversationService = conversationService;

        // 启动主动检查
        startProactiveMonitoring();
    }

    private void startProactiveMonitoring() {
        // 空闲检查
        scheduler.scheduleAtFixedRate(
            this::checkIdleStatus,
            IDLE_CHECK_INTERVAL_SECONDS,
            IDLE_CHECK_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );

        // 情绪检查
        scheduler.scheduleAtFixedRate(
            this::checkMoodChanges,
            MOOD_CHECK_INTERVAL_SECONDS,
            MOOD_CHECK_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );

        logger.info("ProactiveService started - idle threshold: {} min, proactive cooldown: {} min",
                IDLE_THRESHOLD_MINUTES, PROACTIVE_COOLDOWN.toMinutes());
    }

    /**
     * 检查空闲状态
     */
    private void checkIdleStatus() {
        try {
            PerceptionSystem.Perception perception = unifiedContextService.getPerception();
            if (perception == null || perception.user() == null) {
                return;
            }

            UserPerception user = perception.user();
            PresenceStatus presence = user.presence();

            // 记录最后活动时间
            if (presence == PresenceStatus.ACTIVE) {
                lastActivityTime.put("user", Instant.now());
            }

            // 检查是否空闲超时
            Instant lastActivity = lastActivityTime.get("user");
            if (lastActivity == null) {
                lastActivity = Instant.now();
            }

            Duration idleTime = Duration.between(lastActivity, Instant.now());
            if (idleTime.toMinutes() >= IDLE_THRESHOLD_MINUTES) {
                // 用户空闲超过阈值
                if (shouldProactivelyContact()) {
                    triggerIdleProactiveMessage(idleTime.toMinutes());
                }
            }
        } catch (Exception e) {
            logger.debug("Error checking idle status: {}", e.getMessage());
        }
    }

    /**
     * 检查情绪变化
     */
    private void checkMoodChanges() {
        try {
            PerceptionSystem.Perception perception = unifiedContextService.getPerception();
            if (perception == null) {
                return;
            }

            WorldModel.World world = unifiedContextService.getWorldModel();
            if (world == null || world.owner() == null || world.owner().emotionalState() == null) {
                return;
            }

            String currentMood = world.owner().emotionalState().currentMood().name();

            // 检测情绪变化
            if (!currentMood.equals(lastMood) && !lastMood.equals("平静")) {
                // 情绪发生了变化，且之前不是平静状态
                logger.info("Mood changed from {} to {}", lastMood, currentMood);
                lastMood = currentMood;

                // 检查是否需要主动关心
                if (shouldProactivelyContact()) {
                    triggerMoodProactiveMessage(currentMood);
                }
            } else {
                lastMood = currentMood;
            }
        } catch (Exception e) {
            logger.debug("Error checking mood: {}", e.getMessage());
        }
    }

    /**
     * 判断是否应该主动联系
     */
    private boolean shouldProactivelyContact() {
        // 检查冷却时间
        Duration sinceLastProactive = Duration.between(lastProactiveTime, Instant.now());
        if (sinceLastProactive.compareTo(PROACTIVE_COOLDOWN) < 0) {
            return false;
        }

        // 检查主人是否在忙
        PerceptionSystem.Perception perception = unifiedContextService.getPerception();
        if (perception != null && perception.user() != null) {
            PresenceStatus presence = perception.user().presence();
            if (presence == PresenceStatus.ACTIVE) {
                // 主人在活跃状态，不打扰
                return false;
            }
        }

        return true;
    }

    /**
     * 触发空闲主动消息
     */
    private void triggerIdleProactiveMessage(long idleMinutes) {
        lastProactiveTime = Instant.now();

        String message;
        if (idleMinutes >= 60) {
            message = String.format("主人已经休息 %d 小时了，工作不要太累哦。有需要随时叫我。", idleMinutes / 60);
        } else {
            message = String.format("主人已经空闲 %d 分钟了，有什么我可以帮忙的吗？", idleMinutes);
        }

        logger.info("Proactive message (idle {} min): {}", idleMinutes, message);
        sendProactiveMessage(message);
    }

    /**
     * 触发情绪主动消息
     */
    private void triggerMoodProactiveMessage(String mood) {
        lastProactiveTime = Instant.now();

        String message = switch (mood) {
            case "焦虑" -> "注意到主人好像有点焦虑，需要我帮忙分析一下问题吗？";
            case "疲惫" -> "主人看起来有点疲惫，要不要休息一下？";
            case "开心" -> "主人心情不错呀！有什么好事想分享吗？";
            case "烦躁" -> "主人心情不太好，需要我安静待着或者帮忙处理些事情吗？";
            case "低落" -> "主人看起来有点低落，有什么事我可以帮忙的吗？";
            default -> "主人，当前状态还好吗？需要我做什么吗？";
        };

        logger.info("Proactive message (mood={}): {}", mood, message);
        sendProactiveMessage(message);
    }

    /**
     * 发送主动消息
     */
    private void sendProactiveMessage(String message) {
        try {
            // 使用固定 session 进行主动对话
            String proactiveSessionId = "proactive-" + Instant.now().toEpochMilli();
            ConversationService.ConversationResponse response =
                conversationService.chat(message, proactiveSessionId);

            if (response.success()) {
                logger.info("Proactive message sent successfully");
            } else {
                logger.warn("Failed to send proactive message: {}", response.response());
            }
        } catch (Exception e) {
            logger.error("Error sending proactive message: {}", e.getMessage());
        }
    }

    /**
     * 主动提醒（供外部调用）
     */
    public void triggerReminder(String reminder) {
        if (!shouldProactivelyContact()) {
            logger.debug("Skipping reminder due to cooldown: {}", reminder);
            return;
        }

        lastProactiveTime = Instant.now();
        String message = "提醒：" + reminder;
        logger.info("Proactive reminder: {}", reminder);
        sendProactiveMessage(message);
    }

    /**
     * 主动通知（供外部调用）
     */
    public void triggerNotification(String notification) {
        if (!shouldProactivelyContact()) {
            logger.debug("Skipping notification due to cooldown: {}", notification);
            return;
        }

        lastProactiveTime = Instant.now();
        String message = "通知：" + notification;
        logger.info("Proactive notification: {}", notification);
        sendProactiveMessage(message);
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        scheduler.shutdown();
        logger.info("ProactiveService shutdown complete");
    }
}
