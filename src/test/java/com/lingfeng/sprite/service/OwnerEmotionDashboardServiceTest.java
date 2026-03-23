package com.lingfeng.sprite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OwnerEmotionDashboardService 单元测试
 */
class OwnerEmotionDashboardServiceTest {

    private OwnerEmotionDashboardService service;

    @BeforeEach
    void setUp() {
        service = new OwnerEmotionDashboardService();
    }

    @Test
    void testRecordEmotion() {
        service.recordEmotion(0.5f, "happy", "good_news", "conversation");

        List<OwnerEmotionDashboardService.EmotionSnapshot> history = service.getHistory();
        assertEquals(1, history.size());
        assertEquals(0.5f, history.get(0).sentiment());
        assertEquals("happy", history.get(0).emotion());
        assertEquals("good_news", history.get(0).trigger());
        assertEquals("conversation", history.get(0).context());
    }

    @Test
    void testRecordEmotionSimplified() {
        service.recordEmotion(0.3f, "neutral");

        List<OwnerEmotionDashboardService.EmotionSnapshot> history = service.getHistory();
        assertEquals(1, history.size());
        assertEquals(0.3f, history.get(0).sentiment());
        assertEquals("neutral", history.get(0).emotion());
        assertNull(history.get(0).trigger());
        assertNull(history.get(0).context());
    }

    @Test
    void testRecordEmotionMaxHistoryLimit() {
        // Record more than MAX_HISTORY (500)
        for (int i = 0; i < 550; i++) {
            service.recordEmotion(0.1f * (i % 10), "emotion" + i);
        }

        List<OwnerEmotionDashboardService.EmotionSnapshot> history = service.getHistory();
        assertEquals(500, history.size());
    }

    @Test
    void testGetHistory() {
        service.recordEmotion(0.5f, "happy");
        service.recordEmotion(-0.3f, "sad");

        List<OwnerEmotionDashboardService.EmotionSnapshot> history = service.getHistory();
        assertEquals(2, history.size());
    }

    @Test
    void testGetHistoryEmpty() {
        List<OwnerEmotionDashboardService.EmotionSnapshot> history = service.getHistory();
        assertTrue(history.isEmpty());
    }

    @Test
    void testGetHistoryWithDays() {
        service.recordEmotion(0.5f, "happy");

        List<OwnerEmotionDashboardService.EmotionSnapshot> history = service.getHistory(7);
        assertNotNull(history);
    }

    @Test
    void testGenerateDashboardData() {
        service.recordEmotion(0.5f, "happy");
        service.recordEmotion(0.3f, "content");
        service.recordEmotion(-0.2f, "neutral");

        OwnerEmotionDashboardService.OwnerEmotionDashboardData data =
            service.generateDashboardData(new ArrayList<>(), new ArrayList<>());

        assertNotNull(data);
        assertNotNull(data.timestamp());
        assertNotNull(data.distribution());
        assertNotNull(data.recentTrend());
        assertNotNull(data.weeklyPatterns());
        assertNotNull(data.averageSentiment());
    }

    @Test
    void testGenerateDashboardDataWithEmptyHistory() {
        OwnerEmotionDashboardService.OwnerEmotionDashboardData data =
            service.generateDashboardData(new ArrayList<>(), new ArrayList<>());

        assertNotNull(data);
        assertEquals(0, data.distribution().positiveCount());
        assertEquals(0, data.distribution().neutralCount());
        assertEquals(0, data.distribution().negativeCount());
        assertEquals(0.0f, data.averageSentiment());
        assertEquals("unknown", data.currentEmotion());
    }

    @Test
    void testGenerateDashboardDataPositiveEmotions() {
        service.recordEmotion(0.5f, "happy");
        service.recordEmotion(0.6f, "excited");
        service.recordEmotion(0.4f, "content");

        OwnerEmotionDashboardService.OwnerEmotionDashboardData data =
            service.generateDashboardData(new ArrayList<>(), new ArrayList<>());

        assertEquals(3, data.distribution().positiveCount());
        assertEquals(0, data.distribution().negativeCount());
        assertEquals(100.0f, data.distribution().positivePercent());
    }

    @Test
    void testGenerateDashboardDataNegativeEmotions() {
        service.recordEmotion(-0.5f, "sad");
        service.recordEmotion(-0.6f, "angry");
        service.recordEmotion(-0.4f, "frustrated");

        OwnerEmotionDashboardService.OwnerEmotionDashboardData data =
            service.generateDashboardData(new ArrayList<>(), new ArrayList<>());

        assertEquals(0, data.distribution().positiveCount());
        assertEquals(3, data.distribution().negativeCount());
        assertEquals(100.0f, data.distribution().negativePercent());
    }

    @Test
    void testGenerateDashboardDataMixedEmotions() {
        service.recordEmotion(0.5f, "happy");
        service.recordEmotion(-0.5f, "sad");
        service.recordEmotion(0.0f, "neutral");

        OwnerEmotionDashboardService.OwnerEmotionDashboardData data =
            service.generateDashboardData(new ArrayList<>(), new ArrayList<>());

        assertEquals(1, data.distribution().positiveCount());
        assertEquals(1, data.distribution().neutralCount());
        assertEquals(1, data.distribution().negativeCount());
        assertEquals(33.33f, data.distribution().positivePercent(), 0.1f);
    }

    @Test
    void testGetEmotionSummary() {
        service.recordEmotion(0.5f, "happy");
        service.recordEmotion(0.3f, "content");
        service.recordEmotion(-0.2f, "neutral");

        String summary = service.getEmotionSummary();

        assertNotNull(summary);
        assertTrue(summary.contains("Distribution:"));
        assertTrue(summary.contains("Positive="));
        assertTrue(summary.contains("Neutral="));
        assertTrue(summary.contains("Negative="));
        assertTrue(summary.contains("Current:"));
        assertTrue(summary.contains("Average:"));
    }

    @Test
    void testGetEmotionSummaryEmpty() {
        String summary = service.getEmotionSummary();

        assertEquals("No emotion data recorded", summary);
    }

    @Test
    void testEmotionSnapshotRecord() {
        Instant now = Instant.now();
        OwnerEmotionDashboardService.EmotionSnapshot snapshot =
            new OwnerEmotionDashboardService.EmotionSnapshot(
                now, 0.75f, "happy", "good_news", "conversation"
            );

        assertEquals(now, snapshot.timestamp());
        assertEquals(0.75f, snapshot.sentiment());
        assertEquals("happy", snapshot.emotion());
        assertEquals("good_news", snapshot.trigger());
        assertEquals("conversation", snapshot.context());
    }

    @Test
    void testEmotionDistributionRecord() {
        OwnerEmotionDashboardService.EmotionDistribution dist =
            new OwnerEmotionDashboardService.EmotionDistribution(60, 30, 10, 60.0f, 30.0f, 10.0f);

        assertEquals(60, dist.positiveCount());
        assertEquals(30, dist.neutralCount());
        assertEquals(10, dist.negativeCount());
        assertEquals(60.0f, dist.positivePercent());
        assertEquals(30.0f, dist.neutralPercent());
        assertEquals(10.0f, dist.negativePercent());
    }

    @Test
    void testEmotionTrendPointRecord() {
        Instant now = Instant.now();
        OwnerEmotionDashboardService.EmotionTrendPoint point =
            new OwnerEmotionDashboardService.EmotionTrendPoint(now, 0.5f, "happy");

        assertEquals(now, point.timestamp());
        assertEquals(0.5f, point.sentiment());
        assertEquals("happy", point.emotion());
    }

    @Test
    void testWeeklyPatternRecord() {
        OwnerEmotionDashboardService.WeeklyPattern pattern =
            new OwnerEmotionDashboardService.WeeklyPattern(1, "周一", 0.5f, 10, 0.8f);

        assertEquals(1, pattern.dayOfWeek());
        assertEquals("周一", pattern.dayName());
        assertEquals(0.5f, pattern.averageSentiment());
        assertEquals(10, pattern.interactionCount());
        assertEquals(0.8f, pattern.emotionalStability());
    }

    @Test
    void testOptimalContactTimeRecord() {
        OwnerEmotionDashboardService.OptimalContactTime optimal =
            new OwnerEmotionDashboardService.OptimalContactTime(14, "14:00-15:00", 85.0f, "主人情绪较好");

        assertEquals(14, optimal.hourOfDay());
        assertEquals("14:00-15:00", optimal.timeSlot());
        assertEquals(85.0f, optimal.score());
        assertEquals("主人情绪较好", optimal.reason());
    }

    @Test
    void testWeeklyPatternsArray() {
        OwnerEmotionDashboardService.WeeklyPattern[] patterns =
            service.generateDashboardData(new ArrayList<>(), new ArrayList<>()).weeklyPatterns();

        assertNotNull(patterns);
        assertEquals(7, patterns.length);
    }

    @Test
    void testOptimalTimesArray() {
        OwnerEmotionDashboardService.OptimalContactTime[] times =
            service.generateDashboardData(new ArrayList<>(), new ArrayList<>()).optimalTimes();

        assertNotNull(times);
        // May be empty if no data
    }

    @Test
    void testSentimentVolatilityCalculation() {
        // Record emotions with varying sentiments to create volatility
        service.recordEmotion(0.8f, "very_happy");
        service.recordEmotion(-0.8f, "very_sad");
        service.recordEmotion(0.0f, "neutral");
        service.recordEmotion(0.5f, "happy");
        service.recordEmotion(-0.5f, "unhappy");

        OwnerEmotionDashboardService.OwnerEmotionDashboardData data =
            service.generateDashboardData(new ArrayList<>(), new ArrayList<>());

        assertTrue(data.sentimentVolatility() >= 0);
    }

    @Test
    void testVolatilityWithSingleEmotion() {
        service.recordEmotion(0.5f, "happy");

        OwnerEmotionDashboardService.OwnerEmotionDashboardData data =
            service.generateDashboardData(new ArrayList<>(), new ArrayList<>());

        assertEquals(0.0f, data.sentimentVolatility());
    }
}