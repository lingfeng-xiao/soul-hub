package com.lingfeng.sprite.service;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * S24-4: Memory Usage Optimization Service
 *
 * Provides comprehensive memory management and optimization:
 * - Real-time memory usage monitoring
 * - Object pooling for frequent allocations
 * - Memory pressure detection and recommendations
 * - GC hints and optimization triggers
 * - Memory-efficient data structures
 */
@Service
public class MemoryOptimizationService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryOptimizationService.class);

    // Memory thresholds (percentages)
    private static final float HIGH_THRESHOLD = 80f;
    private static final float MEDIUM_THRESHOLD = 70f;
    private static final float LOW_THRESHOLD = 60f;

    // Pool configuration
    private static final int DEFAULT_POOL_MIN_SIZE = 10;
    private static final int DEFAULT_POOL_MAX_SIZE = 100;

    // GC notification types
    private static final String GC_NOTIFICATION_TYPE = "javax.management.Notification";
    private static final String MEMORY_THRESHOLD_EXCEEDED = "memory.threshold.exceeded";

    private final MemoryMXBean memoryBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    private final Map<String, ManagedObjectPool<?>> objectPools;
    private final AtomicLong totalAllocationsSaved = new AtomicLong(0);
    private final AtomicLong totalPoolHits = new AtomicLong(0);
    private final AtomicLong totalPoolMisses = new AtomicLong(0);

    private volatile MemoryPressureLevel lastPressureLevel = MemoryPressureLevel.NONE;
    private volatile long lastGcTime = 0;
    private volatile int consecutiveGcCount = 0;

    /**
     * Memory status record - S24-4
     */
    public record MemoryStatus(
        long used,
        long max,
        float percentage,
        GcStatus gcStatus
    ) {
        public MemoryStatus {
            if (max <= 0) max = used;
            if (percentage < 0) percentage = (float) used / max * 100;
        }
    }

    /**
     * GC status record - S24-4
     */
    public record GcStatus(
        int gcCount,
        long gcTimeMillis,
        String lastGcName,
        long lastGcCollectionCount,
        long lastGcTime,
        float gcEfficiency
    ) {}

    /**
     * Memory pressure level - S24-4
     */
    public enum MemoryPressureLevel {
        NONE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /**
     * Memory pressure record - S24-4
     */
    public record MemoryPressure(
        MemoryPressureLevel level,
        float memoryPercentage,
        List<String> recommendations,
        long timestamp
    ) {}

    /**
     * Object pool record - S24-4
     */
    public record ObjectPool<T>(
        String name,
        int size,
        int hitCount,
        int missCount,
        long allocationsSaved
    ) {
        public ObjectPool {
            if (size < 0) size = 0;
            if (hitCount < 0) hitCount = 0;
            if (missCount < 0) missCount = 0;
            if (allocationsSaved < 0) allocationsSaved = 0;
        }
    }

    /**
     * Internal pool entry with actual object storage
     */
    private static class PoolEntry<T> {
        final T object;
        final long createdAt;
        long lastUsedAt;
        int useCount;

        PoolEntry(T object) {
            this.object = object;
            this.createdAt = System.currentTimeMillis();
            this.lastUsedAt = createdAt;
            this.useCount = 0;
        }

        void markUsed() {
            lastUsedAt = System.currentTimeMillis();
            useCount++;
        }
    }

    /**
     * Object pool container with management capabilities
     */
    private class ManagedObjectPool<T> {
        final String name;
        final Supplier<T> factory;
        final List<PoolEntry<T>> available;
        final Map<T, PoolEntry<T>> inUse;
        final int minSize;
        final int maxSize;
        final AtomicLong hitCount = new AtomicLong(0);
        final AtomicLong missCount = new AtomicLong(0);
        final AtomicLong allocationsSaved = new AtomicLong(0);

        ManagedObjectPool(String name, Supplier<T> factory, int minSize, int maxSize) {
            this.name = name;
            this.factory = factory;
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.available = new ArrayList<>();
            this.inUse = new ConcurrentHashMap<>();
        }

        synchronized T acquire() {
            if (!available.isEmpty()) {
                PoolEntry<T> entry = available.remove(available.size() - 1);
                inUse.put(entry.object, entry);
                entry.markUsed();
                hitCount.incrementAndGet();
                allocationsSaved.incrementAndGet();
                return entry.object;
            }

            missCount.incrementAndGet();
            T obj = factory.get();
            PoolEntry<T> entry = new PoolEntry<>(obj);
            inUse.put(obj, entry);
            return obj;
        }

        synchronized void release(T obj) {
            PoolEntry<T> entry = inUse.remove(obj);
            if (entry != null) {
                available.add(entry);
                entry.markUsed();
            }
        }

        synchronized void shrink() {
            while (available.size() > minSize) {
                available.remove(available.size() - 1);
            }
        }

        synchronized void warmUp() {
            while (available.size() < minSize) {
                available.add(new PoolEntry<>(factory.get()));
            }
        }

        synchronized ObjectPool<T> getStats() {
            return new ObjectPool<>(
                name,
                available.size() + inUse.size(),
                (int) hitCount.get(),
                (int) missCount.get(),
                allocationsSaved.get()
            );
        }
    }

    public MemoryOptimizationService() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.objectPools = new ConcurrentHashMap<>();
        this.lastGcTime = System.currentTimeMillis();

        registerMemoryThresholdListener();
        logger.info("MemoryOptimizationService initialized - thresholds: NONE<{%, LOW<{%, MEDIUM<{%, HIGH<{%.CRITICAL",
            LOW_THRESHOLD, MEDIUM_THRESHOLD, HIGH_THRESHOLD);
    }

    /**
     * Register for memory threshold notifications from the JVM
     */
    private void registerMemoryThresholdListener() {
        try {
            if (memoryBean instanceof NotificationEmitter emitter) {
                NotificationListener listener = (notification, handback) -> {
                    if (MEMORY_THRESHOLD_EXCEEDED.equals(notification.getType())) {
                        CompositeData data = (CompositeData) notification.getUserData();
                        String poolName = (String) data.get("poolName");
                        long used = ((Long) data.get("used")).longValue();
                        long max = ((Long) data.get("max")).longValue();
                        logger.warn("Memory threshold exceeded - pool: {}, used: {}MB, max: {}MB",
                            poolName, used / (1024 * 1024), max / (1024 * 1024));
                        triggerOptimizationIfNeeded();
                    }
                };
                emitter.addNotificationListener(listener, null, null);
                logger.debug("Memory threshold listener registered");
            }
        } catch (Exception e) {
            logger.warn("Could not register memory threshold listener: {}", e.getMessage());
        }
    }

    // ==================== S24-4: Memory Status ====================

    /**
     * S24-4: Get current memory status
     */
    public MemoryStatus getMemoryStatus() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax() >= 0 ? heapUsage.getMax() : heapUsage.getCommitted();
        float percentage = max > 0 ? (float) used / max * 100 : 0;

        GcStatus gcStatus = getGcStatus();

        return new MemoryStatus(used, max, percentage, gcStatus);
    }

    /**
     * S24-4: Get GC status
     */
    private GcStatus getGcStatus() {
        long totalGcCount = 0;
        long totalGcTime = 0;
        String lastGcName = "Unknown";
        long lastGcCollectionCount = 0;
        long lastGcTime = 0;

        for (GarbageCollectorMXBean gc : gcBeans) {
            totalGcCount += gc.getCollectionCount();
            totalGcTime += gc.getCollectionTime();
            String name = gc.getName();
            if (gc.getCollectionCount() > lastGcCollectionCount) {
                lastGcCollectionCount = gc.getCollectionCount();
                lastGcName = name;
                lastGcTime = System.currentTimeMillis();
            }
        }

        float gcEfficiency = calculateGcEfficiency(totalGcCount, totalGcTime);

        return new GcStatus(
            (int) totalGcCount,
            totalGcTime,
            lastGcName,
            lastGcCollectionCount,
            lastGcTime,
            gcEfficiency
        );
    }

    /**
     * Calculate GC efficiency score (higher is better)
     */
    private float calculateGcEfficiency(long gcCount, long gcTimeMillis) {
        if (gcCount == 0) return 100f;
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        if (uptime == 0) return 100f;

        float gcRate = (float) gcCount / uptime * 1000;
        float timeRatio = (float) gcTimeMillis / uptime;

        // Efficiency: low GC rate and low time ratio = high efficiency
        float efficiency = Math.max(0, 100f - (gcRate * 10 + timeRatio * 100));
        return Math.min(100f, efficiency);
    }

    // ==================== S24-4: Memory Pressure Detection ====================

    /**
     * S24-4: Check current memory pressure level
     */
    public MemoryPressure checkPressure() {
        MemoryStatus status = getMemoryStatus();
        float percentage = status.percentage();

        MemoryPressureLevel level;
        List<String> recommendations = new ArrayList<>();

        if (percentage >= 90) {
            level = MemoryPressureLevel.CRITICAL;
            recommendations.add("CRITICAL: Immediate action required - consider full GC");
            recommendations.add("Dump memory and analyze for leaks");
            recommendations.add("Consider restarting application if pressure persists");
        } else if (percentage >= HIGH_THRESHOLD) {
            level = MemoryPressureLevel.HIGH;
            recommendations.add("HIGH: Trigger optimization immediately");
            recommendations.add("Clear unnecessary caches");
            recommendations.add("Release unused object pools");
            recommendations.add("Consider increasing heap size");
        } else if (percentage >= MEDIUM_THRESHOLD) {
            level = MemoryPressureLevel.MEDIUM;
            recommendations.add("MEDIUM: Monitor closely and optimize if trending up");
            recommendations.add("Shrink object pools to minimum size");
            recommendations.add("Clear soft/weak references");
        } else if (percentage >= LOW_THRESHOLD) {
            level = MemoryPressureLevel.LOW;
            recommendations.add("LOW: Normal operation, continue monitoring");
            recommendations.add("Good time to warm up object pools");
        } else {
            level = MemoryPressureLevel.NONE;
            recommendations.add("NONE: Memory usage is healthy");
        }

        // Check for GC pressure
        GcStatus gc = status.gcStatus();
        if (gc.gcEfficiency() < 50) {
            recommendations.add("WARNING: Low GC efficiency (" + String.format("%.1f", gc.gcEfficiency()) + "%) - consider memory optimization");
        }

        // Check for frequent GC
        long timeSinceLastGc = System.currentTimeMillis() - gc.lastGcTime();
        if (timeSinceLastGc < 5000 && gc.gcEfficiency() < 70) {
            recommendations.add("WARNING: Frequent GC detected - possible memory pressure");
        }

        lastPressureLevel = level;
        return new MemoryPressure(level, percentage, recommendations, System.currentTimeMillis());
    }

    // ==================== S24-4: Memory Optimization ====================

    /**
     * S24-4: Optimize memory usage
     */
    public void optimize() {
        logger.info("Starting memory optimization...");
        MemoryStatus status = getMemoryStatus();
        MemoryPressure pressure = checkPressure();

        logger.info("Current memory: {}% ({}/{})",
            String.format("%.1f", status.percentage()),
            formatBytes(status.used()),
            formatBytes(status.max()));

        // Shrink all object pools
        shrinkAllPools();

        // Request GC hint if pressure is medium or higher
        if (pressure.level().ordinal() >= MemoryPressureLevel.MEDIUM.ordinal()) {
            requestGcHint();
        }

        // Clear any unnecessary data structures
        performLightCleanup();

        // If critical, suggest full GC
        if (pressure.level() == MemoryPressureLevel.CRITICAL) {
            logger.warn("Critical memory pressure detected - consider calling System.gc()");
            requestFullGc();
        }

        logger.info("Memory optimization complete. New status: {}%",
            String.format("%.1f", getMemoryStatus().percentage()));
    }

    /**
     * Shrink all registered object pools to minimum size
     */
    private void shrinkAllPools() {
        objectPools.values().forEach(pool -> {
            pool.shrink();
            logger.debug("Shrunk pool: {}", pool.name);
        });
    }

    /**
     * Perform light cleanup of internal structures
     */
    private void performLightCleanup() {
        // This method can be extended to clean up other internal caches
        Runtime rt = Runtime.getRuntime();
        rt.gc();
    }

    /**
     * Request a GC hint (systemic suggestion to GC)
     */
    public void requestGcHint() {
        logger.debug("Requesting GC hint...");
        System.gc();
        lastGcTime = System.currentTimeMillis();
        consecutiveGcCount = 0;
    }

    /**
     * Request full GC (more aggressive cleanup)
     */
    private void requestFullGc() {
        logger.debug("Requesting full GC...");
        System.gc();
    }

    /**
     * Trigger optimization if memory pressure is detected
     */
    private void triggerOptimizationIfNeeded() {
        MemoryPressure pressure = checkPressure();
        if (pressure.level().ordinal() >= MemoryPressureLevel.HIGH.ordinal()) {
            logger.warn("Auto-triggering memory optimization due to {} pressure", pressure.level());
            optimize();
        }
    }

    // ==================== S24-4: Object Pooling ====================

    /**
     * S24-4: Register a new object pool
     */
    public <T> ObjectPool<T> registerPool(String name, Supplier<T> factory) {
        return registerPool(name, factory, DEFAULT_POOL_MIN_SIZE, DEFAULT_POOL_MAX_SIZE);
    }

    /**
     * S24-4: Register a new object pool with custom size
     */
    public <T> ObjectPool<T> registerPool(String name, Supplier<T> factory, int minSize, int maxSize) {
        logger.info("Registering object pool: {} (min={}, max={})", name, minSize, maxSize);

        ManagedObjectPool<T> pool = new ManagedObjectPool<>(name, factory, minSize, maxSize);
        pool.warmUp();
        objectPools.put(name, pool);

        return pool.getStats();
    }

    /**
     * S24-4: Acquire an object from a pool
     */
    @SuppressWarnings("unchecked")
    public <T> T acquire(String poolName) {
        ManagedObjectPool<T> pool = (ManagedObjectPool<T>) objectPools.get(poolName);
        if (pool == null) {
            throw new IllegalArgumentException("Pool not found: " + poolName);
        }
        return pool.acquire();
    }

    /**
     * S24-4: Release an object back to a pool
     */
    @SuppressWarnings("unchecked")
    public <T> void release(String poolName, T obj) {
        ManagedObjectPool<T> pool = (ManagedObjectPool<T>) objectPools.get(poolName);
        if (pool == null) {
            throw new IllegalArgumentException("Pool not found: " + poolName);
        }
        pool.release(obj);
    }

    /**
     * S24-4: Get statistics for a specific pool
     */
    @SuppressWarnings("unchecked")
    public <T> ObjectPool<T> getPoolStats(String poolName) {
        ManagedObjectPool<T> pool = (ManagedObjectPool<T>) objectPools.get(poolName);
        return pool != null ? pool.getStats() : null;
    }

    /**
     * S24-4: Get all registered pools
     */
    public Map<String, ObjectPool<?>> getAllPools() {
        Map<String, ObjectPool<?>> stats = new ConcurrentHashMap<>();
        objectPools.forEach((name, pool) -> {
            stats.put(name, pool.getStats());
        });
        return stats;
    }

    /**
     * Get total allocations saved through object pooling
     */
    public long getTotalAllocationsSaved() {
        return totalAllocationsSaved.get();
    }

    /**
     * Get total pool hits across all pools
     */
    public long getTotalPoolHits() {
        return totalPoolHits.get();
    }

    /**
     * Get total pool misses across all pools
     */
    public long getTotalPoolMisses() {
        return totalPoolMisses.get();
    }

    // ==================== Utility Methods ====================

    /**
     * Format bytes to human readable string
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1fMB", bytes / (1024.0 * 1024));
        return String.format("%.2fGB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Get memory pool information
     */
    public List<MemoryPoolInfo> getMemoryPools() {
        List<MemoryPoolInfo> pools = new ArrayList<>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            MemoryUsage usage = pool.getUsage();
            if (usage != null) {
                long max = usage.getMax() >= 0 ? usage.getMax() : usage.getCommitted();
                float usagePercent = max > 0 ? (float) usage.getUsed() / max * 100 : 0;
                pools.add(new MemoryPoolInfo(
                    pool.getName(),
                    usage.getUsed(),
                    max,
                    usagePercent
                ));
            }
        }
        return pools;
    }

    /**
     * Memory pool information record
     */
    public record MemoryPoolInfo(
        String name,
        long used,
        long max,
        float usagePercent
    ) {}

    /**
     * Get a comprehensive memory report
     */
    public String getMemoryReport() {
        MemoryStatus status = getMemoryStatus();
        MemoryPressure pressure = checkPressure();
        StringBuilder sb = new StringBuilder();

        sb.append("=== Memory Optimization Report ===\n\n");

        sb.append("--- Memory Status ---\n");
        sb.append(String.format("Heap Used: %s / %s (%.1f%%)\n",
            formatBytes(status.used()), formatBytes(status.max()), status.percentage()));

        sb.append("\n--- GC Status ---\n");
        sb.append(String.format("GC Count: %d\n", status.gcStatus().gcCount()));
        sb.append(String.format("GC Time: %dms\n", status.gcStatus().gcTimeMillis()));
        sb.append(String.format("Last GC: %s\n", status.gcStatus().lastGcName()));
        sb.append(String.format("GC Efficiency: %.1f%%\n", status.gcStatus().gcEfficiency()));

        sb.append("\n--- Memory Pressure ---\n");
        sb.append(String.format("Level: %s (%.1f%%)\n", pressure.level(), pressure.memoryPercentage()));
        sb.append("Recommendations:\n");
        for (String rec : pressure.recommendations()) {
            sb.append("  - ").append(rec).append("\n");
        }

        sb.append("\n--- Object Pools ---\n");
        Map<String, ObjectPool<?>> pools = getAllPools();
        if (pools.isEmpty()) {
            sb.append("No pools registered\n");
        } else {
            pools.forEach((name, pool) -> {
                sb.append(String.format("%s: size=%d, hits=%d, misses=%d, saved=%d\n",
                    name, pool.size(), pool.hitCount(), pool.missCount(), pool.allocationsSaved()));
            });
        }

        return sb.toString();
    }
}
