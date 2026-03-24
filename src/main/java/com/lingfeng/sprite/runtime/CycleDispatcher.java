package com.lingfeng.sprite.runtime;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;

/**
 * CycleDispatcher creates and manages cognitive cycles with deduplication,
 * concurrency control, and priority management.
 *
 * Responsibilities:
 * - Create global unique cycle IDs
 * - Deduplicate concurrent requests for similar cycles
 * - Manage cycle priority and concurrency limits
 * - Track active cycles and their states
 * - Coordinate with RuntimeCoordinator for lifecycle management
 */
public class CycleDispatcher {

    private final AtomicLong globalCycleCounter = new AtomicLong(0);
    private final Map<String, CycleState> activeCycles = new ConcurrentHashMap<>();
    private final Map<String, String> cycleDeduplicationCache = new ConcurrentHashMap<>();
    private final int maxConcurrentCycles;
    private final long deduplicationWindowMs;

    /**
     * Holds the state of an active cycle being tracked by the dispatcher.
     */
    public static class CycleState {
        private final String cycleId;
        private CycleSession session;
        private final Instant createdAt;
        private Instant lastUpdatedAt;
        private int priority;
        private final String requestKey; // for deduplication

        public CycleState(String cycleId, CycleSession session, String requestKey) {
            this.cycleId = cycleId;
            this.session = session;
            this.createdAt = Instant.now();
            this.lastUpdatedAt = Instant.now();
            this.priority = 0;
            this.requestKey = requestKey;
        }

        public String cycleId() { return cycleId; }
        public CycleSession session() { return session; }
        public Instant createdAt() { return createdAt; }
        public Instant lastUpdatedAt() { return lastUpdatedAt; }
        public int priority() { return priority; }
        public String requestKey() { return requestKey; }

        public void updateSession(CycleSession newSession) {
            this.session = newSession;
            this.lastUpdatedAt = Instant.now();
        }

        public void updatePriority(int priority) {
            this.priority = priority;
            this.lastUpdatedAt = Instant.now();
        }
    }

    /**
     * Creates a CycleDispatcher with default settings.
     */
    public CycleDispatcher() {
        this(10, 5000); // default: 10 concurrent cycles, 5s deduplication window
    }

    /**
     * Creates a CycleDispatcher with custom settings.
     * @param maxConcurrentCycles maximum number of cycles that can run simultaneously
     * @param deduplicationWindowMs window in milliseconds for cycle deduplication
     */
    public CycleDispatcher(int maxConcurrentCycles, long deduplicationWindowMs) {
        this.maxConcurrentCycles = maxConcurrentCycles;
        this.deduplicationWindowMs = deduplicationWindowMs;
    }

    /**
     * Creates a new cycle ID using a global counter.
     * @return globally unique cycle ID in format "cycle-{counter}-{uuid}"
     */
    public String createGlobalCycleId() {
        long counter = globalCycleCounter.incrementAndGet();
        return "cycle-" + counter + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Attempts to acquire a cycle slot, checking for duplicates and concurrency limits.
     *
     * @param requestKey unique key representing the cycle request (for deduplication)
     * @param priority priority of the cycle (higher values = higher priority)
     * @return CycleState if slot acquired, or null if deduplicated or concurrency limit reached
     */
    public CycleState tryAcquire(String requestKey, int priority) {
        // Check for deduplication
        if (requestKey != null && cycleDeduplicationCache.containsKey(requestKey)) {
            String existingCycleId = cycleDeduplicationCache.get(requestKey);
            CycleState existing = activeCycles.get(existingCycleId);
            if (existing != null && !isTerminalState(existing.session)) {
                // Duplicate request within window, return existing cycle
                return existing;
            }
            // Existing cycle is terminal, remove from cache
            cycleDeduplicationCache.remove(requestKey);
        }

        // Check concurrency limit
        if (activeCycles.size() >= maxConcurrentCycles) {
            // Try to preempt lower priority cycle
            CycleState lowestPriority = findLowestPriorityCycle();
            if (lowestPriority != null && priority > lowestPriority.priority) {
                // Preempt the lower priority cycle
                lowestPriority.updateSession(CycleSession.CANCELLED);
                activeCycles.remove(lowestPriority.cycleId());
            } else {
                return null; // Cannot acquire slot
            }
        }

        // Create new cycle
        String cycleId = createGlobalCycleId();
        CycleState state = new CycleState(cycleId, CycleSession.CREATED, requestKey);
        state.updatePriority(priority);
        activeCycles.put(cycleId, state);

        // Register for deduplication
        if (requestKey != null) {
            cycleDeduplicationCache.put(requestKey, cycleId);
        }

        return state;
    }

    /**
     * Releases a cycle slot, marking it as completed or failed.
     * @param cycleId the cycle ID to release
     * @param finalState the final state (COMPLETED, FAILED, or CANCELLED)
     */
    public void release(String cycleId, CycleSession finalState) {
        CycleState state = activeCycles.remove(cycleId);
        if (state != null && state.requestKey() != null) {
            cycleDeduplicationCache.remove(state.requestKey());
        }
    }

    /**
     * Updates the session state of an active cycle.
     * @param cycleId the cycle ID
     * @param newSession the new session state
     */
    public void updateSession(String cycleId, CycleSession newSession) {
        CycleState state = activeCycles.get(cycleId);
        if (state != null) {
            state.updateSession(newSession);
        }
    }

    /**
     * Gets the current state of a cycle.
     * @param cycleId the cycle ID
     * @return CycleState or null if not found
     */
    public CycleState getCycleState(String cycleId) {
        return activeCycles.get(cycleId);
    }

    /**
     * Gets all active cycles.
     * @return list of all active CycleStates
     */
    public List<CycleState> getActiveCycles() {
        return List.copyOf(activeCycles.values());
    }

    /**
     * Gets the count of active cycles.
     * @return number of active cycles
     */
    public int getActiveCycleCount() {
        return activeCycles.size();
    }

    /**
     * Gets the maximum concurrent cycles setting.
     * @return max concurrent cycles
     */
    public int getMaxConcurrentCycles() {
        return maxConcurrentCycles;
    }

    /**
     * Cleans up stale entries from the deduplication cache.
     * Should be called periodically.
     */
    public void cleanupStaleEntries() {
        long now = System.currentTimeMillis();
        cycleDeduplicationCache.entrySet().removeIf(entry -> {
            CycleState state = activeCycles.get(entry.getValue());
            if (state == null) return true;
            if (now - state.createdAt().toEpochMilli() > deduplicationWindowMs) {
                return true;
            }
            return isTerminalState(state.session);
        });
    }

    private boolean isTerminalState(CycleSession session) {
        return session == CycleSession.COMPLETED ||
               session == CycleSession.FAILED ||
               session == CycleSession.CANCELLED;
    }

    private CycleState findLowestPriorityCycle() {
        CycleState lowest = null;
        for (CycleState state : activeCycles.values()) {
            if (isTerminalState(state.session)) continue;
            if (lowest == null || state.priority() < lowest.priority()) {
                lowest = state;
            }
        }
        return lowest;
    }

    /**
     * Progresses a cycle through its phases in order.
     * @param cycleId the cycle to progress
     * @return true if progression successful, false if cycle not found or already terminal
     */
    public boolean progressCycle(String cycleId) {
        CycleState state = activeCycles.get(cycleId);
        if (state == null || isTerminalState(state.session)) {
            return false;
        }

        CycleSession next = switch (state.session()) {
            case CREATED -> CycleSession.COLLECTING;
            case COLLECTING -> CycleSession.REASONING;
            case REASONING -> CycleSession.DECIDING;
            case DECIDING -> CycleSession.ACTING;
            case ACTING -> CycleSession.CONSOLIDATING;
            case CONSOLIDATING -> CycleSession.COMPLETED;
            default -> null;
        };

        if (next != null) {
            state.updateSession(next);
            return true;
        }
        return false;
    }

    /**
     * Marks a cycle as failed.
     * @param cycleId the cycle ID
     */
    public void failCycle(String cycleId) {
        CycleState state = activeCycles.get(cycleId);
        if (state != null) {
            state.updateSession(CycleSession.FAILED);
        }
    }
}
