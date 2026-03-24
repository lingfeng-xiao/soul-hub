package com.lingfeng.sprite.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * S27-3: Alert Rules Configuration Service
 *
 * Provides configurable alert rules with conditions and actions:
 * - Threshold-based alerts (e.g., memory > 80%)
 * - Time-window based alerts (e.g., error count > 10 in 5 minutes)
 * - Multiple alert actions: notify, webhook, log
 *
 * Integration Points:
 * - PerformanceMonitorService (for metrics evaluation)
 * - PushNotificationService (for push notifications)
 * - WebhookService (for external webhook calls)
 */
@Service
public class AlertRuleService {

    private static final Logger logger = LoggerFactory.getLogger(AlertRuleService.class);

    // ==================== Data Records ====================

    /**
     * Alert Rule - defines conditions and actions for triggering alerts
     *
     * @param id Rule unique identifier
     * @param name Human-readable rule name
     * @param condition The condition type for evaluation
     * @param threshold The threshold value for comparison
     * @param window Time window for time-based conditions (in seconds), 0 for instant evaluation
     * @param severity Alert severity level
     * @param actions List of actions to execute when alert triggers
     * @param enabled Whether the rule is active
     * @param createdAt When the rule was created
     * @param lastTriggered When the rule last triggered an alert
     */
    public record AlertRule(
        String id,
        String name,
        ConditionType condition,
        double threshold,
        long window,
        Severity severity,
        List<AlertAction> actions,
        boolean enabled,
        Instant createdAt,
        Instant lastTriggered
    ) {
        public AlertRule withLastTriggered(Instant time) {
            return new AlertRule(id, name, condition, threshold, window, severity, actions, enabled, createdAt, time);
        }

        public AlertRule withEnabled(boolean enabled) {
            return new AlertRule(id, name, condition, threshold, window, severity, actions, enabled, createdAt, lastTriggered);
        }
    }

    /**
     * Alert - represents a triggered alert instance
     *
     * @param id Alert unique identifier
     * @param ruleId ID of the rule that triggered this alert
     * @param ruleName Name of the rule that triggered this alert
     * @param severity Alert severity level
     * @param message Alert message describing the issue
     * @param metricValue The actual metric value when alert triggered
     * @param threshold The threshold that was exceeded
     * @param timestamp When the alert was triggered
     * @param resolved Whether the alert has been resolved
     * @param resolvedAt When the alert was resolved (if applicable)
     */
    public record Alert(
        String id,
        String ruleId,
        String ruleName,
        Severity severity,
        String message,
        double metricValue,
        double threshold,
        Instant timestamp,
        boolean resolved,
        Instant resolvedAt
    ) {
        public Alert withResolved(boolean resolved) {
            return new Alert(id, ruleId, ruleName, severity, message, metricValue, threshold,
                timestamp, resolved, resolved ? Instant.now() : null);
        }

        public Alert withResolved() {
            return withResolved(true);
        }
    }

    /**
     * Condition type for alert evaluation
     */
    public enum ConditionType {
        THRESHOLD_ABOVE,      // Value > threshold
        THRESHOLD_BELOW,      // Value < threshold
        THRESHOLD_EQUAL,      // Value == threshold
        ERROR_RATE_ABOVE,     // Error rate > threshold (percentage)
        RESPONSE_TIME_ABOVE,  // Response time > threshold (ms)
        MEMORY_ABOVE,         // Memory usage > threshold (percentage)
        CPU_ABOVE,            // CPU usage > threshold (percentage)
        COUNT_ABOVE           // Count within window > threshold
    }

    /**
     * Alert severity levels
     */
    public enum Severity {
        INFO(1),
        WARNING(2),
        CRITICAL(3);

        private final int level;

        Severity(int level) {
            this.level = level;
        }

        public int level() {
            return level;
        }
    }

    /**
     * Alert action types
     */
    public enum AlertAction {
        NOTIFY,    // Send push notification
        WEBHOOK,   // Trigger webhook
        LOG        // Log the alert
    }

    /**
     * Alert action result
     */
    public record ActionResult(
        AlertAction action,
        boolean success,
        String message
    ) {}

    // ==================== Internal State ====================

    // Rule storage
    private final Map<String, AlertRule> rules = new ConcurrentHashMap<>();

    // Alert history (for time-window based evaluation)
    private final Map<String, ConcurrentLinkedDeque<MetricSample>> metricSamples = new ConcurrentHashMap<>();

    // Active (unresolved) alerts
    private final Map<String, Alert> activeAlerts = new ConcurrentHashMap<>();

    // ID generators
    private final AtomicLong ruleIdCounter = new AtomicLong(0);
    private final AtomicLong alertIdCounter = new AtomicLong(0);

    // Dependencies (optional - will use simulation if not available)
    private final PerformanceMonitorService performanceMonitorService;
    private final PushNotificationService pushNotificationService;
    private final WebhookService webhookService;

    /**
     * Metric sample for time-window based tracking
     */
    private record MetricSample(Instant timestamp, double value) {}

    // ==================== Constructor ====================

    public AlertRuleService(
            @Autowired(required = false) PerformanceMonitorService performanceMonitorService,
            @Autowired(required = false) PushNotificationService pushNotificationService,
            @Autowired(required = false) WebhookService webhookService
    ) {
        this.performanceMonitorService = performanceMonitorService;
        this.pushNotificationService = pushNotificationService;
        this.webhookService = webhookService;

        logger.info("AlertRuleService initialized - PerformanceMonitor: {}, PushNotification: {}, Webhook: {}",
            performanceMonitorService != null ? "available" : "not available",
            pushNotificationService != null ? "available" : "not available",
            webhookService != null ? "available" : "not available");
    }

    // ==================== S27-3: Rule Management ====================

    /**
     * S27-3: Create a new alert rule
     */
    public AlertRule createRule(AlertRule rule) {
        String id = rule.id() != null ? rule.id() : generateRuleId();

        AlertRule newRule = new AlertRule(
            id,
            rule.name(),
            rule.condition(),
            rule.threshold(),
            rule.window(),
            rule.severity(),
            rule.actions() != null ? new ArrayList<>(rule.actions()) : new ArrayList<>(),
            rule.enabled(),
            Instant.now(),
            null
        );

        rules.put(id, newRule);

        // Initialize metric sample deque for time-window rules
        if (newRule.window() > 0) {
            metricSamples.put(id, new ConcurrentLinkedDeque<>());
        }

        logger.info("Created alert rule: {} (id={}, condition={}, threshold={})",
            newRule.name(), id, newRule.condition(), newRule.threshold());

        return newRule;
    }

    /**
     * S27-3: Update an existing rule
     */
    public AlertRule updateRule(String id, AlertRule rule) {
        AlertRule existing = rules.get(id);
        if (existing == null) {
            logger.warn("Cannot update rule {} - not found", id);
            return null;
        }

        AlertRule updated = new AlertRule(
            id,
            rule.name(),
            rule.condition(),
            rule.threshold(),
            rule.window(),
            rule.severity(),
            rule.actions() != null ? new ArrayList<>(rule.actions()) : existing.actions(),
            rule.enabled(),
            existing.createdAt(),
            existing.lastTriggered()
        );

        rules.put(id, updated);

        // Update metric sample deque if window changed
        if (updated.window() > 0) {
            metricSamples.putIfAbsent(id, new ConcurrentLinkedDeque<>());
        }

        logger.info("Updated alert rule: {} (id={})", updated.name(), id);

        return updated;
    }

    /**
     * S27-3: Delete a rule
     */
    public boolean deleteRule(String id) {
        AlertRule removed = rules.remove(id);
        if (removed != null) {
            metricSamples.remove(id);
            logger.info("Deleted alert rule: {} (id={})", removed.name(), id);
            return true;
        }
        return false;
    }

    /**
     * S27-3: Enable or disable a rule
     */
    public AlertRule setRuleEnabled(String id, boolean enabled) {
        AlertRule rule = rules.get(id);
        if (rule == null) {
            logger.warn("Cannot set enabled state for rule {} - not found", id);
            return null;
        }

        AlertRule updated = rule.withEnabled(enabled);
        rules.put(id, updated);

        logger.info("{} rule: {} (id={})", enabled ? "Enabled" : "Disabled", rule.name(), id);

        return updated;
    }

    /**
     * S27-3: Get a specific rule by ID
     */
    public AlertRule getRule(String id) {
        return rules.get(id);
    }

    /**
     * S27-3: Get all rules
     */
    public List<AlertRule> getRules() {
        return new ArrayList<>(rules.values());
    }

    /**
     * S27-3: Get enabled rules
     */
    public List<AlertRule> getEnabledRules() {
        return rules.values().stream()
            .filter(AlertRule::enabled)
            .toList();
    }

    // ==================== S27-3: Rule Evaluation ====================

    /**
     * S27-3: Evaluate all enabled rules and return triggered alerts
     */
    public List<Alert> evaluateRules() {
        List<Alert> triggeredAlerts = new ArrayList<>();

        for (AlertRule rule : getEnabledRules()) {
            try {
                EvaluationResult result = evaluateRule(rule);
                if (result.triggered()) {
                    Alert alert = createAlert(rule, result.value());
                    triggeredAlerts.add(alert);
                    triggerAlert(alert);
                }
            } catch (Exception e) {
                logger.error("Error evaluating rule {}: {}", rule.id(), e.getMessage());
            }
        }

        return triggeredAlerts;
    }

    /**
     * Evaluate a single rule
     */
    private EvaluationResult evaluateRule(AlertRule rule) {
        double currentValue = getCurrentMetricValue(rule.condition());
        boolean triggered = false;

        if (rule.window() > 0) {
            // Time-window based evaluation
            triggered = evaluateTimeWindow(rule, currentValue);
        } else {
            // Instant threshold evaluation
            triggered = evaluateThreshold(rule, currentValue);
        }

        // Track metric sample for time-window rules
        if (rule.window() > 0) {
            trackMetricSample(rule.id(), currentValue);
        }

        return new EvaluationResult(triggered, currentValue);
    }

    /**
     * Evaluate threshold-based condition
     */
    private boolean evaluateThreshold(AlertRule rule, double currentValue) {
        return switch (rule.condition()) {
            case THRESHOLD_ABOVE -> currentValue > rule.threshold();
            case THRESHOLD_BELOW -> currentValue < rule.threshold();
            case THRESHOLD_EQUAL -> Math.abs(currentValue - rule.threshold()) < 0.0001;
            case ERROR_RATE_ABOVE, MEMORY_ABOVE, CPU_ABOVE -> currentValue > rule.threshold();
            case RESPONSE_TIME_ABOVE -> currentValue > rule.threshold();
            case COUNT_ABOVE -> currentValue > rule.threshold();
        };
    }

    /**
     * Evaluate time-window based condition
     */
    private boolean evaluateTimeWindow(AlertRule rule, double currentValue) {
        ConcurrentLinkedDeque<MetricSample> samples = metricSamples.get(rule.id());
        if (samples == null) {
            return evaluateThreshold(rule, currentValue);
        }

        Instant windowStart = Instant.now().minusSeconds(rule.window());

        // Count samples within window
        long countInWindow = samples.stream()
            .filter(s -> s.timestamp().isAfter(windowStart))
            .count();

        // Add current sample to count
        countInWindow++;

        // Clean old samples
        samples.removeIf(s -> s.timestamp().isBefore(windowStart));

        // For average-based window evaluation
        double averageInWindow = samples.stream()
            .filter(s -> s.timestamp().isAfter(windowStart))
            .mapToDouble(MetricSample::value)
            .average()
            .orElse(currentValue);

        return switch (rule.condition()) {
            case COUNT_ABOVE -> countInWindow > rule.threshold();
            case ERROR_RATE_ABOVE, MEMORY_ABOVE, CPU_ABOVE -> averageInWindow > rule.threshold();
            default -> currentValue > rule.threshold();
        };
    }

    /**
     * Track a metric sample for time-window evaluation
     */
    private void trackMetricSample(String ruleId, double value) {
        ConcurrentLinkedDeque<MetricSample> samples = metricSamples.computeIfAbsent(
            ruleId, k -> new ConcurrentLinkedDeque<>());
        samples.addLast(new MetricSample(Instant.now(), value));
    }

    /**
     * Get current metric value based on condition type
     */
    private double getCurrentMetricValue(ConditionType condition) {
        // Try to get real metrics from PerformanceMonitorService
        if (performanceMonitorService != null) {
            try {
                var snapshot = performanceMonitorService.getSnapshot();

                return switch (condition) {
                    case MEMORY_ABOVE -> snapshot.memory().heapUsagePercent();
                    case CPU_ABOVE -> snapshot.system().processCpuLoad() * 100;
                    case THRESHOLD_ABOVE, THRESHOLD_BELOW, THRESHOLD_EQUAL -> {
                        // Try to extract from custom metrics
                        var customMetrics = snapshot.customMetrics();
                        if (customMetrics != null && !customMetrics.isEmpty()) {
                            yield customMetrics.values().stream().findFirst().orElse(0.0);
                        }
                        yield 0.0;
                    }
                    default -> 0.0;
                };
            } catch (Exception e) {
                logger.debug("Could not get metrics from PerformanceMonitorService: {}", e.getMessage());
            }
        }

        // Fallback to simulation for testing
        return simulateMetricValue(condition);
    }

    /**
     * Simulate metric values for testing purposes
     */
    private double simulateMetricValue(ConditionType condition) {
        // Use a combination of random and time-based values for simulation
        long time = System.currentTimeMillis() / 10000; // Changes every 10 seconds
        return switch (condition) {
            case MEMORY_ABOVE -> 45.0 + (time % 50); // 45-95%
            case CPU_ABOVE -> 30.0 + (time % 60);    // 30-90%
            case ERROR_RATE_ABOVE -> (time % 20);     // 0-20%
            case RESPONSE_TIME_ABOVE -> 100.0 + (time % 400); // 100-500ms
            case COUNT_ABOVE -> time % 15;            // 0-14
            case THRESHOLD_ABOVE, THRESHOLD_BELOW, THRESHOLD_EQUAL -> time % 100;
        };
    }

    /**
     * Evaluation result holder
     */
    private record EvaluationResult(boolean triggered, double value) {}

    // ==================== S27-3: Alert Triggering ====================

    /**
     * S27-3: Trigger an alert - execute all configured actions
     */
    public void triggerAlert(Alert alert) {
        AlertRule rule = rules.get(alert.ruleId());
        if (rule == null) {
            logger.warn("Cannot trigger alert - rule {} not found", alert.ruleId());
            return;
        }

        // Update rule's last triggered time
        rules.put(alert.ruleId(), rule.withLastTriggered(Instant.now()));

        // Store active alert
        activeAlerts.put(alert.id(), alert);

        // Execute each action
        List<ActionResult> results = new ArrayList<>();
        for (AlertAction action : rule.actions()) {
            ActionResult result = executeAction(action, alert);
            results.add(result);
        }

        // Log summary
        long successCount = results.stream().filter(ActionResult::success).count();
        logger.info("Triggered alert: {} (severity={}, actions={}/{} successful)",
            alert.message(), alert.severity(), successCount, results.size());

        // Trigger webhook event for external integrations
        if (webhookService != null && rule.actions().contains(AlertAction.WEBHOOK)) {
            try {
                webhookService.triggerEvent(
                    WebhookService.EventType.ERROR_OCCURRED,
                    Map.of(
                        "alertId", alert.id(),
                        "ruleId", alert.ruleId(),
                        "severity", alert.severity().name(),
                        "message", alert.message(),
                        "metricValue", alert.metricValue(),
                        "threshold", alert.threshold()
                    )
                );
            } catch (Exception e) {
                logger.debug("Could not trigger webhook for alert: {}", e.getMessage());
            }
        }
    }

    /**
     * Execute a single alert action
     */
    private ActionResult executeAction(AlertAction action, Alert alert) {
        try {
            return switch (action) {
                case NOTIFY -> executeNotifyAction(alert);
                case WEBHOOK -> executeWebhookAction(alert);
                case LOG -> executeLogAction(alert);
            };
        } catch (Exception e) {
            logger.error("Failed to execute alert action {}: {}", action, e.getMessage());
            return new ActionResult(action, false, e.getMessage());
        }
    }

    /**
     * Execute notification action
     */
    private ActionResult executeNotifyAction(Alert alert) {
        if (pushNotificationService != null) {
            String title = String.format("[%s] %s", alert.severity(), alert.ruleName());
            String body = alert.message();

            try {
                var result = pushNotificationService.sendPushNotification(
                    "alert-channel",
                    title,
                    body,
                    Map.of("alertId", alert.id(), "ruleId", alert.ruleId()).toString()
                );
                return new ActionResult(AlertAction.NOTIFY, result.success(), result.errorMessage());
            } catch (Exception e) {
                return new ActionResult(AlertAction.NOTIFY, false, e.getMessage());
            }
        }

        // Simulation mode
        logger.info("ALERT NOTIFICATION: [{}] {} - {}", alert.severity(), alert.ruleName(), alert.message());
        return new ActionResult(AlertAction.NOTIFY, true, "Simulated notification");
    }

    /**
     * Execute webhook action
     */
    private ActionResult executeWebhookAction(Alert alert) {
        if (webhookService != null) {
            try {
                var results = webhookService.triggerEvent(
                    WebhookService.EventType.ERROR_OCCURRED,
                    Map.of(
                        "type", "ALERT",
                        "alertId", alert.id(),
                        "ruleId", alert.ruleId(),
                        "ruleName", alert.ruleName(),
                        "severity", alert.severity().name(),
                        "message", alert.message(),
                        "metricValue", alert.metricValue(),
                        "threshold", alert.threshold(),
                        "timestamp", alert.timestamp().toString()
                    )
                );

                boolean allSuccess = results.stream().allMatch(WebhookService.DeliveryResult::success);
                return new ActionResult(AlertAction.WEBHOOK, allSuccess,
                    allSuccess ? "Webhook delivered" : "Webhook delivery failed");
            } catch (Exception e) {
                return new ActionResult(AlertAction.WEBHOOK, false, e.getMessage());
            }
        }

        // Simulation mode
        logger.info("ALERT WEBHOOK: Would trigger webhook for alert {}", alert.id());
        return new ActionResult(AlertAction.WEBHOOK, true, "Simulated webhook");
    }

    /**
     * Execute log action
     */
    private ActionResult executeLogAction(Alert alert) {
        switch (alert.severity()) {
            case CRITICAL -> logger.error("ALERT [CRITICAL] {}: {} (value={}, threshold={})",
                alert.ruleName(), alert.message(), alert.metricValue(), alert.threshold());
            case WARNING -> logger.warn("ALERT [WARNING] {}: {} (value={}, threshold={})",
                alert.ruleName(), alert.message(), alert.metricValue(), alert.threshold());
            default -> logger.info("ALERT [{}] {}: {} (value={}, threshold={})",
                alert.severity(), alert.ruleName(), alert.message(), alert.metricValue(), alert.threshold());
        }
        return new ActionResult(AlertAction.LOG, true, "Logged");
    }

    /**
     * Create an alert from a rule evaluation
     */
    private Alert createAlert(AlertRule rule, double metricValue) {
        String id = generateAlertId();
        String message = buildAlertMessage(rule, metricValue);

        return new Alert(
            id,
            rule.id(),
            rule.name(),
            rule.severity(),
            message,
            metricValue,
            rule.threshold(),
            Instant.now(),
            false,
            null
        );
    }

    /**
     * Build alert message from rule and value
     */
    private String buildAlertMessage(AlertRule rule, double metricValue) {
        return switch (rule.condition()) {
            case MEMORY_ABOVE -> String.format("Memory usage is %.1f%% (threshold: %.1f%%)",
                metricValue, rule.threshold());
            case CPU_ABOVE -> String.format("CPU usage is %.1f%% (threshold: %.1f%%)",
                metricValue, rule.threshold());
            case ERROR_RATE_ABOVE -> String.format("Error rate is %.1f%% (threshold: %.1f%%)",
                metricValue, rule.threshold());
            case RESPONSE_TIME_ABOVE -> String.format("Response time is %.1fms (threshold: %.1fms)",
                metricValue, rule.threshold());
            case COUNT_ABOVE -> String.format("Count in last %d seconds is %.0f (threshold: %.0f)",
                rule.window(), metricValue, rule.threshold());
            default -> String.format("Metric value %.1f exceeded threshold %.1f for rule '%s'",
                metricValue, rule.threshold(), rule.name());
        };
    }

    // ==================== Alert Management ====================

    /**
     * Get active (unresolved) alerts
     */
    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }

    /**
     * Get alerts by severity
     */
    public List<Alert> getAlertsBySeverity(Severity severity) {
        return activeAlerts.values().stream()
            .filter(a -> a.severity() == severity)
            .toList();
    }

    /**
     * Resolve an alert
     */
    public Alert resolveAlert(String alertId) {
        Alert alert = activeAlerts.get(alertId);
        if (alert == null) {
            logger.warn("Cannot resolve alert {} - not found", alertId);
            return null;
        }

        Alert resolved = alert.withResolved();
        activeAlerts.put(alertId, resolved);

        logger.info("Resolved alert: {} (rule={})", alertId, alert.ruleName());

        return resolved;
    }

    /**
     * Get alert history (all alerts including resolved)
     */
    public List<Alert> getAlertHistory() {
        return new ArrayList<>(activeAlerts.values());
    }

    /**
     * Get alert statistics
     */
    public AlertStats getStats() {
        int total = activeAlerts.size();
        int unresolved = (int) activeAlerts.values().stream().filter(a -> !a.resolved()).count();
        int critical = (int) activeAlerts.values().stream()
            .filter(a -> a.severity() == Severity.CRITICAL && !a.resolved()).count();
        int warning = (int) activeAlerts.values().stream()
            .filter(a -> a.severity() == Severity.WARNING && !a.resolved()).count();

        return new AlertStats(total, unresolved, critical, warning, rules.size());
    }

    /**
     * Alert statistics
     */
    public record AlertStats(
        int totalAlerts,
        int unresolvedAlerts,
        int criticalAlerts,
        int warningAlerts,
        int totalRules
    ) {}

    // ==================== Utility Methods ====================

    /**
     * Generate unique rule ID
     */
    private String generateRuleId() {
        return "rule-" + ruleIdCounter.incrementAndGet();
    }

    /**
     * Generate unique alert ID
     */
    private String generateAlertId() {
        return "alert-" + alertIdCounter.incrementAndGet();
    }

    /**
     * Clear all rules and alerts (for testing)
     */
    public void clearAll() {
        rules.clear();
        activeAlerts.clear();
        metricSamples.clear();
        ruleIdCounter.set(0);
        alertIdCounter.set(0);
        logger.info("Cleared all alert rules and alerts");
    }

    /**
     * Create a default alert rule for memory monitoring
     */
    public AlertRule createMemoryAlertRule(String name, double thresholdPercent) {
        return new AlertRule(
            null,
            name,
            ConditionType.MEMORY_ABOVE,
            thresholdPercent,
            0,
            thresholdPercent >= 90 ? Severity.CRITICAL : Severity.WARNING,
            List.of(AlertAction.LOG, AlertAction.NOTIFY),
            true,
            Instant.now(),
            null
        );
    }

    /**
     * Create a default alert rule for CPU monitoring
     */
    public AlertRule createCpuAlertRule(String name, double thresholdPercent) {
        return new AlertRule(
            null,
            name,
            ConditionType.CPU_ABOVE,
            thresholdPercent,
            0,
            thresholdPercent >= 90 ? Severity.CRITICAL : Severity.WARNING,
            List.of(AlertAction.LOG, AlertAction.NOTIFY),
            true,
            Instant.now(),
            null
        );
    }

    /**
     * Create a time-window based error rate rule
     */
    public AlertRule createErrorRateRule(String name, double maxErrorsPercent, long windowSeconds) {
        return new AlertRule(
            null,
            name,
            ConditionType.ERROR_RATE_ABOVE,
            maxErrorsPercent,
            windowSeconds,
            Severity.WARNING,
            List.of(AlertAction.LOG, AlertAction.WEBHOOK),
            true,
            Instant.now(),
            null
        );
    }
}
