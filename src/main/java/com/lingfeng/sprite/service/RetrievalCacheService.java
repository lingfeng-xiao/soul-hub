package com.lingfeng.sprite.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * S24-2: Memory Retrieval Cache Service
 *
 * Provides caching layer for memory retrieval operations with LRU eviction,
 * configurable TTL, and cache statistics tracking.
 *
 * Features:
 * - LRU (Least Recently Used) eviction policy
 * - Configurable cache size and TTL
 * - Cache invalidation on memory updates
 * - Cache hit/miss statistics
 * - Thread-safe concurrent access
 */
@Service
public class RetrievalCacheService {

    private static final Logger logger = LoggerFactory.getLogger(RetrievalCacheService.class);

    // Default configuration
    private static final int DEFAULT_MAX_SIZE = 1000;
    private static final long DEFAULT_TTL_SECONDS = 300; // 5 minutes

    // Cache storage: key -> cache entry
    private final Map<String, CacheEntry> cache;

    // LRU tracking: order of access (most recent at end)
    private final Set<String> lruOrder;

    // Configuration
    private int maxSize;
    private long ttlMillis;

    // Statistics
    private final AtomicLong statsHits;
    private final AtomicLong statsMisses;
    private final AtomicLong statsEvictions;

    // Lock for thread-safe operations
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * S24-2: Memory query for cache lookup
     */
    public record MemoryQuery(
        String type,      // Memory type: EPISODIC, SEMANTIC, PROCEDURAL, PERCEPTIVE
        String keywords,  // Search keywords
        String context,    // Additional context
        int limit          // Result limit
    ) {
        public MemoryQuery {
            if (limit <= 0) limit = 10;
        }

        /**
         * Create a cache key from query parameters
         */
        public String toCacheKey() {
            return String.format("%s:%s:%s:%d",
                type != null ? type : "ALL",
                keywords != null ? keywords : "",
                context != null ? context : "",
                limit);
        }
    }

    /**
     * S24-2: Cache statistics record
     */
    public record CacheStats(
        long hits,
        long misses,
        double hitRate,
        int size,
        long evictions
    ) {
        public static CacheStats empty() {
            return new CacheStats(0, 0, 0.0, 0, 0);
        }
    }

    /**
     * S24-2: Cached memory result entry
     */
    private record CacheEntry(
        String key,
        List<?> memories,
        long createdAt,
        long lastAccessedAt,
        int accessCount
    ) {
        public boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - createdAt > ttlMillis;
        }

        @SuppressWarnings("unchecked")
        public CacheEntry withAccessed() {
            return new CacheEntry(key, memories, createdAt, System.currentTimeMillis(), accessCount + 1);
        }
    }

    public RetrievalCacheService() {
        this(DEFAULT_MAX_SIZE, DEFAULT_TTL_SECONDS * 1000);
    }

    public RetrievalCacheService(int maxSize, long ttlMillis) {
        this.maxSize = maxSize;
        this.ttlMillis = ttlMillis;
        this.cache = new ConcurrentHashMap<>();
        this.lruOrder = Collections.synchronizedSet(new LinkedHashSet<>(maxSize));
        this.statsHits = new AtomicLong(0);
        this.statsMisses = new AtomicLong(0);
        this.statsEvictions = new AtomicLong(0);
        logger.info("RetrievalCacheService initialized with maxSize={}, ttlMs={}", maxSize, ttlMillis);
    }

    /**
     * S24-2: Get cached memories for a query
     *
     * @param query The memory query
     * @return Cached memories or empty list if not found
     */
    @SuppressWarnings("unchecked")
    public List<?> getCached(MemoryQuery query) {
        if (query == null) {
            return List.of();
        }

        String key = query.toCacheKey();

        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(key);

            if (entry == null) {
                statsMisses.incrementAndGet();
                logger.debug("Cache miss for key: {}", key);
                return List.of();
            }

            // Check expiration
            if (entry.isExpired(ttlMillis)) {
                lock.readLock().unlock();
                invalidate(key);
                statsMisses.incrementAndGet();
                logger.debug("Cache expired for key: {}", key);
                return List.of();
            }

            // Update access tracking
            lruOrder.remove(key);
            lruOrder.add(key);

            statsHits.incrementAndGet();
            logger.debug("Cache hit for key: {}, accessCount: {}", key, entry.accessCount());

            return entry.memories();

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * S24-2: Cache memories for a query
     *
     * @param key Cache key
     * @param memories List of memories to cache
     */
    @SuppressWarnings("unchecked")
    public void cache(String key, List<?> memories) {
        if (key == null || key.isEmpty() || memories == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            // Evict if necessary before adding
            while (cache.size() >= maxSize) {
                evictLRU();
            }

            CacheEntry entry = new CacheEntry(
                key,
                new ArrayList<>(memories),
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                0
            );

            CacheEntry existing = cache.put(key, entry);
            lruOrder.remove(key);
            lruOrder.add(key);

            if (existing == null) {
                logger.debug("Cached new entry for key: {}, size: {}", key, memories.size());
            } else {
                logger.debug("Updated existing cache for key: {}, size: {}", key, memories.size());
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * S24-2: Invalidate cache for a specific key
     *
     * @param key Cache key to invalidate
     */
    public void invalidate(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }

        lock.writeLock().lock();
        try {
            CacheEntry removed = cache.remove(key);
            lruOrder.remove(key);

            if (removed != null) {
                logger.debug("Invalidated cache for key: {}", key);
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * S24-2: Invalidate all caches for a specific memory type
     *
     * @param memoryType Memory type to invalidate (e.g., "EPISODIC", "SEMANTIC")
     */
    public void invalidateByType(String memoryType) {
        if (memoryType == null || memoryType.isEmpty()) {
            return;
        }

        lock.writeLock().lock();
        try {
            Set<String> keysToRemove = cache.entrySet().stream()
                .filter(e -> {
                    String key = e.getKey();
                    return key.startsWith(memoryType + ":");
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

            for (String key : keysToRemove) {
                cache.remove(key);
                lruOrder.remove(key);
            }

            logger.info("Invalidated {} cache entries for type: {}", keysToRemove.size(), memoryType);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * S24-2: Invalidate all caches (e.g., when memory is updated)
     */
    public void invalidateAll() {
        lock.writeLock().lock();
        try {
            int size = cache.size();
            cache.clear();
            lruOrder.clear();
            logger.info("Invalidated all {} cache entries", size);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * S24-2: Get cache statistics
     *
     * @return Current cache statistics
     */
    public CacheStats getStats() {
        long hits = statsHits.get();
        long misses = statsMisses.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total : 0.0;

        lock.readLock().lock();
        try {
            return new CacheStats(
                hits,
                misses,
                hitRate,
                cache.size(),
                statsEvictions.get()
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * S24-2: Reset statistics (hit/miss counters)
     */
    public void resetStats() {
        statsHits.set(0);
        statsMisses.set(0);
        logger.debug("Cache statistics reset");
    }

    /**
     * S24-2: Update cache configuration
     *
     * @param maxSize Maximum number of cache entries
     * @param ttlSeconds Time-to-live in seconds
     */
    public void updateConfig(int maxSize, long ttlSeconds) {
        lock.writeLock().lock();
        try {
            this.maxSize = maxSize;
            this.ttlMillis = ttlSeconds * 1000;

            // Evict excess entries if new size is smaller
            while (cache.size() > maxSize) {
                evictLRU();
            }

            logger.info("Cache config updated: maxSize={}, ttlSeconds={}", maxSize, ttlSeconds);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Evict the least recently used entry
     */
    private void evictLRU() {
        synchronized (lruOrder) {
            Iterator<String> iterator = lruOrder.iterator();
            if (iterator.hasNext()) {
                String lruKey = iterator.next();
                iterator.remove();
                cache.remove(lruKey);
                statsEvictions.incrementAndGet();
                logger.debug("Evicted LRU entry: {}", lruKey);
            }
        }
    }

    /**
     * Perform background cleanup of expired entries
     * Should be called periodically (e.g., by a scheduled task)
     */
    public void cleanupExpired() {
        lock.writeLock().lock();
        try {
            long now = System.currentTimeMillis();
            Set<String> expiredKeys = cache.entrySet().stream()
                .filter(e -> e.getValue().isExpired(ttlMillis))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

            for (String key : expiredKeys) {
                cache.remove(key);
                lruOrder.remove(key);
            }

            if (!expiredKeys.isEmpty()) {
                logger.info("Cleaned up {} expired cache entries", expiredKeys.size());
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get current cache size
     */
    public int getSize() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Check if cache contains a key
     */
    public boolean containsKey(String key) {
        lock.readLock().lock();
        try {
            return cache.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get cache hit ratio as percentage
     */
    public double getHitRatePercent() {
        return getStats().hitRate() * 100;
    }

    @Override
    public String toString() {
        CacheStats stats = getStats();
        return String.format("RetrievalCacheService{size=%d/%d, hits=%d, misses=%d, hitRate=%.1f%%, evictions=%d}",
            stats.size(), maxSize, stats.hits(), stats.misses(), stats.hitRate() * 100, stats.evictions());
    }
}
