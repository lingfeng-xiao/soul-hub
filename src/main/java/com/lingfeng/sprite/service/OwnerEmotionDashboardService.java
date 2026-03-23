package com.lingfeng.sprite.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lingfeng.sprite.OwnerModel.Mood;

/**
 * S10-4: 主人情绪历史Dashboard服务 - S12-2: 连接真实EmotionHistoryService数据
 *
 * 提供主人情绪历史的可视化数据：
 * - 情绪趋势图
 * - 情绪分布统计
 * - 周内模式分析
 * - 最优联系时间建议
 */
public class OwnerEmotionDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(OwnerEmotionDashboardService.class);

    private final List<EmotionSnapshot> emotionHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 500;

    /**
     * 情绪快照
     */
    public record EmotionSnapshot(
        Instant timestamp,
        float sentiment,
        String emotion,
        String trigger,
        String context
    ) {}

    /**
     * 情绪分布
     */
    public record EmotionDistribution(
        int positiveCount,
        int neutralCount,
        int negativeCount,
        float positivePercent,
        float neutralPercent,
        float negativePercent
    ) {}

    /**
     * 情绪趋势数据点
     */
    public record EmotionTrendPoint(
        Instant timestamp,
        float sentiment,
        String emotion
    ) {}

    /**
     * 周内模式
     */
    public record WeeklyPattern(
        int dayOfWeek,
        String dayName,
        float averageSentiment,
        int interactionCount,
        float emotionalStability
    ) {}

    /**
     * 主人情绪Dashboard数据
     */
    public record OwnerEmotionDashboardData(
        Instant timestamp,
        EmotionDistribution distribution,
        List<EmotionTrendPoint> recentTrend,
        WeeklyPattern[] weeklyPatterns,
        float averageSentiment,
        float sentimentVolatility,
        String currentEmotion,
        OptimalContactTime[] optimalTimes
    ) {}

    /**
     * 最优联系时间
     */
    public record OptimalContactTime(
        int hourOfDay,
        String timeSlot,
        float score,
        String reason
    ) {}

    public OwnerEmotionDashboardService() {}

    // ==================== 情绪记录 ====================

    /**
     * 记录情绪
     */
    public void recordEmotion(float sentiment, String emotion, String trigger, String context) {
        EmotionSnapshot snapshot = new EmotionSnapshot(
            Instant.now(),
            sentiment,
            emotion,
            trigger,
            context
        );
        emotionHistory.add(snapshot);

        while (emotionHistory.size() > MAX_HISTORY) {
            emotionHistory.remove(0);
        }
    }

    /**
     * 记录情绪（简化版）
     */
    public void recordEmotion(float sentiment, String emotion) {
        recordEmotion(sentiment, emotion, null, null);
    }

    // ==================== 数据生成 ====================

    /**
     * S12-2: 从真实EmotionHistoryService生成Dashboard数据
     * 直接连接EmotionHistoryService获取真实数据
     */
    public OwnerEmotionDashboardData generateDashboardData(EmotionHistoryService emotionService) {
        if (emotionService == null) {
            return createEmptyDashboardData();
        }

        Instant now = Instant.now();
        ZoneId timezone = ZoneId.of("Asia/Shanghai");

        // 获取当前情绪
        EmotionHistoryService.EmotionRecord currentRecord = emotionService.getCurrentEmotion();
        String currentEmotion = currentRecord != null ? getMoodName(currentRecord.mood()) : "unknown";
        float currentSentiment = currentRecord != null ? currentRecord.sentimentScore() : 0;

        // 获取最近的情绪记录（最近7天）
        LocalDate today = now.atZone(timezone).toLocalDate();
        List<EmotionHistoryService.EmotionRecord> recentRecords = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.minusDays(i);
            EmotionHistoryService.EmotionStats stats = emotionService.getStatsForDate(date);
            // Note: EmotionStats doesn't provide individual records, so we approximate from stats
        }

        // 获取周模式
        EmotionHistoryService.WeeklyPattern weeklyPattern = emotionService.getWeeklyPattern();

        // 获取联系建议
        EmotionHistoryService.WeeklyContactAdvice contactAdvice = emotionService.getWeeklyContactAdvice();

        // 计算情绪分布（基于周模式）
        EmotionDistribution distribution = calculateDistributionFromWeeklyPattern(weeklyPattern);

        // 生成趋势数据（从情绪变化记录）
        List<EmotionTrendPoint> recentTrend = generateTrendFromEmotionHistory(emotionService);

        // 生成周内模式（从WeeklyPattern）
        WeeklyPattern[] weeklyPatterns = generateWeeklyPatternsFromService(weeklyPattern);

        // 计算平均情绪
        float avgSentiment = calculateAverageSentiment(emotionService);

        // 计算情绪波动性
        float volatility = calculateVolatilityFromService(emotionService);

        // 最优联系时间（从WeeklyContactAdvice）
        OptimalContactTime[] optimalTimes = generateOptimalTimesFromAdvice(contactAdvice);

        return new OwnerEmotionDashboardData(
            now,
            distribution,
            recentTrend,
            weeklyPatterns,
            avgSentiment,
            volatility,
            currentEmotion,
            optimalTimes
        );
    }

    /**
     * S12-2: 从周模式计算情绪分布
     */
    private EmotionDistribution calculateDistributionFromWeeklyPattern(EmotionHistoryService.WeeklyPattern pattern) {
        if (pattern == null || pattern.moodDistribution() == null) {
            return new EmotionDistribution(0, 0, 0, 0, 0, 0);
        }

        int[] dist = pattern.moodDistribution();
        int positive = 0, neutral = 0, negative = 0;

        // Mood: HAPPY, EXCITED, CONFIDENT, GRATEFUL, CALM -> positive
        // Mood: NEUTRAL, TIRED -> neutral
        // Mood: SAD, ANXIOUS, FRUSTRATED, CONFUSED -> negative

        if (dist.length > 0) positive += dist[0]; // HAPPY
        if (dist.length > 4) positive += dist[4]; // CALM
        if (dist.length > 5) positive += dist[5]; // EXCITED

        if (dist.length > 10) neutral += dist[10]; // NEUTRAL
        if (dist.length > 9) neutral += dist[9]; // TIRED

        if (dist.length > 1) negative += dist[1]; // SAD
        if (dist.length > 2) negative += dist[2]; // ANXIOUS
        if (dist.length > 6) negative += dist[6]; // FRUSTRATED
        if (dist.length > 7) negative += dist[7]; // CONFUSED

        int total = positive + neutral + negative;
        if (total == 0) {
            return new EmotionDistribution(0, 0, 0, 0, 0, 0);
        }

        return new EmotionDistribution(
            positive, neutral, negative,
            positive * 100f / total,
            neutral * 100f / total,
            negative * 100f / total
        );
    }

    /**
     * S12-2: 从EmotionHistoryService生成趋势数据
     */
    private List<EmotionTrendPoint> generateTrendFromEmotionHistory(EmotionHistoryService emotionService) {
        List<EmotionTrendPoint> trend = new ArrayList<>();
        ZoneId timezone = ZoneId.of("Asia/Shanghai");

        // 获取最近24小时的情感记录
        Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        LocalDate today = Instant.now().atZone(timezone).toLocalDate();

        for (int hour = 0; hour < 24; hour++) {
            LocalDate date = today.minusDays(1).plusHours(hour);
            // 从daily stats获取该小时的平均情绪
            EmotionHistoryService.EmotionStats stats = emotionService.getStatsForDate(date);
            if (stats != null && stats.totalRecords() > 0) {
                trend.add(new EmotionTrendPoint(
                    date.atStartOfDay().toInstant(ZoneId.of("Asia/Shanghai").getRules().getOffset(date)),
                    stats.avgIntensity(),
                    stats.mostCommonMood() != null ? getMoodName(stats.mostCommonMood()) : "unknown"
                ));
            }
        }

        return trend;
    }

    /**
     * S12-2: 从WeeklyPattern生成周内模式
     */
    private WeeklyPattern[] generateWeeklyPatternsFromService(EmotionHistoryService.WeeklyPattern pattern) {
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        WeeklyPattern[] patterns = new WeeklyPattern[7];

        if (pattern == null) {
            return patterns;
        }

        Map<DayOfWeek, Mood> typicalMoods = pattern.typicalMoods();
        Map<DayOfWeek, Float> avgIntensities = pattern.avgIntensities();

        for (int i = 0; i < 7; i++) {
            DayOfWeek day = DayOfWeek.of(i + 1);
            Mood mood = typicalMoods != null ? typicalMoods.get(day) : null;
            Float intensity = avgIntensities != null ? avgIntensities.get(day) : 0f;

            patterns[i] = new WeeklyPattern(
                i + 1,
                dayNames[i],
                intensity != null ? intensity : 0f,
                0, // interaction count not available from pattern
                intensity != null ? intensity : 0f
            );
        }

        return patterns;
    }

    /**
     * S12-2: 计算平均情绪
     */
    private float calculateAverageSentiment(EmotionHistoryService emotionService) {
        ZoneId timezone = ZoneId.of("Asia/Shanghai");
        LocalDate today = Instant.now().atZone(timezone).toLocalDate();

        float totalSentiment = 0;
        int count = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate date = today.minusDays(i);
            EmotionHistoryService.EmotionStats stats = emotionService.getStatsForDate(date);
            if (stats != null && stats.totalRecords() > 0) {
                totalSentiment += stats.avgIntensity() * stats.totalRecords();
                count += stats.totalRecords();
            }
        }

        return count > 0 ? totalSentiment / count : 0.5f;
    }

    /**
     * S12-2: 从服务计算情绪波动性
     */
    private float calculateVolatilityFromService(EmotionHistoryService emotionService) {
        ZoneId timezone = ZoneId.of("Asia/Shanghai");
        LocalDate today = Instant.now().atZone(timezone).toLocalDate();

        List<Float> intensities = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.minusDays(i);
            EmotionHistoryService.EmotionStats stats = emotionService.getStatsForDate(date);
            if (stats != null && stats.totalRecords() > 0) {
                intensities.add(stats.avgIntensity());
            }
        }

        if (intensities.size() < 2) return 0f;

        double mean = intensities.stream().mapToDouble(Float::floatValue).average().orElse(0);
        double variance = intensities.stream().mapToDouble(e -> Math.pow(e - mean, 2)).average().orElse(0);

        return (float) Math.sqrt(variance);
    }

    /**
     * S12-2: 从联系建议生成最优联系时间
     */
    private OptimalContactTime[] generateOptimalTimesFromAdvice(EmotionHistoryService.WeeklyContactAdvice advice) {
        if (advice == null || advice.bestWindows() == null || advice.bestWindows().isEmpty()) {
            return new OptimalContactTime[0];
        }

        List<OptimalContactTime> times = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (EmotionHistoryService.OptimalContactWindow window : advice.bestWindows()) {
            String timeSlot = window.startTime().format(timeFormatter) + "-" +
                             window.endTime().format(timeFormatter);
            String reason = window.reason() != null ? window.reason() :
                           "情绪预期: " + getMoodName(window.expectedMood());

            times.add(new OptimalContactTime(
                window.dayOfWeek().getValue(),
                timeSlot,
                window.score(),
                reason
            ));
        }

        return times.toArray(new OptimalContactTime[0]);
    }

    /**
     * S12-2: Mood转String
     */
    private String getMoodName(Mood mood) {
        if (mood == null) return "unknown";
        return switch (mood) {
            case HAPPY -> "开心";
            case SAD -> "悲伤";
            case ANXIOUS -> "焦虑";
            case CALM -> "平静";
            case EXCITED -> "兴奋";
            case FRUSTRATED -> "沮丧";
            case GRATEFUL -> "感激";
            case CONFUSED -> "困惑";
            case CONFIDENT -> "自信";
            case TIRED -> "疲惫";
            case NEUTRAL -> "中性";
        };
    }

    /**
     * 创建空的Dashboard数据
     */
    private OwnerEmotionDashboardData createEmptyDashboardData() {
        Instant now = Instant.now();
        EmotionDistribution distribution = new EmotionDistribution(0, 0, 0, 0, 0, 0);
        WeeklyPattern[] patterns = new WeeklyPattern[7];
        return new OwnerEmotionDashboardData(
            now, distribution, List.of(), patterns, 0.5f, 0f, "unknown", new OptimalContactTime[0]
        );
    }

    /**
     * 生成Dashboard数据（示例实现 - 兼容旧接口）
     * @deprecated 使用 {@link #generateDashboardData(EmotionHistoryService)} 替代
     */
    @Deprecated
    public OwnerEmotionDashboardData generateDashboardData(
        List<?> emotionHistoryData,
        List<?> weeklyAdviceData
    ) {
        // 尝试从EmotionHistoryService获取真实数据
        EmotionHistoryService service = EmotionHistoryService.getInstance();
        if (service != null) {
            return generateDashboardData(service);
        }

        Instant now = Instant.now();

        // 计算情绪分布
        EmotionDistribution distribution = calculateDistribution();

        // 生成趋势数据（最近24小时）
        List<EmotionTrendPoint> recentTrend = generateRecentTrend();

        // 生成周内模式
        WeeklyPattern[] weeklyPatterns = generateWeeklyPatterns();

        // 计算平均情绪
        float avgSentiment = emotionHistory.isEmpty() ? 0 :
            (float) emotionHistory.stream()
                .mapToDouble(EmotionSnapshot::sentiment)
                .average().orElse(0);

        // 计算情绪波动性
        float volatility = calculateVolatility();

        // 当前情绪
        String currentEmotion = emotionHistory.isEmpty() ? "unknown" :
            emotionHistory.get(emotionHistory.size() - 1).emotion();

        // 最优联系时间
        OptimalContactTime[] optimalTimes = generateOptimalTimes();

        return new OwnerEmotionDashboardData(
            now,
            distribution,
            recentTrend,
            weeklyPatterns,
            avgSentiment,
            volatility,
            currentEmotion,
            optimalTimes
        );
    }

    /**
     * 计算情绪分布
     */
    private EmotionDistribution calculateDistribution() {
        if (emotionHistory.isEmpty()) {
            return new EmotionDistribution(0, 0, 0, 0, 0, 0);
        }

        int positive = 0, neutral = 0, negative = 0;
        for (EmotionSnapshot e : emotionHistory) {
            if (e.sentiment() > 0.2f) positive++;
            else if (e.sentiment() < -0.2f) negative++;
            else neutral++;
        }

        int total = emotionHistory.size();
        return new EmotionDistribution(
            positive, neutral, negative,
            positive * 100f / total,
            neutral * 100f / total,
            negative * 100f / total
        );
    }

    /**
     * 生成最近趋势
     */
    private List<EmotionTrendPoint> generateRecentTrend() {
        Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        return emotionHistory.stream()
            .filter(e -> e.timestamp().isAfter(oneDayAgo))
            .map(e -> new EmotionTrendPoint(e.timestamp(), e.sentiment(), e.emotion()))
            .toList();
    }

    /**
     * 生成周内模式
     */
    private WeeklyPattern[] generateWeeklyPatterns() {
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        WeeklyPattern[] patterns = new WeeklyPattern[7];

        ZoneId timezone = ZoneId.of("Asia/Shanghai");

        for (int i = 0; i < 7; i++) {
            int dayOfWeek = i + 1;
            final int targetDay = dayOfWeek;

            List<EmotionSnapshot> dayEmotions = emotionHistory.stream()
                .filter(e -> e.timestamp().atZone(timezone).getDayOfWeek().getValue() == targetDay)
                .toList();

            float avgSentiment = dayEmotions.isEmpty() ? 0 :
                (float) dayEmotions.stream().mapToDouble(EmotionSnapshot::sentiment).average().orElse(0);

            float stability = calculateStability(dayEmotions);

            patterns[i] = new WeeklyPattern(
                dayOfWeek,
                dayNames[i],
                avgSentiment,
                dayEmotions.size(),
                stability
            );
        }

        return patterns;
    }

    /**
     * 计算情绪波动性
     */
    private float calculateVolatility() {
        if (emotionHistory.size() < 2) return 0f;

        double mean = emotionHistory.stream()
            .mapToDouble(EmotionSnapshot::sentiment)
            .average().orElse(0);

        double variance = emotionHistory.stream()
            .mapToDouble(e -> Math.pow(e.sentiment() - mean, 2))
            .average().orElse(0);

        return (float) Math.sqrt(variance);
    }

    /**
     * 计算稳定性
     */
    private float calculateStability(List<EmotionSnapshot> emotions) {
        if (emotions.size() < 2) return 1f;

        double mean = emotions.stream()
            .mapToDouble(EmotionSnapshot::sentiment)
            .average().orElse(0);

        double variance = emotions.stream()
            .mapToDouble(e -> Math.pow(e.sentiment() - mean, 2))
            .average().orElse(0);

        // 转换为稳定性分数（低方差 = 高稳定性）
        return (float) Math.max(0, 1 - Math.sqrt(variance));
    }

    /**
     * 生成最优联系时间
     */
    private OptimalContactTime[] generateOptimalTimes() {
        ZoneId timezone = ZoneId.of("Asia/Shanghai");

        // 按小时统计
        double[] hourSentiments = new double[24];
        int[] hourCounts = new int[24];

        for (EmotionSnapshot e : emotionHistory) {
            int hour = e.timestamp().atZone(timezone).getHour();
            hourSentiments[hour] += e.sentiment();
            hourCounts[hour]++;
        }

        // 计算平均情绪并排序
        List<OptimalContactTime> times = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            if (hourCounts[hour] > 0) {
                float avgSentiment = (float) (hourSentiments[hour] / hourCounts[hour]);
                // 分数 = (情绪值 + 1) / 2 * 100，限制在0-100
                float score = Math.max(0, Math.min(100, (avgSentiment + 1) * 50));

                String timeSlot = String.format("%02d:00-%02d:00", hour, (hour + 1) % 24);
                String reason = avgSentiment > 0.2 ? "主人情绪较好" :
                               avgSentiment < -0.2 ? "主人情绪较低" : "主人情绪平稳";

                times.add(new OptimalContactTime(hour, timeSlot, score, reason));
            }
        }

        // 按分数排序，取前5
        return times.stream()
            .sorted((a, b) -> Float.compare(b.score(), a.score()))
            .limit(5)
            .toArray(OptimalContactTime[]::new);
    }

    // ==================== 工具方法 ====================

    /**
     * 获取情绪历史
     */
    public List<EmotionSnapshot> getHistory() {
        return new ArrayList<>(emotionHistory);
    }

    /**
     * 获取指定天数的历史
     */
    public List<EmotionSnapshot> getHistory(int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return emotionHistory.stream()
            .filter(e -> e.timestamp().isAfter(cutoff))
            .toList();
    }

    /**
     * 获取情绪摘要
     */
    public String getEmotionSummary() {
        if (emotionHistory.isEmpty()) {
            return "No emotion data recorded";
        }

        EmotionDistribution dist = calculateDistribution();

        StringBuilder sb = new StringBuilder();
        sb.append("Owner Emotion Summary (Last 7 days):\n");
        sb.append(String.format("  Distribution: Positive=%.1f%%, Neutral=%.1f%%, Negative=%.1f%%\n",
            dist.positivePercent(), dist.neutralPercent(), dist.negativePercent()));

        EmotionSnapshot last = emotionHistory.get(emotionHistory.size() - 1);
        sb.append(String.format("  Current: %s (%.2f)\n", last.emotion(), last.sentiment()));

        float avg = (float) emotionHistory.stream()
            .mapToDouble(EmotionSnapshot::sentiment)
            .average().orElse(0);
        sb.append(String.format("  Average: %.2f\n", avg));

        return sb.toString();
    }
}
