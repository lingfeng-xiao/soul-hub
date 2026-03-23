package com.lingfeng.sprite.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 * 健康监控服务 - S1-1: 服务器内存告警监控, S5-3: 传感器健康检查
 *
 * 功能：
 * 1. 定期检查系统资源使用情况
 * 2. 当内存使用率超过80%时触发告警
 * 3. 通过ProactiveService发送通知
 * 4. 维护告警冷却时间，避免重复告警
 * 5. S5-3: 检查所有传感器健康状态
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

    // S5-3: 传感器健康检查阈值
    private static final long SENSOR_TIMEOUT_MS = 60000; // 60秒无响应认为传感器异常

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

    // S5-3: 传感器健康状态跟踪
    private final AtomicReference<Map<String, SensorHealth>> sensorHealthMap = new AtomicReference<>(new java.util.HashMap<>());
    private Instant lastSensorCheckTime = Instant.now();

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
     * S5-3: 检查所有传感器健康状态
     */
    private void checkSensorHealth() {
        try {
            Instant now = Instant.now();
            PerceptionSystem.Perception perception = unifiedContextService.getPerception();

            if (perception == null) {
                return;
            }

            // 更新各个传感器的健康状态
            updateSensorHealth("user", perception.user() != null, now);
            updateSensorHealth("environment", perception.environment() != null, now);
            updateSensorHealth("platform", perception.platform() != null, now);

            lastSensorCheckTime = now;

        } catch (Exception e) {
            logger.debug("Error checking sensor health: {}", e.getMessage());
        }
    }

    /**
     * S5-3: 更新单个传感器健康状态
     */
    private void updateSensorHealth(String sensorName, boolean isResponding, Instant now) {
        Map<String, SensorHealth> current = sensorHealthMap.get();
        SensorHealth existing = current.get(sensorName);

        if (existing == null) {
            existing = new SensorHealth(sensorName, true, now, now, 0);
        }

        SensorHealth updated;
        if (isResponding) {
            updated = new SensorHealth(
                    sensorName,
                    true,
                    existing.firstFailure() != null ? existing.firstFailure() : now,
                    now,
                    0
            );
        } else {
            Instant firstFailure = existing.firstFailure() != null ? existing.firstFailure() : now;
            long failureDuration = Duration.between(firstFailure, now).toMillis();
            boolean healthy = failureDuration < SENSOR_TIMEOUT_MS;
            updated = new SensorHealth(
                    sensorName,
                    healthy,
                    firstFailure,
                    existing.lastResponse(),
                    failureDuration
            );

            // 如果传感器首次失败超过阈值，触发告警
            if (!healthy && existing.healthy()) {
                triggerSensorAlert(sensorName, failureDuration);
            }
        }

        current.put(sensorName, updated);
    }

    /**
     * S5-3: 触发传感器告警
     */
    private void triggerSensorAlert(String sensorName, long failureDurationMs) {
        try {
            String alertMessage = String.format(
                    "⚠️ 传感器告警：%s 传感器无响应（已离线 %.1f 秒）",
                    sensorName,
                    failureDurationMs / 1000.0
            );

            logger.warn("Sensor alert triggered: {}", alertMessage);
            proactiveService.triggerNotification(alertMessage);

        } catch (Exception e) {
            logger.error("Failed to send sensor alert: {}", e.getMessage());
        }
    }

    /**
     * S5-3: 获取所有传感器健康状态
     */
    public Map<String, SensorHealth> getSensorHealthStatus() {
        // 先执行一次检查
        checkSensorHealth();
        return new ConcurrentHashMap<>(sensorHealthMap.get());
    }

    /**
     * S5-3: 获取指定传感器健康状态
     */
    public SensorHealth getSensorHealth(String sensorName) {
        return sensorHealthMap.get().get(sensorName);
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

        // S5-3: 检查传感器健康
        checkSensorHealth();
        Map<String, SensorHealth> sensorHealth = new ConcurrentHashMap<>(sensorHealthMap.get());

        // 如果有任何传感器不健康，整体状态降级
        boolean anySensorUnhealthy = sensorHealth.values().stream()
                .anyMatch(s -> !s.healthy());
        if (anySensorUnhealthy && status != HealthStatus.ALERT) {
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
                Instant.now(),
                sensorHealth
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
            Instant checkedAt,
            Map<String, SensorHealth> sensorHealth
    ) {}

    /**
     * S5-3: 传感器健康状态
     */
    public record SensorHealth(
            String sensorName,
            boolean healthy,
            Instant firstFailure,
            Instant lastResponse,
            long failureDurationMs
    ) {}
}
