package com.lingfeng.sprite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PerformanceMonitorService 单元测试
 */
class PerformanceMonitorServiceTest {

    private PerformanceMonitorService monitorService;

    @BeforeEach
    void setUp() {
        monitorService = new PerformanceMonitorService();
    }

    @Test
    void testGetSnapshot() {
        PerformanceMonitorService.PerformanceSnapshot snapshot = monitorService.getSnapshot();

        assertNotNull(snapshot);
        assertNotNull(snapshot.timestamp());
        assertNotNull(snapshot.memory());
        assertNotNull(snapshot.threads());
        assertNotNull(snapshot.system());
        assertNotNull(snapshot.customMetrics());
    }

    @Test
    void testGetPerformanceStatus() {
        String status = monitorService.getPerformanceStatus();

        assertNotNull(status);
        assertTrue(status.contains("Memory:"));
        assertTrue(status.contains("Threads:"));
        assertTrue(status.contains("CPU:"));
        assertTrue(status.contains("Uptime:"));
    }

    @Test
    void testMemoryInfo() {
        PerformanceMonitorService.PerformanceSnapshot snapshot = monitorService.getSnapshot();
        PerformanceMonitorService.MemoryInfo memory = snapshot.memory();

        assertTrue(memory.heapUsed() >= 0);
        assertTrue(memory.heapMax() >= 0);
        assertTrue(memory.heapUsagePercent() >= 0);
        assertTrue(memory.nonHeapUsed() >= 0);
        assertNotNull(memory.pools());
    }

    @Test
    void testThreadInfo() {
        PerformanceMonitorService.PerformanceSnapshot snapshot = monitorService.getSnapshot();
        PerformanceMonitorService.ThreadInfo threads = snapshot.threads();

        assertTrue(threads.totalThreads() >= 0);
        assertTrue(threads.peakThreads() >= 0);
        assertTrue(threads.daemonThreads() >= 0);
        assertTrue(threads.totalStarted() >= 0);
        assertTrue(threads.peakThreads() >= threads.totalThreads());
    }

    @Test
    void testSystemInfo() {
        PerformanceMonitorService.PerformanceSnapshot snapshot = monitorService.getSnapshot();
        PerformanceMonitorService.SystemInfo system = snapshot.system();

        assertTrue(system.processCpuLoad() >= 0);
        assertTrue(system.systemCpuLoad() >= 0);
        assertTrue(system.uptime() >= 0);
    }

    @Test
    void testRegisterMetric() {
        monitorService.registerMetric("test-metric", "Test metric description", PerformanceMonitorService.MetricType.GAUGE);

        Map<String, Double> values = monitorService.getAllCurrentValues();
        assertTrue(values.containsKey("test-metric"));
    }

    @Test
    void testRecordValue() {
        monitorService.registerMetric("record-test", "Record test", PerformanceMonitorService.MetricType.GAUGE);
        monitorService.recordValue("record-test", 42.0);

        Map<String, Double> values = monitorService.getAllCurrentValues();
        assertEquals(42.0, values.get("record-test"));
    }

    @Test
    void testRecordValueAutoRegister() {
        // Recording a value for non-existent metric should auto-register it
        monitorService.recordValue("auto-registered", 100.0);

        Map<String, Double> values = monitorService.getAllCurrentValues();
        assertTrue(values.containsKey("auto-registered"));
    }

    @Test
    void testIncrementCounter() {
        monitorService.registerMetric("counter-test", "Counter test", PerformanceMonitorService.MetricType.COUNTER);
        monitorService.incrementCounter("counter-test");
        monitorService.incrementCounter("counter-test");
        monitorService.incrementCounter("counter-test");

        Map<String, Double> values = monitorService.getAllCurrentValues();
        assertEquals(3.0, values.get("counter-test"));
    }

    @Test
    void testStartTimer() {
        monitorService.registerMetric("timer-test", "Timer test", PerformanceMonitorService.MetricType.TIMER);

        PerformanceMonitorService.TimerContext timer = monitorService.startTimer("timer-test");
        assertNotNull(timer);

        // Simulate some work
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        timer.close();

        Map<String, Double> values = monitorService.getAllCurrentValues();
        assertTrue(values.containsKey("timer-test.duration"));
        assertTrue(values.get("timer-test.duration") >= 10);
    }

    @Test
    void testTimerAutoCloseable() {
        monitorService.registerMetric("auto-timer", "Auto timer", PerformanceMonitorService.MetricType.TIMER);

        // Using try-with-resources
        try (PerformanceMonitorService.TimerContext timer = monitorService.startTimer("auto-timer")) {
            // Simulate work
        }

        Map<String, Double> values = monitorService.getAllCurrentValues();
        assertTrue(values.containsKey("auto-timer.duration"));
    }

    @Test
    void testGetMetricHistory() {
        monitorService.registerMetric("history-test", "History test", PerformanceMonitorService.MetricType.GAUGE);

        for (int i = 0; i < 5; i++) {
            monitorService.recordValue("history-test", (double) i);
        }

        List<PerformanceMonitorService.MetricPoint> history = monitorService.getMetricHistory("history-test", 10);
        assertEquals(5, history.size());
    }

    @Test
    void testGetMetricHistoryLimit() {
        monitorService.registerMetric("history-limit-test", "History limit test", PerformanceMonitorService.MetricType.GAUGE);

        for (int i = 0; i < 10; i++) {
            monitorService.recordValue("history-limit-test", (double) i);
        }

        List<PerformanceMonitorService.MetricPoint> history = monitorService.getMetricHistory("history-limit-test", 3);
        assertEquals(3, history.size());
    }

    @Test
    void testGetMetricHistoryNonExistent() {
        List<PerformanceMonitorService.MetricPoint> history = monitorService.getMetricHistory("non-existent", 10);
        assertTrue(history.isEmpty());
    }

    @Test
    void testCheckAlerts() {
        List<PerformanceMonitorService.Alert> alerts = monitorService.checkAlerts();
        assertNotNull(alerts);
        // May or may not have alerts depending on system state
    }

    @Test
    void testGetPerformanceReport() {
        String report = monitorService.getPerformanceReport();

        assertNotNull(report);
        assertTrue(report.contains("=== Performance Report ==="));
        assertTrue(report.contains("--- Memory ---"));
        assertTrue(report.contains("--- Threads ---"));
        assertTrue(report.contains("--- Custom Metrics ---"));
        assertTrue(report.contains("Heap:"));
        assertTrue(report.contains("Total:"));
    }

    @Test
    void testGetAllCurrentValues() {
        monitorService.registerMetric("metric-1", "Metric 1", PerformanceMonitorService.MetricType.GAUGE);
        monitorService.registerMetric("metric-2", "Metric 2", PerformanceMonitorService.MetricType.COUNTER);
        monitorService.recordValue("metric-1", 10.0);
        monitorService.recordValue("metric-2", 5.0);

        Map<String, Double> values = monitorService.getAllCurrentValues();
        assertEquals(2, values.size());
        assertEquals(10.0, values.get("metric-1"));
        assertEquals(5.0, values.get("metric-2"));
    }

    @Test
    void testMetricTypes() {
        assertEquals(3, PerformanceMonitorService.MetricType.values().length);
        assertNotNull(PerformanceMonitorService.MetricType.valueOf("GAUGE"));
        assertNotNull(PerformanceMonitorService.MetricType.valueOf("COUNTER"));
        assertNotNull(PerformanceMonitorService.MetricType.valueOf("TIMER"));
    }

    @Test
    void testAlertLevels() {
        assertEquals(3, PerformanceMonitorService.AlertLevel.values().length);
        assertNotNull(PerformanceMonitorService.AlertLevel.valueOf("INFO"));
        assertNotNull(PerformanceMonitorService.AlertLevel.valueOf("WARNING"));
        assertNotNull(PerformanceMonitorService.AlertLevel.valueOf("CRITICAL"));
    }

    @Test
    void testAlertRecord() {
        PerformanceMonitorService.Alert alert = new PerformanceMonitorService.Alert(
            PerformanceMonitorService.AlertLevel.WARNING,
            "Memory",
            "Heap usage above 80%",
            85.5
        );

        assertEquals(PerformanceMonitorService.AlertLevel.WARNING, alert.level());
        assertEquals("Memory", alert.source());
        assertEquals("Heap usage above 80%", alert.message());
        assertEquals(85.5, alert.value());
    }

    @Test
    void testMetricPointRecord() {
        java.time.Instant now = java.time.Instant.now();
        PerformanceMonitorService.MetricPoint point = new PerformanceMonitorService.MetricPoint(
            now, 42.0, "ms"
        );

        assertEquals(now, point.timestamp());
        assertEquals(42.0, point.value());
        assertEquals("ms", point.unit());
    }

    @Test
    void testMetricGaugeRecord() {
        java.time.Instant now = java.time.Instant.now();
        PerformanceMonitorService.MetricGauge gauge = new PerformanceMonitorService.MetricGauge(
            "test-gauge",
            "Test gauge description",
            PerformanceMonitorService.MetricType.GAUGE,
            50.0,
            10.0,
            100.0,
            now
        );

        assertEquals("test-gauge", gauge.name());
        assertEquals("Test gauge description", gauge.description());
        assertEquals(PerformanceMonitorService.MetricType.GAUGE, gauge.type());
        assertEquals(50.0, gauge.currentValue());
        assertEquals(10.0, gauge.minValue());
        assertEquals(100.0, gauge.maxValue());
        assertEquals(now, gauge.lastUpdated());
    }

    @Test
    void testFormatValue() {
        // The formatValue method is private, but we can test it through the report
        monitorService.registerMetric("large-value", "Large value test", PerformanceMonitorService.MetricType.GAUGE);
        monitorService.recordValue("large-value", 1500000.0);
        monitorService.recordValue("large-value", 2500.0);
        monitorService.recordValue("large-value", 50.0);

        String report = monitorService.getPerformanceReport();
        assertTrue(report.contains("large-value"));
    }
}
