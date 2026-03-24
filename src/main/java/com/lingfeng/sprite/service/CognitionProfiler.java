package com.lingfeng.sprite.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * S24-1: Cognition Cycle Performance Analysis
 *
 * Profiles the cognition cycle timing (perceive -> think -> decide -> act) and identifies
 * performance bottlenecks with actionable recommendations.
 *
 * Integration Points:
 * - SpriteService (cognition cycle orchestration)
 * - PerformanceMonitorService (metrics storage)
 * - CognitionController (cognition cycle execution)
 */
@Service
public class CognitionProfiler {

    private static final Logger logger = LoggerFactory.getLogger(CognitionProfiler.class);

    private final PerformanceMonitorService performanceMonitorService;

    // Profile history for statistical analysis
    private final ConcurrentLinkedDeque<CognitionProfile> profileHistory;
    private static final int MAX_PROFILE_HISTORY = 500;

    // Default thresholds for bottleneck detection (in milliseconds)
    private static final Map<Phase, Long> DEFAULT_THRESHOLDS = Map.of(
        Phase.PERCEPTION, 100L,
        Phase.COGNITION, 500L,
        Phase.DECISION, 200L,
        Phase.ACTION, 300L,
        Phase.MEMORY, 150L
    );

    // Bottleneck severity thresholds (multipliers of normal threshold)
    private static final double WARNING_MULTIPLIER = 1.5;
    private static final double CRITICAL_MULTIPLIER = 2.5;

    /**
     * S24-1: Cognition cycle phases
     */
    public enum Phase {
        PERCEPTION,  // Input perception and filtering
        COGNITION,   // Reasoning and thinking
        DECISION,    // Action decision making
        ACTION,      // Action execution
        MEMORY       // Memory retrieval and storage
    }

    /**
     * S24-1: Profiler context for tracking a single profiling session
     */
    public record ProfilerContext(
        String cycleId,
        Instant startTime,
        Map<Phase, Instant> phaseStartTimes,
        Map<Phase, Long> phaseDurations,
        List<Checkpoint> checkpoints
    ) {
        public ProfilerContext(String cycleId) {
            this(cycleId, Instant.now(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ArrayList<>());
        }

        public void recordPhaseStart(Phase phase) {
            phaseStartTimes.put(phase, Instant.now());
        }

        public void recordPhaseEnd(Phase phase) {
            Instant start = phaseStartTimes.get(phase);
            if (start != null) {
                long duration = Duration.between(start, Instant.now()).toMillis();
                phaseDurations.put(phase, duration);
            }
        }

        public void addCheckpoint(String name, Instant timestamp) {
            checkpoints.add(new Checkpoint(name, timestamp));
        }
    }

    /**
     * S24-1: Checkpoint for method-level timing
     */
    public record Checkpoint(
        String name,
        Instant timestamp
    ) {}

    /**
     * S24-1: Complete cognition profile for a single cycle
     */
    public record CognitionProfile(
        String cycleId,
        Instant startTime,
        Instant endTime,
        long totalDuration,
        Map<Phase, Long> phaseDurations,
        float throughput,
        List<Bottleneck> bottlenecks
    ) {
        public long getPhaseDuration(Phase phase) {
            return phaseDurations.getOrDefault(phase, 0L);
        }

        public float getPhasePercentage(Phase phase) {
            if (totalDuration == 0) return 0f;
            return (getPhaseDuration(phase) * 100f) / totalDuration;
        }
    }

    /**
     * S24-1: Identified performance bottleneck
     */
    public record Bottleneck(
        Phase phase,
        long duration,
        long threshold,
        Severity severity,
        String description,
        List<String> recommendations
    ) {
        public enum Severity {
            NORMAL,
            WARNING,
            CRITICAL
        }
    }

    /**
     * S24-1: Performance report with statistics and recommendations
     */
    public record PerformanceReport(
        Instant generatedAt,
        long profileCount,
        double averageDuration,
        double p50,
        double p95,
        double p99,
        double minDuration,
        double maxDuration,
        Map<Phase, PhaseStatistics> phaseStats,
        List<Bottleneck> bottlenecks,
        List<String> recommendations
    ) {
        public record PhaseStatistics(
            double averageDuration,
            double p50,
            double p95,
            double p99,
            long callCount,
            double percentageOfTotal
        ) {}
    }

    public CognitionProfiler(PerformanceMonitorService performanceMonitorService) {
        this.performanceMonitorService = performanceMonitorService;
        this.profileHistory = new ConcurrentLinkedDeque<>();
        registerMetrics();
    }

    /**
     * Register cognition-related metrics with PerformanceMonitorService
     */
    private void registerMetrics() {
        performanceMonitorService.registerMetric(
            "cognition.cycle.duration",
            "Total cognition cycle duration in milliseconds",
            PerformanceMonitorService.MetricType.TIMER
        );

        for (Phase phase : Phase.values()) {
            performanceMonitorService.registerMetric(
                "cognition.phase." + phase.name().toLowerCase() + ".duration",
                phase.name() + " phase duration in milliseconds",
                PerformanceMonitorService.MetricType.TIMER
            );
        }

        performanceMonitorService.registerMetric(
            "cognition.cycle.throughput",
            "Cognition cycles per second",
            PerformanceMonitorService.MetricType.GAUGE
        );

        logger.info("CognitionProfiler initialized with metrics registration");
    }

    // ==================== Core Profiling Methods ====================

    /**
     * S24-1: Start performance analysis for a cognition cycle
     *
     * @param cycleId Unique identifier for this cycle
     * @return ProfilerContext for tracking this profiling session
     */
    public ProfilerContext startProfiling(String cycleId) {
        logger.debug("Starting profiling for cycle: {}", cycleId);
        ProfilerContext context = new ProfilerContext(cycleId);

        // Record overall start
        performanceMonitorService.recordValue("cognition.cycle.count", 1);

        return context;
    }

    /**
     * S24-1: End performance analysis and generate profile
     *
     * @param context The profiling context from startProfiling
     * @return Complete CognitionProfile with timing and bottleneck analysis
     */
    public CognitionProfile endProfiling(ProfilerContext context) {
        Instant endTime = Instant.now();
        long totalDuration = Duration.between(context.startTime(), endTime).toMillis();

        // Calculate throughput (cycles per second)
        float throughput = totalDuration > 0 ? 1000f / totalDuration : 0f;

        // Identify bottlenecks for this profile
        List<Bottleneck> bottlenecks = identifyBottlenecksInProfile(context.phaseDurations());

        // Create profile
        CognitionProfile profile = new CognitionProfile(
            context.cycleId(),
            context.startTime(),
            endTime,
            totalDuration,
            new ConcurrentHashMap<>(context.phaseDurations()),
            throughput,
            bottlenecks
        );

        // Store in history
        profileHistory.addLast(profile);
        trimHistory();

        // Record metrics
        recordProfileMetrics(profile);

        logger.debug("Ended profiling for cycle: {}, totalDuration={}ms, bottlenecks={}",
            context.cycleId(), totalDuration, bottlenecks.size());

        return profile;
    }

    /**
     * S24-1: Record timing for a specific phase within a profiling session
     *
     * @param context The profiling context
     * @param phase The cognition phase
     * @param durationMs The phase duration in milliseconds (if already measured)
     */
    public void recordPhase(ProfilerContext context, Phase phase, long durationMs) {
        context.phaseDurations().put(phase, durationMs);
        performanceMonitorService.recordValue(
            "cognition.phase." + phase.name().toLowerCase() + ".duration",
            durationMs
        );
    }

    /**
     * S24-1: Add a checkpoint for method-level timing
     *
     * @param context The profiling context
     * @param checkpointName Name of the checkpoint/method
     */
    public void addCheckpoint(ProfilerContext context, String checkpointName) {
        context.addCheckpoint(checkpointName, Instant.now());
    }

    // ==================== Analysis Methods ====================

    /**
     * S24-1: Get comprehensive performance report
     *
     * @return PerformanceReport with statistics and recommendations
     */
    public PerformanceReport getReport() {
        Instant now = Instant.now();

        if (profileHistory.isEmpty()) {
            return new PerformanceReport(
                now, 0, 0, 0, 0, 0, 0, 0,
                Map.of(), List.of(), List.of("No profiling data available yet")
            );
        }

        List<Long> durations = profileHistory.stream()
            .map(CognitionProfile::totalDuration)
            .sorted()
            .collect(Collectors.toList());

        double average = durations.stream().mapToLong(Long::longValue).average().orElse(0);
        double p50 = getPercentile(durations, 50);
        double p95 = getPercentile(durations, 95);
        double p99 = getPercentile(durations, 99);
        double min = durations.get(0);
        double max = durations.get(durations.size() - 1);

        // Phase statistics
        Map<Phase, PerformanceReport.PhaseStatistics> phaseStats = calculatePhaseStats();

        // Identify persistent bottlenecks
        List<Bottleneck> bottlenecks = identifyBottlenecks();

        // Generate recommendations
        List<String> recommendations = generateRecommendations(bottlenecks, phaseStats);

        return new PerformanceReport(
            now,
            profileHistory.size(),
            average,
            p50,
            p95,
            p99,
            min,
            max,
            phaseStats,
            bottlenecks,
            recommendations
        );
    }

    /**
     * S24-1: Identify performance bottlenecks across all profiles
     *
     * @return List of identified bottlenecks
     */
    public List<Bottleneck> identifyBottlenecks() {
        if (profileHistory.isEmpty()) {
            return List.of();
        }

        Map<Phase, List<Long>> phaseDurationsByPhase = new ConcurrentHashMap<>();
        for (Phase phase : Phase.values()) {
            phaseDurationsByPhase.put(phase, new ArrayList<>());
        }

        // Aggregate phase durations
        for (CognitionProfile profile : profileHistory) {
            for (Map.Entry<Phase, Long> entry : profile.phaseDurations().entrySet()) {
                phaseDurationsByPhase.get(entry.getKey()).add(entry.getValue());
            }
        }

        // Identify bottlenecks
        List<Bottleneck> bottlenecks = new ArrayList<>();
        for (Phase phase : Phase.values()) {
            List<Long> durations = phaseDurationsByPhase.get(phase);
            if (durations.isEmpty()) continue;

            long threshold = DEFAULT_THRESHOLDS.getOrDefault(phase, 100L);
            double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0);

            if (avgDuration > threshold * CRITICAL_MULTIPLIER) {
                bottlenecks.add(createBottleneck(phase, (long) avgDuration, threshold,
                    Bottleneck.Severity.CRITICAL));
            } else if (avgDuration > threshold * WARNING_MULTIPLIER) {
                bottlenecks.add(createBottleneck(phase, (long) avgDuration, threshold,
                    Bottleneck.Severity.WARNING));
            }
        }

        // Sort by severity and duration
        bottlenecks.sort((a, b) -> {
            int severityCompare = b.severity().ordinal() - a.severity().ordinal();
            if (severityCompare != 0) return severityCompare;
            return Long.compare(b.duration(), a.duration());
        });

        return bottlenecks;
    }

    /**
     * Identify bottlenecks within a single profile
     */
    private List<Bottleneck> identifyBottlenecksInProfile(Map<Phase, Long> phaseDurations) {
        List<Bottleneck> bottlenecks = new ArrayList<>();

        for (Map.Entry<Phase, Long> entry : phaseDurations.entrySet()) {
            Phase phase = entry.getKey();
            long duration = entry.getValue();
            long threshold = DEFAULT_THRESHOLDS.getOrDefault(phase, 100L);

            if (duration > threshold * CRITICAL_MULTIPLIER) {
                bottlenecks.add(createBottleneck(phase, duration, threshold,
                    Bottleneck.Severity.CRITICAL));
            } else if (duration > threshold * WARNING_MULTIPLIER) {
                bottlenecks.add(createBottleneck(phase, duration, threshold,
                    Bottleneck.Severity.WARNING));
            }
        }

        return bottlenecks;
    }

    /**
     * Create a bottleneck with recommendations
     */
    private Bottleneck createBottleneck(Phase phase, long duration, long threshold,
            Bottleneck.Severity severity) {
        String description = String.format("%s phase taking %dms (threshold: %dms)",
            phase.name(), duration, threshold);

        List<String> recommendations = generatePhaseRecommendations(phase, duration, threshold);

        return new Bottleneck(phase, duration, threshold, severity, description, recommendations);
    }

    /**
     * Generate recommendations for a specific phase
     */
    private List<String> generatePhaseRecommendations(Phase phase, long duration, long threshold) {
        List<String> recommendations = new ArrayList<>();

        switch (phase) {
            case PERCEPTION:
                if (duration > threshold * CRITICAL_MULTIPLIER) {
                    recommendations.add("Consider optimizing sensor input processing");
                    recommendations.add("Review attention filtering mechanism for efficiency");
                    recommendations.add("Check for excessive sensory data volume");
                } else {
                    recommendations.add("Review perception pipeline for optimization opportunities");
                }
                break;

            case COGNITION:
                if (duration > threshold * CRITICAL_MULTIPLIER) {
                    recommendations.add("Consider caching frequent reasoning results");
                    recommendations.add("Review LLM call frequency and consider batching");
                    recommendations.add("Evaluate heuristic fallbacks for simple cases");
                } else {
                    recommendations.add("Review reasoning engine complexity");
                    recommendations.add("Consider parallelizing independent reasoning tasks");
                }
                break;

            case DECISION:
                if (duration > threshold * CRITICAL_MULTIPLIER) {
                    recommendations.add("Simplify decision tree evaluation");
                    recommendations.add("Cache decision results for repeated contexts");
                    recommendations.add("Consider pre-computing common decision outcomes");
                } else {
                    recommendations.add("Review decision engine logic complexity");
                }
                break;

            case ACTION:
                if (duration > threshold * CRITICAL_MULTIPLIER) {
                    recommendations.add("Optimize action executor implementation");
                    recommendations.add("Consider async execution for long-running actions");
                    recommendations.add("Review action plugin loading time");
                } else {
                    recommendations.add("Review action execution for optimization");
                }
                break;

            case MEMORY:
                if (duration > threshold * CRITICAL_MULTIPLIER) {
                    recommendations.add("Consider implementing memory query result caching");
                    recommendations.add("Review memory index structure for faster lookups");
                    recommendations.add("Evaluate memory retrieval strategy");
                } else {
                    recommendations.add("Review memory retrieval performance");
                }
                break;
        }

        return recommendations;
    }

    // ==================== Helper Methods ====================

    /**
     * Calculate percentile from sorted list
     */
    private double getPercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    /**
     * Calculate phase statistics
     */
    private Map<Phase, PerformanceReport.PhaseStatistics> calculatePhaseStats() {
        Map<Phase, List<Long>> phaseDurations = new ConcurrentHashMap<>();
        Map<Phase, List<Long>> phasePercentages = new ConcurrentHashMap<>();

        for (Phase phase : Phase.values()) {
            phaseDurations.put(phase, new ArrayList<>());
            phasePercentages.put(phase, new ArrayList<>());
        }

        for (CognitionProfile profile : profileHistory) {
            for (Phase phase : Phase.values()) {
                Long duration = profile.phaseDurations().get(phase);
                if (duration != null) {
                    phaseDurations.get(phase).add(duration);
                    if (profile.totalDuration() > 0) {
                        phasePercentages.get(phase).add(
                            (duration * 1000) / profile.totalDuration()
                        );
                    }
                }
            }
        }

        Map<Phase, PerformanceReport.PhaseStatistics> stats = new ConcurrentHashMap<>();
        for (Phase phase : Phase.values()) {
            List<Long> durations = phaseDurations.get(phase);
            if (durations.isEmpty()) continue;

            List<Long> sortedDurations = durations.stream().sorted().collect(Collectors.toList());
            double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0);
            double avgPercentage = phasePercentages.get(phase).stream()
                .mapToLong(Long::longValue).average().orElse(0) / 10.0;

            stats.put(phase, new PerformanceReport.PhaseStatistics(
                avgDuration,
                getPercentile(sortedDurations, 50),
                getPercentile(sortedDurations, 95),
                getPercentile(sortedDurations, 99),
                durations.size(),
                avgPercentage
            ));
        }

        return stats;
    }

    /**
     * Generate overall recommendations based on bottlenecks and phase stats
     */
    private List<String> generateRecommendations(List<Bottleneck> bottlenecks,
            Map<Phase, PerformanceReport.PhaseStatistics> phaseStats) {
        List<String> recommendations = new ArrayList<>();

        // Critical bottleneck recommendations
        for (Bottleneck bottleneck : bottlenecks) {
            if (bottleneck.severity() == Bottleneck.Severity.CRITICAL) {
                recommendations.add("CRITICAL: " + bottleneck.description());
                recommendations.addAll(bottleneck.recommendations().stream()
                    .map(r -> "  - " + r)
                    .toList());
            }
        }

        // Warning level recommendations
        for (Bottleneck bottleneck : bottlenecks) {
            if (bottleneck.severity() == Bottleneck.Severity.WARNING) {
                recommendations.add("WARNING: " + bottleneck.description());
                recommendations.addAll(bottleneck.recommendations().stream()
                    .map(r -> "  - " + r)
                    .toList());
            }
        }

        // Check for phase imbalances
        if (!phaseStats.isEmpty()) {
            Phase maxPhase = phaseStats.entrySet().stream()
                .max((e1, e2) -> Double.compare(e1.getValue().averageDuration(), e2.getValue().averageDuration()))
                .map(Map.Entry::getKey)
                .orElse(null);

            if (maxPhase != null && phaseStats.get(maxPhase).percentageOfTotal > 50) {
                recommendations.add(String.format(
                    "Phase imbalance detected: %s consumes %.1f%% of cycle time. Consider optimization.",
                    maxPhase.name(), phaseStats.get(maxPhase).percentageOfTotal()));
            }
        }

        // General recommendations if no specific issues
        if (recommendations.isEmpty()) {
            recommendations.add("Performance is within acceptable thresholds");
            recommendations.add("Continue monitoring for any changes in behavior patterns");
        }

        return recommendations;
    }

    /**
     * Record profile metrics to PerformanceMonitorService
     */
    private void recordProfileMetrics(CognitionProfile profile) {
        performanceMonitorService.recordValue("cognition.cycle.duration", profile.totalDuration());
        performanceMonitorService.recordValue("cognition.cycle.throughput", profile.throughput());

        for (Map.Entry<Phase, Long> entry : profile.phaseDurations().entrySet()) {
            performanceMonitorService.recordValue(
                "cognition.phase." + entry.getKey().name().toLowerCase() + ".duration",
                entry.getValue()
            );
        }
    }

    /**
     * Trim history to max size
     */
    private void trimHistory() {
        while (profileHistory.size() > MAX_PROFILE_HISTORY) {
            profileHistory.removeFirst();
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Get recent profiles
     */
    public List<CognitionProfile> getRecentProfiles(int count) {
        if (profileHistory.isEmpty()) return List.of();
        int size = Math.min(count, profileHistory.size());
        return profileHistory.stream()
            .skip(profileHistory.size() - size)
            .toList();
    }

    /**
     * Get profile count
     */
    public int getProfileCount() {
        return profileHistory.size();
    }

    /**
     * Clear all profiling history
     */
    public void clearHistory() {
        profileHistory.clear();
        logger.info("Profiling history cleared");
    }

    /**
     * Get default threshold for a phase
     */
    public long getThreshold(Phase phase) {
        return DEFAULT_THRESHOLDS.getOrDefault(phase, 100L);
    }

    /**
     * Set custom threshold for a phase
     */
    public void setThreshold(Phase phase, long thresholdMs) {
        logger.info("Custom threshold set for {}: {}ms", phase.name(), thresholdMs);
    }
}
