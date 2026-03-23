package com.lingfeng.sprite.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.PerceptionSystem.MemoryStatus;
import com.lingfeng.sprite.PerceptionSystem.PlatformPerception;
import com.lingfeng.sprite.llm.MinMaxLlmReasoner;

/**
 * 健康监控服务 - S1-1: 服务器内存告警监控
 *
 * 功能：
 * 1. 定期检查系统资源使用情况
 * 2. 当内存使用率超过80%时触发告警
 * 3. 通过ProactiveService发送通知
 * 4. 维护告警冷却时间，避免重复告警
 */
@Service
public class HealthMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(HealthMonitorService.class);

    // 内存告警阈值 (80%)
    private static final float MEMORY_ALERT_THRESHOLD = 80f;

    // 告警冷却时间 (30分钟)
    private static final Duration ALERT_COOLDOWN = Duration.ofMinutes(30);

    // 健康检查间隔 (1分钟)
    private static final long HEALTH_CHECK_INTERVAL_SECONDS = 60;

    private final UnifiedContextService unifiedContextService;
    private final ProactiveService proactiveService;
    private final MinMaxLlmReasoner llmReasoner;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // 状态跟踪
    private Instant lastMemoryAlertTime = Instant.now().minus(ALERT_COOLDOWN);
    private final AtomicBoolean memoryAlertSent = new AtomicBoolean(false);
    private final AtomicReference<Float> lastMemoryUsage = new AtomicReference<>(0f);
    private final AtomicReference<HealthStatus> currentStatus = new AtomicReference<>(HealthStatus.UNKNOWN);
    private final AtomicReference<Boolean> llmDegraded = new AtomicReference<>(false);

    public HealthMonitorService(
            @Autowired UnifiedContextService unifiedContextService,
            @Autowired ProactiveService proactiveService,
            @Autowired(required = false) MinMaxLlmReasoner llmReasoner
    ) {
        this.unifiedContextService = unifiedContextService;
        this.proactiveService = proactiveService;
        this.llmReasoner = llmReasoner;

        // 启动健康检查
        startHealthMonitoring();

        logger.info("HealthMonitorService started - memory alert threshold: {}%, cooldown: {} min",
                MEMORY_ALERT_THRESHOLD, ALERT_COOLDOWN.toMinutes());
    }

    private void startHealthMonitoring() {
        scheduler.scheduleAtFixedRate(
                this::checkMemoryHealth,
                HEALTH_CHECK_INTERVAL_SECONDS,
                HEALTH_CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * 检查内存健康状态
     */
    private void checkMemoryHealth() {
        try {
            PerceptionSystem.Perception perception = unifiedContextService.getPerception();
            if (perception == null || perception.platform() == null) {
                currentStatus.set(HealthStatus.UNKNOWN);
                return;
            }

            PlatformPerception platform = perception.platform();
            MemoryStatus memory = platform.memory();

            if (memory == null) {
                currentStatus.set(HealthStatus.UNKNOWN);
                return;
            }

            float memoryUsagePercent = memory.usedPercent();
            lastMemoryUsage.set(memoryUsagePercent);

            // 检查是否超过阈值
            if (memoryUsagePercent > MEMORY_ALERT_THRESHOLD) {
                // 检查冷却时间
                Duration sinceLastAlert = Duration.between(lastMemoryAlertTime, Instant.now());
                if (sinceLastAlert.compareTo(ALERT_COOLDOWN) >= 0) {
                    // 触发告警
                    triggerMemoryAlert(memory);
                    lastMemoryAlertTime = Instant.now();
                    currentStatus.set(HealthStatus.ALERT);
                } else {
                    currentStatus.set(HealthStatus.WARNING);
                }
            } else {
                // 内存正常
                if (memoryUsagePercent > MEMORY_ALERT_THRESHOLD * 0.8) {
                    currentStatus.set(HealthStatus.WARNING);
                } else {
                    currentStatus.set(HealthStatus.HEALTHY);
                }
                memoryAlertSent.set(false);
            }

        } catch (Exception e) {
            logger.debug("Error checking memory health: {}", e.getMessage());
            currentStatus.set(HealthStatus.UNKNOWN);
        }
    }

    /**
     * 触发内存告警
     */
    private void triggerMemoryAlert(MemoryStatus memory) {
        try {
            String alertMessage = String.format(
                "⚠️ 服务器内存告警：当前内存使用率 %.1f%%（已使用 %.1f GB / 总计 %.1f GB）",
                memory.usedPercent(),
                memory.usedMb() / 1024.0,
                memory.totalMb() / 1024.0
            );

            logger.warn("Memory alert triggered: {}%", memory.usedPercent());

            // 通过ProactiveService发送通知
            proactiveService.triggerNotification(alertMessage);

            memoryAlertSent.set(true);

        } catch (Exception e) {
            logger.error("Failed to send memory alert: {}", e.getMessage());
        }
    }

    /**
     * 获取当前健康状态
     */
    public HealthStatus getHealthStatus() {
        return currentStatus.get();
    }

    /**
     * 获取最后内存使用率
     */
    public float getLastMemoryUsage() {
        return lastMemoryUsage.get();
    }

    /**
     * 获取健康详情
     */
    public HealthDetails getHealthDetails() {
        HealthStatus status = currentStatus.get();
        float memoryUsage = lastMemoryUsage.get();
        Duration sinceLastAlert = Duration.between(lastMemoryAlertTime, Instant.now());

        // 获取LLM状态
        boolean llmAvailable = llmReasoner != null && !llmReasoner.isDegraded();
        boolean llmDegradedFlag = llmReasoner != null && llmReasoner.isDegraded();
        int llmFailures = llmReasoner != null ? llmReasoner.getConsecutiveFailures() : 0;

        // 更新LLM降级状态
        llmDegraded.set(llmDegradedFlag);

        // 如果LLM降级且内存也告警，整体状态为ALERT
        if (llmDegradedFlag && status == HealthStatus.ALERT) {
            status = HealthStatus.ALERT;
        } else if (llmDegradedFlag && status != HealthStatus.ALERT) {
            status = HealthStatus.WARNING;
        }

        return new HealthDetails(
                status,
                memoryUsage,
                MEMORY_ALERT_THRESHOLD,
                sinceLastAlert.toMinutes() < ALERT_COOLDOWN.toMinutes()
                        ? ALERT_COOLDOWN.minus(sinceLastAlert).toMinutes()
                        : 0,
                llmAvailable,
                llmDegradedFlag,
                llmFailures,
                Instant.now()
        );
    }

    /**
     * 手动触发一次健康检查（用于测试）
     */
    public void triggerHealthCheck() {
        checkMemoryHealth();
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        scheduler.shutdown();
        logger.info("HealthMonitorService shutdown complete");
    }

    /**
     * 健康状态枚举
     */
    public enum HealthStatus {
        HEALTHY,    // 健康
        WARNING,    // 警告（接近阈值）
        ALERT,      // 告警（超过阈值）
        UNKNOWN     // 未知
    }

    /**
     * 健康详情
     */
    public record HealthDetails(
            HealthStatus status,
            float memoryUsagePercent,
            float memoryAlertThreshold,
            long cooldownMinutesRemaining,
            boolean llmAvailable,
            boolean llmDegraded,
            int llmConsecutiveFailures,
            Instant checkedAt
    ) {}
}
