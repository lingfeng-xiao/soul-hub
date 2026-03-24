package com.lingfeng.sprite.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.Memory;
import com.lingfeng.sprite.MemorySystem.StimulusType;
import com.lingfeng.sprite.MemorySystem.StoreType;

/**
 * S28-3: Forgetting Curve Optimization Service
 *
 * Implements scientifically-based forgetting curves (Ebbinghaus) and
 * optimizes memory consolidation timing based on recall probability.
 * Provides spaced repetition scheduling for important memories and
 * adaptive decay rates per memory type.
 *
 * ## Ebbinghaus Forgetting Curve
 *
 * The original Ebbinghaus forgetting curve shows that memory retention
 * follows an exponential decay pattern:
 * - R = e^(-t/S) where R is retention, t is time, S is stability
 *
 * ## Spaced Repetition Algorithm
 *
 * Uses a modified SM-2 algorithm for optimal review scheduling:
 * - Each successful recall increases the interval
 * - Failed recalls reset to shorter intervals
 * - Memory importance affects decay rate
 *
 * ## Adaptive Decay Rates
 *
 * Different memory types have different baseline decay rates:
 * - EPISODIC: Fast decay (emotional events)
 * - PROCEDURAL: Slow decay (skills, habits)
 * - SEMANTIC: Medium decay (facts, concepts)
 * - PERCEPTIVE: Fastest decay (patterns, associations)
 */
@Service
public class ForgettingCurveOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(ForgettingCurveOptimizer.class);

    // Ebbinghaus forgetting curve constants
    private static final double EBBINGHAUS_DECAY_CONSTANT = 1.25;  // S parameter in R = e^(-t/S)
    private static final double MIN_RETENTION_THRESHOLD = 0.3;       // Below this, memory needs review
    private static final double OPTIMAL_RETENTION_TARGET = 0.9;      // Target retention for review

    // Spaced repetition constants (SM-2 inspired)
    private static final double DEFAULT_EASE_FACTOR = 2.5;
    private static final double MIN_EASE_FACTOR = 1.3;
    private static final double MAX_EASE_FACTOR = 3.0;
    private static final int INITIAL_INTERVAL_MINUTES = 10;
    private static final int SECOND_INTERVAL_MINUTES = 60;  // 1 hour

    // Memory type decay multipliers (higher = faster decay)
    private static final Map<StoreType, Double> DECAY_MULTIPLIERS = new ConcurrentHashMap<>();
    // Memory importance impact on decay
    private static final double IMPORTANCE_DECAY_IMPACT = 0.1;

    // Memory metadata keys
    private static final String KEY_LAST_REVIEW = "lastReview";
    private static final String KEY_REVIEW_COUNT = "reviewCount";
    private static final String KEY_EASE_FACTOR = "easeFactor";
    private static final String KEY_INTERVAL_MINUTES = "intervalMinutes";
    private static final String KEY_RETENTION_STRENGTH = "retentionStrength";
    private static final String KEY_DECAY_RATE = "decayRate";
    private static final String KEY_LAST_RECALLED = "lastRecalled";

    static {
        // EPISODIC memories decay faster due to emotional intensity
        DECAY_MULTIPLIERS.put(StoreType.EPISODIC, 1.2);
        // PROCEDURAL memories decay slowest (muscle memory)
        DECAY_MULTIPLIERS.put(StoreType.PROCEDURAL, 0.6);
        // SEMANTIC memories decay at medium rate
        DECAY_MULTIPLIERS.put(StoreType.SEMANTIC, 1.0);
        // PERCEPTIVE memories decay fastest (pattern associations)
        DECAY_MULTIPLIERS.put(StoreType.PERCEPTIVE, 1.5);
    }

    /**
     * Memory review data stored per memory item
     */
    public record MemoryReviewData(
        Instant lastReview,
        int reviewCount,
        double easeFactor,
        long intervalMinutes,
        double retentionStrength,
        double decayRate,
        Instant lastRecalled,
        boolean wasRecalled
    ) {
        public MemoryReviewData {
            if (easeFactor < MIN_EASE_FACTOR) easeFactor = MIN_EASE_FACTOR;
            if (easeFactor > MAX_EASE_FACTOR) easeFactor = MAX_EASE_FACTOR;
            if (retentionStrength < 0) retentionStrength = 0;
            if (retentionStrength > 1) retentionStrength = 1;
        }

        public static MemoryReviewData initial() {
            return new MemoryReviewData(
                Instant.now(),
                0,
                DEFAULT_EASE_FACTOR,
                INITIAL_INTERVAL_MINUTES,
                1.0,
                1.0,
                Instant.now(),
                true
            );
        }
    }

    /**
     * Review schedule for a memory
     */
    public record ReviewSchedule(
        Instant nextReviewTime,
        Duration timeUntilReview,
        double predictedRetention,
        double confidence
    ) {}

    /**
     * Statistics about the forgetting curve optimization
     */
    public record OptimizationStats(
        int totalTrackedMemories,
        int memoriesNeedingReview,
        double averageRetention,
        double averageEaseFactor,
        Instant lastOptimization
    ) {}

    // Internal storage for review data (in production, this would be persisted)
    private final Map<String, MemoryReviewData> reviewDataCache = new ConcurrentHashMap<>();
    private Instant lastOptimizationTime = Instant.now();

    public ForgettingCurveOptimizer() {
        logger.info("ForgettingCurveOptimizer initialized - Ebbinghaus decay constant: {}, threshold: {}",
            EBBINGHAUS_DECAY_CONSTANT, MIN_RETENTION_THRESHOLD);
    }

    // ==================== S28-3: Calculate Retention Strength ====================

    /**
     * S28-3: Calculate the current retention strength of a memory
     *
     * Uses the Ebbinghaus forgetting curve formula:
     * R = e^(-t/S) where:
     * - R = retention (0 to 1)
     * - t = time since last review
     * - S = stability (varies by memory type and review count)
     *
     * @param memory The memory system containing the memories
     * @return Retention strength between 0 (forgotten) and 1 (perfect)
     */
    public double calculateRetentionStrength(Memory memory) {
        if (memory == null) {
            return 0.0;
        }

        double totalRetention = 0.0;
        int memoryCount = 0;

        // Calculate retention for episodic memories
        for (var episodic : memory.getLongTerm().getAllEpisodic()) {
            String memoryId = episodic.id();
            double retention = calculateSingleMemoryRetention(
                memoryId,
                StoreType.EPISODIC,
                episodic.timestamp()
            );
            totalRetention += retention;
            memoryCount++;
        }

        // Calculate retention for semantic memories
        for (var semantic : memory.getLongTerm().getAllSemantic()) {
            String memoryId = semantic.id();
            double retention = calculateSingleMemoryRetention(
                memoryId,
                StoreType.SEMANTIC,
                semantic.createdAt()
            );
            totalRetention += retention;
            memoryCount++;
        }

        // Calculate retention for procedural memories
        for (var procedural : memory.getLongTerm().getAllProcedural()) {
            String memoryId = procedural.id();
            double retention = calculateSingleMemoryRetention(
                memoryId,
                StoreType.PROCEDURAL,
                procedural.lastPracticed() != null ? procedural.lastPracticed() : Instant.now()
            );
            totalRetention += retention;
            memoryCount++;
        }

        // Calculate retention for perceptive memories
        for (var perceptive : memory.getLongTerm().getAllPerceptive()) {
            String memoryId = perceptive.id();
            double retention = calculateSingleMemoryRetention(
                memoryId,
                StoreType.PERCEPTIVE,
                Instant.now()
            );
            totalRetention += retention;
            memoryCount++;
        }

        return memoryCount > 0 ? totalRetention / memoryCount : 1.0;
    }

    /**
     * Calculate retention for a specific memory item
     */
    private double calculateSingleMemoryRetention(String memoryId, StoreType storeType, Instant referenceTime) {
        MemoryReviewData data = reviewDataCache.get(memoryId);
        if (data == null) {
            return 1.0;  // New memory starts at full strength
        }

        Instant lastReview = data.lastReview();
        Duration timeSinceReview = Duration.between(lastReview, Instant.now());

        // Get adaptive decay rate for this memory
        double baseDecayRate = getBaseDecayRate(storeType);
        double adaptiveDecayRate = data.decayRate();
        double combinedDecayRate = baseDecayRate * adaptiveDecayRate;

        // Calculate stability (S) - increases with more reviews
        double stability = EBBINGHAUS_DECAY_CONSTANT * data.easeFactor() * Math.sqrt(data.reviewCount() + 1);

        // Ebbinghaus formula: R = e^(-t/S)
        double timeSeconds = timeSinceReview.getSeconds();
        double retention = Math.exp(-timeSeconds / stability);

        // Apply importance modifier if available
        double importanceModifier = 1.0 - (IMPORTANCE_DECAY_IMPACT * (1.0 - combinedDecayRate));
        retention *= importanceModifier;

        return Math.max(0.0, Math.min(1.0, retention));
    }

    /**
     * Calculate retention for a specific memory ID
     */
    public double calculateRetentionStrength(String memoryId, StoreType storeType, Instant lastReviewTime) {
        return calculateSingleMemoryRetention(memoryId, storeType, lastReviewTime);
    }

    // ==================== S28-3: Calculate Next Review Time ====================

    /**
     * S28-3: Calculate when a memory should be reviewed next
     *
     * Uses the optimal retention target to determine review timing.
     * When predicted retention drops to OPTIMAL_RETENTION_TARGET, review is due.
     *
     * @param memory The memory system
     * @return Instant when the next review should occur
     */
    public Instant getNextReviewTime(Memory memory) {
        if (memory == null) {
            return Instant.now().plusSeconds(INITIAL_INTERVAL_MINUTES * 60);
        }

        Instant earliestReview = Instant.now().plusSeconds(Long.MAX_VALUE);

        // Check all memory types for earliest review need
        earliestReview = checkEpisodicReviewTimes(memory, earliestReview);
        earliestReview = checkSemanticReviewTimes(memory, earliestReview);
        earliestReview = checkProceduralReviewTimes(memory, earliestReview);
        earliestReview = checkPerceptiveReviewTimes(memory, earliestReview);

        // If no specific memory needs review, schedule based on overall retention
        if (earliestReview.equals(Instant.now().plusSeconds(Long.MAX_VALUE))) {
            double currentRetention = calculateRetentionStrength(memory);
            if (currentRetention > OPTIMAL_RETENTION_TARGET) {
                // Calculate when retention will drop to target
                long secondsUntilTarget = calculateTimeToRetention(currentRetention, OPTIMAL_RETENTION_TARGET);
                earliestReview = Instant.now().plusSeconds(secondsUntilTarget);
            } else {
                // Review now
                earliestReview = Instant.now();
            }
        }

        return earliestReview;
    }

    /**
     * Get next review time for a specific memory item
     */
    public Instant getNextReviewTime(String memoryId, StoreType storeType) {
        MemoryReviewData data = reviewDataCache.get(memoryId);
        if (data == null) {
            return Instant.now().plusSeconds(INITIAL_INTERVAL_MINUTES * 60);
        }

        // Calculate when retention will drop below threshold
        double currentRetention = calculateSingleMemoryRetention(memoryId, storeType, data.lastReview());
        long secondsUntilThreshold = calculateTimeToRetention(currentRetention, MIN_RETENTION_THRESHOLD);

        return Instant.now().plusSeconds(secondsUntilThreshold);
    }

    /**
     * Get full review schedule for a memory
     */
    public ReviewSchedule getReviewSchedule(String memoryId, StoreType storeType) {
        MemoryReviewData data = reviewDataCache.get(memoryId);
        if (data == null) {
            data = MemoryReviewData.initial();
        }

        Instant nextReview = getNextReviewTime(memoryId, storeType);
        double predictedRetention = calculateSingleMemoryRetention(memoryId, storeType, data.lastReview());
        Duration timeUntilReview = Duration.between(Instant.now(), nextReview);

        // Confidence based on review count and data quality
        double confidence = Math.min(1.0, 0.5 + (data.reviewCount() * 0.05));

        return new ReviewSchedule(nextReview, timeUntilReview, predictedRetention, confidence);
    }

    private Instant checkEpisodicReviewTimes(Memory memory, Instant earliest) {
        for (var episodic : memory.getLongTerm().getAllEpisodic()) {
            Instant next = getNextReviewTime(episodic.id(), StoreType.EPISODIC);
            if (next.isBefore(earliest)) {
                earliest = next;
            }
        }
        return earliest;
    }

    private Instant checkSemanticReviewTimes(Memory memory, Instant earliest) {
        for (var semantic : memory.getLongTerm().getAllSemantic()) {
            Instant next = getNextReviewTime(semantic.id(), StoreType.SEMANTIC);
            if (next.isBefore(earliest)) {
                earliest = next;
            }
        }
        return earliest;
    }

    private Instant checkProceduralReviewTimes(Memory memory, Instant earliest) {
        for (var procedural : memory.getLongTerm().getAllProcedural()) {
            Instant next = getNextReviewTime(procedural.id(), StoreType.PROCEDURAL);
            if (next.isBefore(earliest)) {
                earliest = next;
            }
        }
        return earliest;
    }

    private Instant checkPerceptiveReviewTimes(Memory memory, Instant earliest) {
        for (var perceptive : memory.getLongTerm().getAllPerceptive()) {
            Instant next = getNextReviewTime(perceptive.id(), StoreType.PERCEPTIVE);
            if (next.isBefore(earliest)) {
                earliest = next;
            }
        }
        return earliest;
    }

    // ==================== S28-3: Calculate Optimal Interval ====================

    /**
     * S28-3: Calculate the optimal review interval for a memory
     *
     * Uses a modified SM-2 algorithm:
     * - First review: 10 minutes
     * - Second review: 1 hour
     * - Subsequent reviews: previous_interval * ease_factor
     *
     * @param memory The memory (can be null for default calculation)
     * @param reviewCount Number of successful reviews already completed
     * @return Optimal interval until next review
     */
    public Duration calculateOptimalInterval(Memory memory, int reviewCount) {
        return calculateOptimalInterval(memory, reviewCount, 1.0);
    }

    /**
     * Calculate optimal interval with importance factor
     *
     * @param memory The memory
     * @param reviewCount Number of successful reviews
     * @param importance Importance factor (0.5 to 2.0)
     * @return Optimal interval
     */
    public Duration calculateOptimalInterval(Memory memory, int reviewCount, double importance) {
        long intervalMinutes;

        if (reviewCount <= 0) {
            // First review
            intervalMinutes = INITIAL_INTERVAL_MINUTES;
        } else if (reviewCount == 1) {
            // Second review
            intervalMinutes = SECOND_INTERVAL_MINUTES;
        } else {
            // Subsequent reviews - use SM-2 formula
            intervalMinutes = calculateSM2Interval(reviewCount, DEFAULT_EASE_FACTOR);
        }

        // Apply importance modifier
        // Higher importance = longer interval (more stable memory)
        double importanceModifier = 1.0 + (importance - 1.0) * 0.5;
        intervalMinutes = (long) (intervalMinutes * importanceModifier);

        // Cap at reasonable maximum (30 days)
        long maxMinutes = 30 * 24 * 60;
        intervalMinutes = Math.min(intervalMinutes, maxMinutes);

        // Floor at minimum (1 minute)
        intervalMinutes = Math.max(intervalMinutes, 1);

        return Duration.ofMinutes(intervalMinutes);
    }

    /**
     * Calculate optimal interval for a specific memory
     */
    public Duration calculateOptimalInterval(String memoryId) {
        MemoryReviewData data = reviewDataCache.get(memoryId);
        if (data == null) {
            return Duration.ofMinutes(INITIAL_INTERVAL_MINUTES);
        }

        long intervalMinutes = calculateSM2Interval(data.reviewCount(), data.easeFactor());
        return Duration.ofMinutes(intervalMinutes);
    }

    /**
     * SM-2 interval calculation
     * I(n) = I(n-1) * EF where EF is the ease factor
     */
    private long calculateSM2Interval(int reviewCount, double easeFactor) {
        if (reviewCount <= 1) {
            return reviewCount == 0 ? INITIAL_INTERVAL_MINUTES : SECOND_INTERVAL_MINUTES;
        }

        // Exponential growth with ease factor
        double interval = INITIAL_INTERVAL_MINUTES * Math.pow(easeFactor, reviewCount - 1);

        // Apply diminishing returns for very long intervals
        if (interval > 1440) {  // > 1 day
            interval *= 1 + Math.log(interval / 1440) * 0.1;
        }

        return (long) interval;
    }

    // ==================== S28-3: Update Forgetting Curve ====================

    /**
     * S28-3: Update the forgetting curve based on recall result
     *
     * When a memory is recalled:
     * - If successful: increase ease factor, lengthen interval
     * - If failed: decrease ease factor, reset to shorter interval
     *
     * @param memory The memory system (for accessing metadata)
     * @param recalled Whether the memory was successfully recalled
     */
    public void updateForgettingCurve(Memory memory, boolean recalled) {
        if (memory == null) {
            return;
        }

        // Update all memories in the system
        updateEpisodicForgettingCurves(memory, recalled);
        updateSemanticForgettingCurves(memory, recalled);
        updateProceduralForgettingCurves(memory, recalled);
        updatePerceptiveForgettingCurves(memory, recalled);

        lastOptimizationTime = Instant.now();
    }

    /**
     * Update forgetting curve for a specific memory
     *
     * @param memoryId The memory identifier
     * @param storeType The type of memory
     * @param recalled Whether the memory was successfully recalled
     */
    public void updateForgettingCurve(String memoryId, StoreType storeType, boolean recalled) {
        MemoryReviewData current = reviewDataCache.get(memoryId);
        if (current == null) {
            current = MemoryReviewData.initial();
        }

        MemoryReviewData updated;
        if (recalled) {
            updated = processSuccessfulRecall(memoryId, storeType, current);
        } else {
            updated = processFailedRecall(memoryId, storeType, current);
        }

        reviewDataCache.put(memoryId, updated);
        logger.debug("Updated forgetting curve for {}: recalled={}, newEase={}, newInterval={}",
            memoryId, recalled, updated.easeFactor(), updated.intervalMinutes());
    }

    /**
     * Update forgetting curves for episodic memories
     */
    private void updateEpisodicForgettingCurves(Memory memory, boolean recalled) {
        for (var episodic : memory.getLongTerm().getAllEpisodic()) {
            updateForgettingCurve(episodic.id(), StoreType.EPISODIC, recalled);
        }
    }

    /**
     * Update forgetting curves for semantic memories
     */
    private void updateSemanticForgettingCurves(Memory memory, boolean recalled) {
        for (var semantic : memory.getLongTerm().getAllSemantic()) {
            updateForgettingCurve(semantic.id(), StoreType.SEMANTIC, recalled);
        }
    }

    /**
     * Update forgetting curves for procedural memories
     */
    private void updateProceduralForgettingCurves(Memory memory, boolean recalled) {
        for (var procedural : memory.getLongTerm().getAllProcedural()) {
            updateForgettingCurve(procedural.id(), StoreType.PROCEDURAL, recalled);
        }
    }

    /**
     * Update forgetting curves for perceptive memories
     */
    private void updatePerceptiveForgettingCurves(Memory memory, boolean recalled) {
        for (var perceptive : memory.getLongTerm().getAllPerceptive()) {
            updateForgettingCurve(perceptive.id(), StoreType.PERCEPTIVE, recalled);
        }
    }

    /**
     * Process a successful recall - strengthen memory
     */
    private MemoryReviewData processSuccessfulRecall(String memoryId, StoreType storeType, MemoryReviewData current) {
        int newReviewCount = current.reviewCount() + 1;

        // Increase ease factor (memory becomes more stable)
        double newEaseFactor = current.easeFactor() + 0.1;
        newEaseFactor = Math.min(MAX_EASE_FACTOR, newEaseFactor);

        // Calculate new interval
        long newIntervalMinutes = calculateSM2Interval(newReviewCount, newEaseFactor);

        // Decay rate decreases with successful recalls (memory is more stable)
        double newDecayRate = current.decayRate() * 0.95;
        newDecayRate = Math.max(0.5, newDecayRate);

        return new MemoryReviewData(
            Instant.now(),
            newReviewCount,
            newEaseFactor,
            newIntervalMinutes,
            1.0,  // Reset to full retention after successful recall
            newDecayRate,
            Instant.now(),
            true
        );
    }

    /**
     * Process a failed recall - weaken memory, need more frequent review
     */
    private MemoryReviewData processFailedRecall(String memoryId, StoreType storeType, MemoryReviewData current) {
        // Reset review count (need to relearn)
        int newReviewCount = 0;

        // Decrease ease factor significantly (memory is less stable)
        double newEaseFactor = current.easeFactor() - 0.2;
        newEaseFactor = Math.max(MIN_EASE_FACTOR, newEaseFactor);

        // Shorten interval substantially
        long newIntervalMinutes = INITIAL_INTERVAL_MINUTES;

        // Increase decay rate (memory fades faster)
        double newDecayRate = current.decayRate() * 1.2;
        newDecayRate = Math.min(2.0, newDecayRate);

        // Calculate current retention (how much was actually remembered)
        double retentionBeforeReview = calculateSingleMemoryRetention(memoryId, storeType, current.lastReview());

        return new MemoryReviewData(
            Instant.now(),
            newReviewCount,
            newEaseFactor,
            newIntervalMinutes,
            retentionBeforeReview * 0.5,  // Penalty for forgetting
            newDecayRate,
            Instant.now(),
            false
        );
    }

    // ==================== S28-3: Utility Methods ====================

    /**
     * Get base decay rate for a memory type
     */
    private double getBaseDecayRate(StoreType storeType) {
        return DECAY_MULTIPLIERS.getOrDefault(storeType, 1.0);
    }

    /**
     * Calculate time required for retention to drop to target level
     */
    private long calculateTimeToRetention(double currentRetention, double targetRetention) {
        if (currentRetention <= targetRetention) {
            return 0;
        }

        // From R = e^(-t/S), solve for t: t = -S * ln(R)
        double stability = EBBINGHAUS_DECAY_CONSTANT * DEFAULT_EASE_FACTOR;
        double timeSeconds = -stability * Math.log(targetRetention / currentRetention);

        return (long) Math.max(0, timeSeconds);
    }

    /**
     * Get adaptive decay rate for a specific memory
     */
    public double getAdaptiveDecayRate(String memoryId) {
        MemoryReviewData data = reviewDataCache.get(memoryId);
        return data != null ? data.decayRate() : 1.0;
    }

    /**
     * Get all memories that need review (retention below threshold)
     */
    public List<String> getMemoriesNeedingReview(Memory memory) {
        List<String> needingReview = new ArrayList<>();

        // Check all memory types
        for (var episodic : memory.getLongTerm().getAllEpisodic()) {
            if (calculateSingleMemoryRetention(episodic.id(), StoreType.EPISODIC, episodic.timestamp())
                    < MIN_RETENTION_THRESHOLD) {
                needingReview.add(episodic.id());
            }
        }

        for (var semantic : memory.getLongTerm().getAllSemantic()) {
            if (calculateSingleMemoryRetention(semantic.id(), StoreType.SEMANTIC, semantic.createdAt())
                    < MIN_RETENTION_THRESHOLD) {
                needingReview.add(semantic.id());
            }
        }

        for (var procedural : memory.getLongTerm().getAllProcedural()) {
            Instant lastPracticed = procedural.lastPracticed() != null ? procedural.lastPracticed() : Instant.now();
            if (calculateSingleMemoryRetention(procedural.id(), StoreType.PROCEDURAL, lastPracticed)
                    < MIN_RETENTION_THRESHOLD) {
                needingReview.add(procedural.id());
            }
        }

        for (var perceptive : memory.getLongTerm().getAllPerceptive()) {
            if (calculateSingleMemoryRetention(perceptive.id(), StoreType.PERCEPTIVE, Instant.now())
                    < MIN_RETENTION_THRESHOLD) {
                needingReview.add(perceptive.id());
            }
        }

        return needingReview;
    }

    /**
     * Get optimization statistics
     */
    public OptimizationStats getOptimizationStats(Memory memory) {
        int totalTracked = reviewDataCache.size();
        int needingReview = memory != null ? getMemoriesNeedingReview(memory).size() : 0;
        double avgRetention = memory != null ? calculateRetentionStrength(memory) : 0.0;

        double avgEaseFactor = reviewDataCache.values().stream()
            .mapToDouble(MemoryReviewData::easeFactor)
            .average()
            .orElse(DEFAULT_EASE_FACTOR);

        return new OptimizationStats(
            totalTracked,
            needingReview,
            avgRetention,
            avgEaseFactor,
            lastOptimizationTime
        );
    }

    /**
     * Clear review data cache (for testing or reset)
     */
    public void clearCache() {
        reviewDataCache.clear();
        lastOptimizationTime = Instant.now();
        logger.info("ForgettingCurveOptimizer cache cleared");
    }

    /**
     * Get the number of tracked memories
     */
    public int getTrackedMemoryCount() {
        return reviewDataCache.size();
    }

    /**
     * Check if a specific memory is being tracked
     */
    public boolean isTracked(String memoryId) {
        return reviewDataCache.containsKey(memoryId);
    }

    /**
     * Get review data for a specific memory
     */
    public MemoryReviewData getReviewData(String memoryId) {
        return reviewDataCache.get(memoryId);
    }
}
