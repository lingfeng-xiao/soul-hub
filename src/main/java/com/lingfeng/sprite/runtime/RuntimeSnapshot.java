package com.lingfeng.sprite.runtime;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * RuntimeSnapshot captures the complete state of the runtime at a point in time.
 * This is used for monitoring, debugging, and state recovery.
 */
public record RuntimeSnapshot(
    /** Current runtime status */
    RuntimeStatus status,

    /** ID of the currently executing cycle, if any */
    String currentCycleId,

    /** Total number of cycles executed since startup */
    long totalCyclesExecuted,

    /** Total number of cycles that failed */
    long totalCyclesFailed,

    /** Timestamp of the last completed cycle */
    Instant lastCycleTime,

    /** Timestamp when runtime was started */
    Instant startTime,

    /** Current uptime in milliseconds */
    long uptimeMs,

    /** Active cycles currently in progress */
    List<ActiveCycleInfo> activeCycles,

    /** Recent cycle summaries */
    List<CycleSummary> recentCycles,

    /** Runtime health indicators */
    Map<String, Object> healthMetrics,

    /** System resource usage metrics */
    ResourceUsage resourceUsage
) {
    /**
     * Information about an active cycle in progress.
     */
    public record ActiveCycleInfo(
        String cycleId,
        CycleSession state,
        Instant startTime,
        long elapsedMs,
        String currentPhase
    ) {}

    /**
     * Summary of a completed or failed cycle.
     */
    public record CycleSummary(
        String cycleId,
        CycleSession finalState,
        Instant startTime,
        Instant endTime,
        long durationMs,
        boolean success
    ) {}

    /**
     * System resource usage information.
     */
    public record ResourceUsage(
        double cpuUsagePercent,
        long memoryUsedBytes,
        long memoryTotalBytes,
        int activeThreads
    ) {}

    /**
     * Creates an empty snapshot for when runtime is not initialized.
     */
    public static RuntimeSnapshot empty() {
        return new RuntimeSnapshot(
            RuntimeStatus.STOPPED,
            null,
            0,
            0,
            null,
            null,
            0,
            List.of(),
            List.of(),
            Map.of(),
            new ResourceUsage(0, 0, 0, 0)
        );
    }
}
