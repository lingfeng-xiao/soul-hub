package com.lingfeng.sprite.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing worker agents
 * Tracks worker registration, heartbeat, and lifecycle
 */
@Component
public class AgentRegistry {
    private static final Logger logger = LoggerFactory.getLogger(AgentRegistry.class);

    private final Map<String, WorkerInfo> workers = new ConcurrentHashMap<>();
    private final Map<String, Instant> heartbeats = new ConcurrentHashMap<>();
    private static final long HEARTBEAT_TIMEOUT_MS = 30_000; // 30 seconds

    public static class WorkerInfo {
        private final String workerId;
        private final WorkerType type;
        private final Instant registeredAt;
        private volatile WorkerState state;
        private volatile Thread runningThread;

        public WorkerInfo(String workerId, WorkerType type) {
            this.workerId = workerId;
            this.type = type;
            this.registeredAt = Instant.now();
            this.state = WorkerState.REGISTERED;
        }

        public String getWorkerId() {
            return workerId;
        }

        public WorkerType getType() {
            return type;
        }

        public Instant getRegisteredAt() {
            return registeredAt;
        }

        public WorkerState getState() {
            return state;
        }

        public void setState(WorkerState state) {
            this.state = state;
        }

        public Thread getRunningThread() {
            return runningThread;
        }

        public void setRunningThread(Thread runningThread) {
            this.runningThread = runningThread;
        }
    }

    public enum WorkerState {
        REGISTERED,
        INITIALIZING,
        RUNNING,
        IDLE,
        SHUTTING_DOWN,
        TERMINATED,
        FAILED
    }

    /**
     * Register a new worker
     */
    public void register(String workerId, WorkerType type) {
        if (workers.containsKey(workerId)) {
            logger.warn("Worker {} already registered", workerId);
            return;
        }
        WorkerInfo info = new WorkerInfo(workerId, type);
        workers.put(workerId, info);
        heartbeats.put(workerId, Instant.now());
        logger.info("Worker {} registered with type {}", workerId, type);
    }

    /**
     * Deregister a worker
     */
    public void deregister(String workerId) {
        WorkerInfo info = workers.remove(workerId);
        heartbeats.remove(workerId);
        if (info != null) {
            logger.info("Worker {} deregistered", workerId);
        }
    }

    /**
     * Update worker heartbeat
     */
    public void heartbeat(String workerId) {
        heartbeats.put(workerId, Instant.now());
        WorkerInfo info = workers.get(workerId);
        if (info != null) {
            info.setState(WorkerState.IDLE);
        }
    }

    /**
     * Mark worker as running
     */
    public void markRunning(String workerId) {
        WorkerInfo info = workers.get(workerId);
        if (info != null) {
            info.setState(WorkerState.RUNNING);
            heartbeats.put(workerId, Instant.now());
        }
    }

    /**
     * Mark worker as failed
     */
    public void markFailed(String workerId) {
        WorkerInfo info = workers.get(workerId);
        if (info != null) {
            info.setState(WorkerState.FAILED);
        }
    }

    /**
     * Check if worker is alive (heartbeat within timeout)
     */
    public boolean isAlive(String workerId) {
        Instant lastHeartbeat = heartbeats.get(workerId);
        if (lastHeartbeat == null) {
            return false;
        }
        return Instant.now().toEpochMilli() - lastHeartbeat.toEpochMilli() < HEARTBEAT_TIMEOUT_MS;
    }

    /**
     * Get worker info
     */
    public Optional<WorkerInfo> getWorker(String workerId) {
        return Optional.ofNullable(workers.get(workerId));
    }

    /**
     * Get all workers by type
     */
    public List<WorkerInfo> getWorkersByType(WorkerType type) {
        return workers.values().stream()
            .filter(w -> w.getType() == type)
            .toList();
    }

    /**
     * Get all registered workers
     */
    public Collection<WorkerInfo> getAllWorkers() {
        return workers.values();
    }

    /**
     * Get worker count
     */
    public int getWorkerCount() {
        return workers.size();
    }

    /**
     * Get worker count by type
     */
    public int getWorkerCount(WorkerType type) {
        return (int) workers.values().stream()
            .filter(w -> w.getType() == type)
            .count();
    }

    /**
     * Cleanup dead workers
     */
    public void cleanupDeadWorkers() {
        List<String> deadWorkers = new ArrayList<>();
        for (String workerId : workers.keySet()) {
            if (!isAlive(workerId)) {
                deadWorkers.add(workerId);
            }
        }
        for (String workerId : deadWorkers) {
            WorkerInfo info = workers.get(workerId);
            if (info != null && info.getState() != WorkerState.SHUTTING_DOWN) {
                logger.warn("Worker {} is dead, marking as failed", workerId);
                info.setState(WorkerState.FAILED);
            }
        }
    }

    /**
     * Get status summary
     */
    public String getStatusSummary() {
        return String.format("Workers: %d registered, %d running, %d idle, %d failed",
            workers.size(),
            workers.values().stream().filter(w -> w.getState() == WorkerState.RUNNING).count(),
            workers.values().stream().filter(w -> w.getState() == WorkerState.IDLE).count(),
            workers.values().stream().filter(w -> w.getState() == WorkerState.FAILED).count());
    }
}
