package com.lingfeng.sprite.runtime;

import java.time.Instant;

/**
 * RuntimeCoordinator manages the lifecycle and health state of the Sprite runtime system.
 *
 * Responsibilities:
 * - Start, pause, resume, and stop the runtime
 * - Monitor system health and runtime state
 * - Provide snapshots of current runtime status
 * - Coordinate cycle execution through CycleDispatcher
 *
 * This interface abstracts the runtime lifecycle from the underlying Sprite implementation,
 * allowing the Sprite class to focus on cognitive processing while this handles orchestration.
 */
public interface RuntimeCoordinator {

    /**
     * Starts the runtime system.
     * @throws IllegalStateException if runtime is already running or stopped
     */
    void start();

    /**
     * Pauses the runtime, halting cycle execution but preserving state.
     * @throws IllegalStateException if runtime is not running
     */
    void pause();

    /**
     * Resumes a paused runtime from the exact state it was in.
     * @throws IllegalStateException if runtime is not paused
     */
    void resume();

    /**
     * Stops the runtime gracefully, completing any in-progress cycles.
     * After stop, the runtime cannot be resumed.
     * @throws IllegalStateException if runtime is not running or already stopped
     */
    void stop();

    /**
     * Returns a snapshot of the current runtime state.
     * @return RuntimeSnapshot containing current runtime information
     */
    RuntimeSnapshot currentSnapshot();

    /**
     * Returns the current status of the runtime.
     * @return RuntimeStatus indicating RUNNING, PAUSED, or STOPPED
     */
    RuntimeStatus getStatus();

    /**
     * Checks if the runtime is currently active (running or paused).
     * @return true if runtime is running or paused
     */
    default boolean isActive() {
        RuntimeStatus status = getStatus();
        return status == RuntimeStatus.RUNNING || status == RuntimeStatus.PAUSED;
    }

    /**
     * Returns the uptime of the runtime in milliseconds.
     * @return uptime in milliseconds, or 0 if not started
     */
    long getUptimeMs();

    /**
     * Returns the ID of the currently executing cycle, if any.
     * @return current cycle ID or null if no cycle is executing
     */
    String getCurrentCycleId();

    /**
     * Requests graceful shutdown with a timeout.
     * @param timeoutMs maximum time to wait for in-progress cycles to complete
     */
    default void shutdown(long timeoutMs) {
        stop();
    }
}
