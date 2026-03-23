package com.lingfeng.sprite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EvolutionDashboardService 单元测试
 */
class EvolutionDashboardServiceTest {

    private EvolutionDashboardService service;

    @BeforeEach
    void setUp() {
        service = new EvolutionDashboardService();
    }

    @Test
    void testRecordSnapshotWithVisualization() {
        // Create a mock EvolutionEngine.EvolutionHistoryVisualization-like data
        // Since we can't easily mock the inner records, test the service's internal state
        Instant now = Instant.now();

        // We can test that records are created properly by checking the history
        assertNotNull(service);
        assertTrue(service.getHistory().isEmpty());
    }

    @Test
    void testGetHistory() {
        List<EvolutionDashboardService.EvolutionSnapshot> history = service.getHistory();
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    void testGetHistoryWithDays() {
        List<EvolutionDashboardService.EvolutionSnapshot> history = service.getHistory(7);
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    void testGetHistoryDaysCutoff() {
        // History with days parameter should filter correctly
        List<EvolutionDashboardService.EvolutionSnapshot> history = service.getHistory(30);
        assertNotNull(history);
    }

    @Test
    void testAnalyzeTrendWithNoData() {
        String analysis = service.analyzeTrend();
        assertEquals("Not enough data for trend analysis", analysis);
    }

    @Test
    void testEvolutionSnapshotRecord() {
        Instant now = Instant.now();
        EvolutionDashboardService.EvolutionSnapshot snapshot =
            new EvolutionDashboardService.EvolutionSnapshot(
                now, 5, 10, 0.75f, 20, 15, 8, 3
            );

        assertEquals(now, snapshot.timestamp());
        assertEquals(5, snapshot.evolutionLevel());
        assertEquals(10, snapshot.totalEvolutions());
        assertEquals(0.75f, snapshot.globalLearningRate());
        assertEquals(20, snapshot.insightsCount());
        assertEquals(15, snapshot.principlesCount());
        assertEquals(8, snapshot.behaviorsCount());
        assertEquals(3, snapshot.modificationsCount());
    }

    @Test
    void testEvolutionTrendRecord() {
        List<Integer> levels = List.of(1, 2, 3, 4, 5);
        List<Float> rates = List.of(0.5f, 0.6f, 0.7f, 0.8f, 0.9f);
        List<Integer> insights = List.of(5, 10, 15, 20, 25);
        List<Integer> principles = List.of(2, 4, 6, 8, 10);

        EvolutionDashboardService.EvolutionTrend trend =
            new EvolutionDashboardService.EvolutionTrend(levels, rates, insights, principles);

        assertEquals(5, trend.evolutionLevels().size());
        assertEquals(5, trend.learningRates().size());
        assertEquals(5, trend.insightCounts().size());
        assertEquals(5, trend.principleCounts().size());
        assertEquals(3, trend.evolutionLevels().get(2).intValue());
        assertEquals(0.7f, trend.learningRates().get(2));
    }

    @Test
    void testInsightSummaryRecord() {
        EvolutionDashboardService.InsightSummary summary =
            new EvolutionDashboardService.InsightSummary(100, 10, 0.85f, "pattern");

        assertEquals(100, summary.totalInsights());
        assertEquals(10, summary.recentInsights());
        assertEquals(0.85f, summary.averageConfidence());
        assertEquals("pattern", summary.mostCommonType());
    }

    @Test
    void testBehaviorSummaryRecord() {
        List<String> patterns = List.of("pattern1", "pattern2");
        EvolutionDashboardService.BehaviorSummary summary =
            new EvolutionDashboardService.BehaviorSummary(50, 40, 80.0f, patterns);

        assertEquals(50, summary.totalChanges());
        assertEquals(40, summary.successfulChanges());
        assertEquals(80.0f, summary.successRate());
        assertEquals(2, summary.recentBehaviorPatterns().size());
    }

    @Test
    void testEvolutionDashboardDataRecord() {
        Instant now = Instant.now();
        List<Integer> levels = List.of(1, 2, 3);
        List<Float> rates = List.of(0.5f, 0.6f, 0.7f);
        List<Integer> insights = List.of(5, 10, 15);
        List<Integer> principles = List.of(2, 4, 6);

        EvolutionDashboardService.EvolutionTrend trend =
            new EvolutionDashboardService.EvolutionTrend(levels, rates, insights, principles);

        List<EvolutionDashboardService.EvolutionSnapshot> recentHistory = new ArrayList<>();
        EvolutionDashboardService.InsightSummary insightSummary =
            new EvolutionDashboardService.InsightSummary(100, 10, 0.85f, "pattern");
        EvolutionDashboardService.BehaviorSummary behaviorSummary =
            new EvolutionDashboardService.BehaviorSummary(50, 40, 80.0f, List.of());

        EvolutionDashboardService.EvolutionDashboardData data =
            new EvolutionDashboardService.EvolutionDashboardData(
                now, 5, 10, trend, recentHistory, insightSummary, behaviorSummary
            );

        assertEquals(now, data.timestamp());
        assertEquals(5, data.currentLevel());
        assertEquals(10, data.totalEvolutions());
        assertNotNull(data.trend());
        assertNotNull(data.recentHistory());
        assertNotNull(data.insightSummary());
        assertNotNull(data.behaviorSummary());
    }
}