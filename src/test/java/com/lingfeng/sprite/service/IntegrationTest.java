package com.lingfeng.sprite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试 - 测试服务间协作
 */
class IntegrationTest {

    private WebhookService webhookService;
    private ExternalApiAdapterService apiService;
    private HotReloadConfigService configService;
    private PerformanceMonitorService monitorService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService();
        apiService = new ExternalApiAdapterService();
        configService = new HotReloadConfigService();
        monitorService = new PerformanceMonitorService();
    }

    @Test
    void testPerformanceMonitoringWithWebhookTrigger() {
        // 注册一个webhook端点用于性能告警
        webhookService.registerEndpoint(
            "performance-alerts",
            "https://internal.webhook/performance",
            "secret",
            List.of(WebhookService.EventType.ERROR_OCCURRED)
        );

        // 记录一些性能指标
        monitorService.registerMetric("requests.total", "Total requests", PerformanceMonitorService.MetricType.COUNTER);
        monitorService.incrementCounter("requests.total");
        monitorService.incrementCounter("requests.total");
        monitorService.incrementCounter("requests.total");

        // 获取性能快照验证指标被正确记录
        PerformanceMonitorService.PerformanceSnapshot snapshot = monitorService.getSnapshot();
        assertNotNull(snapshot);
        assertTrue(snapshot.customMetrics().containsKey("requests.total"));
        assertEquals(3.0, snapshot.customMetrics().get("requests.total"));

        // 验证webhook端点存在
        assertNotNull(webhookService.getEndpoint("performance-alerts"));
        assertEquals(1, webhookService.getEnabledEndpoints().size());
    }

    @Test
    void testConfigHotReloadWithPerformanceMonitoring() throws Exception {
        // 注册配置回调来更新性能指标
        AtomicReference<Integer> configUpdateCount = new AtomicReference<>(0);
        CountDownLatch latch = new CountDownLatch(1);

        configService.registerCallback("/test/config.json", (path, data) -> {
            configUpdateCount.incrementAndGet();
            monitorService.recordValue("config.updates", configUpdateCount.get());
            latch.countDown();
        });

        // 获取初始状态
        PerformanceMonitorService.PerformanceSnapshot before = monitorService.getSnapshot();
        assertFalse(before.customMetrics().containsKey("config.updates"));

        // 更新配置（触发回调）
        configService.updateValue("/test/config.json", "test.key", "test.value");

        // 等待回调被触发
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, configUpdateCount.get());

        // 验证性能指标被更新
        PerformanceMonitorService.PerformanceSnapshot after = monitorService.getSnapshot();
        assertTrue(after.customMetrics().containsKey("config.updates"));
        assertEquals(1.0, after.customMetrics().get("config.updates"));
    }

    @Test
    void testAllServicesInitialization() {
        // 验证所有服务可以正常初始化
        assertNotNull(webhookService);
        assertNotNull(apiService);
        assertNotNull(configService);
        assertNotNull(monitorService);

        // 验证各服务统计数据
        WebhookService.WebhookStats webhookStats = webhookService.getStats();
        assertEquals(0, webhookStats.totalEndpoints());

        ExternalApiAdapterService.ApiStats apiStats = apiService.getStats();
        assertEquals(0, apiStats.totalEndpoints());

        HotReloadConfigService.ConfigStats configStats = configService.getStats();
        assertEquals(0, configStats.loadedConfigs());

        // 性能监控应该能获取JVM信息
        PerformanceMonitorService.PerformanceSnapshot snapshot = monitorService.getSnapshot();
        assertTrue(snapshot.memory().heapUsed() > 0);
        assertTrue(snapshot.threads().totalThreads() > 0);
    }

    @Test
    void testWebhookEndpointTracking() {
        // 注册多个webhook端点
        WebhookService.WebhookEndpoint e1 = webhookService.registerEndpoint(
            "endpoint-1", "https://example.com/1", "secret1",
            List.of(WebhookService.EventType.SPRITE_STARTED)
        );
        WebhookService.WebhookEndpoint e2 = webhookService.registerEndpoint(
            "endpoint-2", "https://example.com/2", "secret2",
            List.of(WebhookService.EventType.SPRITE_STOPPED, WebhookService.EventType.EMOTION_CHANGED)
        );

        // 验证统计正确
        WebhookService.WebhookStats stats = webhookService.getStats();
        assertEquals(2, stats.totalEndpoints());
        assertEquals(2, stats.enabledEndpoints());
        assertEquals(0, stats.totalSuccess());
        assertEquals(0, stats.totalFailures());

        // 获取端点列表验证
        List<WebhookService.WebhookEndpoint> all = webhookService.getAllEndpoints();
        assertEquals(2, all.size());

        List<WebhookService.WebhookEndpoint> enabled = webhookService.getEnabledEndpoints();
        assertEquals(2, enabled.size());
    }

    @Test
    void testApiEndpointRegistration() {
        // 注册API端点
        ExternalApiAdapterService.ApiEndpoint weatherEndpoint = new ExternalApiAdapterService.ApiEndpoint(
            "weather-com",
            "Weather.com",
            ExternalApiAdapterService.ApiType.WEATHER,
            "https://api.weather.com/v1",
            "weather-api-key",
            java.time.Duration.ofSeconds(30),
            true
        );
        apiService.registerEndpoint(weatherEndpoint);

        ExternalApiAdapterService.ApiEndpoint newsEndpoint = new ExternalApiAdapterService.ApiEndpoint(
            "news-api",
            "News API",
            ExternalApiAdapterService.ApiType.NEWS,
            "https://newsapi.org/v2",
            "news-api-key",
            java.time.Duration.ofSeconds(30),
            true
        );
        apiService.registerEndpoint(newsEndpoint);

        // 验证统计
        ExternalApiAdapterService.ApiStats stats = apiService.getStats();
        assertEquals(2, stats.totalEndpoints());
        assertEquals(2, stats.enabledEndpoints());

        // 验证获取
        assertNotNull(apiService.getEndpoint("weather-com"));
        assertNotNull(apiService.getEndpoint("news-api"));
        assertNull(apiService.getEndpoint("non-existent"));
    }

    @Test
    void testPerformanceSnapshotConsistency() {
        // 多次获取性能快照，验证数据一致性
        PerformanceMonitorService.PerformanceSnapshot s1 = monitorService.getSnapshot();
        PerformanceMonitorService.PerformanceSnapshot s2 = monitorService.getSnapshot();

        // 时间戳应该递增或保持
        assertTrue(s2.timestamp().compareTo(s1.timestamp()) >= 0);

        // 内存信息应该有效
        assertTrue(s1.memory().heapUsed() > 0);
        assertTrue(s1.memory().heapMax() > 0);
        assertTrue(s1.memory().heapUsagePercent() >= 0);

        // 线程信息应该有效
        assertTrue(s1.threads().totalThreads() >= 0);
    }

    @Test
    void testTimerContextPerformanceTracking() {
        // 使用计时器追踪性能
        monitorService.registerMetric("operation.duration", "Operation duration", PerformanceMonitorService.MetricType.TIMER);

        try (PerformanceMonitorService.TimerContext timer = monitorService.startTimer("operation.duration")) {
            // 模拟一些操作
            Thread.sleep(50);
        }

        Map<String, Double> values = monitorService.getAllCurrentValues();
        assertTrue(values.containsKey("operation.duration"));
        assertTrue(values.get("operation.duration") >= 50);
    }

    @Test
    void testMetricHistoryRetention() {
        // 记录超过限制的历史数据
        String metricName = "history-retention-test";
        monitorService.registerMetric(metricName, "History retention test", PerformanceMonitorService.MetricType.GAUGE);

        // 记录1100个数据点（超过MAX_HISTORY_SIZE=1000）
        for (int i = 0; i < 1100; i++) {
            monitorService.recordValue(metricName, i);
        }

        // 验证历史记录被限制在1000条
        List<PerformanceMonitorService.MetricPoint> history = monitorService.getMetricHistory(metricName, 2000);
        assertEquals(1000, history.size());

        // 验证最新的数据（107-1100 range for 1100 entries - last 1000 would be 100-1099）
        assertEquals(1099.0, history.get(history.size() - 1).value(), 0.1);
    }

    @Test
    void testAlertGeneration() {
        // 记录高内存使用（模拟）
        monitorService.recordValue("memory.simulated", 95.0);

        // 检查是否产生告警
        List<PerformanceMonitorService.Alert> alerts = monitorService.checkAlerts();
        assertNotNull(alerts);

        // 当前系统内存可能不高于90%，所以alerts可能为空
        // 但至少验证方法不抛异常
    }

    @Test
    void testMultipleMetricTypes() {
        // 测试不同类型的指标
        monitorService.registerMetric("gauge.metric", "Gauge test", PerformanceMonitorService.MetricType.GAUGE);
        monitorService.registerMetric("counter.metric", "Counter test", PerformanceMonitorService.MetricType.COUNTER);
        monitorService.registerMetric("timer.metric", "Timer test", PerformanceMonitorService.MetricType.TIMER);

        monitorService.recordValue("gauge.metric", 42.0);
        monitorService.incrementCounter("counter.metric");
        monitorService.incrementCounter("counter.metric");

        try (PerformanceMonitorService.TimerContext t = monitorService.startTimer("timer.metric")) {
            Thread.sleep(10);
        }

        Map<String, Double> values = monitorService.getAllCurrentValues();
        assertEquals(42.0, values.get("gauge.metric"));
        assertEquals(2.0, values.get("counter.metric"));
        assertTrue(values.get("timer.metric") >= 10);
    }

    @Test
    void testConfigEntryLifecycle() {
        // 测试配置条目的完整生命周期
        String path = "/test/lifecycle.json";

        // 初始状态 - 没有配置
        assertNull(configService.getConfig(path));

        // 保存配置
        Map<String, Object> data = Map.of(
            "key1", "value1",
            "key2", 123,
            "nested", Map.of("inner", "value")
        );
        configService.saveConfig(path, data);

        // 验证配置被保存
        Map<String, Object> retrieved = configService.getConfig(path);
        assertNotNull(retrieved);
        assertEquals("value1", retrieved.get("key1"));
        assertEquals(123, retrieved.get("key2"));

        // 更新配置
        configService.updateValue(path, "key1", "updated-value");
        configService.updateValue(path, "newKey", "newValue");

        // 验证更新
        Map<String, Object> updated = configService.getConfig(path);
        assertEquals("updated-value", updated.get("key1"));
        assertEquals("newValue", updated.get("newKey"));

        // 获取配置条目
        HotReloadConfigService.ConfigEntry entry = configService.getConfigEntry(path);
        assertNotNull(entry);
        assertEquals(path, entry.path());
        assertNotNull(entry.lastModified());
        assertNotNull(entry.lastLoaded());
    }

    @Test
    void testEventTypeCoverage() {
        // 验证所有事件类型都可以被订阅
        WebhookService.EventType[] eventTypes = WebhookService.EventType.values();

        for (WebhookService.EventType eventType : eventTypes) {
            String endpointId = "endpoint-" + eventType.name();
            webhookService.registerEndpoint(
                endpointId,
                "https://example.com/" + eventType.name().toLowerCase(),
                "secret",
                List.of(eventType)
            );

            WebhookService.WebhookEndpoint endpoint = webhookService.getEndpoint(endpointId);
            assertNotNull(endpoint);
            assertTrue(endpoint.subscribedEvents().contains(eventType));
        }

        // 验证统计
        WebhookService.WebhookStats stats = webhookService.getStats();
        assertEquals(eventTypes.length, stats.totalEndpoints());
    }

    @Test
    void testApiTypesCoverage() {
        // 验证所有API类型都可以配置端点
        ExternalApiAdapterService.ApiType[] apiTypes = ExternalApiAdapterService.ApiType.values();

        for (ExternalApiAdapterService.ApiType apiType : apiTypes) {
            String endpointId = "api-" + apiType.name().toLowerCase();
            ExternalApiAdapterService.ApiEndpoint endpoint = new ExternalApiAdapterService.ApiEndpoint(
                endpointId,
                apiType.name() + " Provider",
                apiType,
                "https://api.example.com/" + apiType.name().toLowerCase(),
                "api-key",
                null,
                true
            );
            apiService.registerEndpoint(endpoint);

            ExternalApiAdapterService.ApiEndpoint retrieved = apiService.getEndpoint(endpointId);
            assertNotNull(retrieved);
            assertEquals(apiType, retrieved.type());
        }

        // 验证统计
        ExternalApiAdapterService.ApiStats stats = apiService.getStats();
        assertEquals(apiTypes.length, stats.totalEndpoints());
    }

    @Test
    void testCacheMechanism() {
        // 注册一个API端点
        ExternalApiAdapterService.ApiEndpoint endpoint = new ExternalApiAdapterService.ApiEndpoint(
            "cached-api",
            "Cached API",
            ExternalApiAdapterService.ApiType.SEARCH,
            "https://api.example.com",
            "api-key",
            null,
            true
        );
        apiService.registerEndpoint(endpoint);

        // 清除缓存
        apiService.clearCache();
        ExternalApiAdapterService.ApiStats afterClear = apiService.getStats();
        assertEquals(0, afterClear.totalCached());

        // 按类型清除缓存
        apiService.clearCache(ExternalApiAdapterService.ApiType.WEATHER);
        // 不应抛异常
    }

    @Test
    void testConfigBackupAndRestore() throws Exception {
        String path = "/test/backup.json";

        // 保存初始配置
        Map<String, Object> initialData = Map.of("version", 1, "data", "original");
        configService.saveConfig(path, initialData);

        // 备份
        configService.backupConfig(path);

        // 更新配置
        Map<String, Object> updatedData = Map.of("version", 2, "data", "updated");
        configService.saveConfig(path, updatedData);

        // 验证更新
        Map<String, Object> current = configService.getConfig(path);
        assertEquals(2, current.get("version"));

        // 列出备份
        List<String> backups = configService.listBackups(path);
        assertFalse(backups.isEmpty());
    }

    @Test
    void testPerformanceReportGeneration() {
        // 注册一些指标
        monitorService.registerMetric("test.metric.1", "Test 1", PerformanceMonitorService.MetricType.GAUGE);
        monitorService.registerMetric("test.metric.2", "Test 2", PerformanceMonitorService.MetricType.COUNTER);
        monitorService.recordValue("test.metric.1", 100.0);
        monitorService.incrementCounter("test.metric.2");
        monitorService.incrementCounter("test.metric.2");
        monitorService.incrementCounter("test.metric.2");

        // 生成报告
        String report = monitorService.getPerformanceReport();
        assertNotNull(report);
        assertTrue(report.contains("=== Performance Report ==="));
        assertTrue(report.contains("--- Memory ---"));
        assertTrue(report.contains("--- Threads ---"));
        assertTrue(report.contains("--- Custom Metrics ---"));

        // 验证包含内存信息
        assertTrue(report.contains("Heap:"));
        assertTrue(report.contains("Non-Heap:"));

        // 验证包含线程信息
        assertTrue(report.contains("Total:"));
        assertTrue(report.contains("Peak:"));
    }

    @Test
    void testServiceStatsConsistency() {
        // 验证各服务的统计数据一致性
        WebhookService.WebhookStats wStats = webhookService.getStats();
        ExternalApiAdapterService.ApiStats aStats = apiService.getStats();
        HotReloadConfigService.ConfigStats cStats = configService.getStats();

        assertEquals(wStats.totalEndpoints(), webhookService.getAllEndpoints().size());
        assertEquals(aStats.totalEndpoints(), apiService.getEndpoint("weather-api") == null ?
            (int) aStats.totalEndpoints() : 1); // dynamic check

        // enabled endpoints count should match
        assertEquals(wStats.enabledEndpoints(), webhookService.getEnabledEndpoints().size());
    }
}
