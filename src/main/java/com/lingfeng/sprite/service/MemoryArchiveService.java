package com.lingfeng.sprite.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.*;

/**
 * S28-1: Memory Hierarchical Storage Strategy Service
 *
 * Implements tiered memory storage with automatic tier placement based on
 * access frequency and age. Supports archiving and restoring memories with
 * compression.
 *
 * Storage Tiers:
 * - HOT: Frequently accessed, recent memories (in-memory)
 * - WARM: Moderately accessed, aging memories (disk, uncompressed)
 * - COLD: Rarely accessed, old memories (disk, compressed)
 * - ARCHIVED: Very old or inactive memories (compressed archive)
 */
@Service
public class MemoryArchiveService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryArchiveService.class);

    // Storage tier thresholds
    private static final int HOT_ACCESS_THRESHOLD = 10;        // Access count to stay in hot
    private static final int WARM_ACCESS_THRESHOLD = 3;        // Access count to stay in warm
    private static final Duration HOT_AGE_THRESHOLD = Duration.ofDays(7);        // Max age for hot
    private static final Duration WARM_AGE_THRESHOLD = Duration.ofDays(30);       // Max age for warm
    private static final Duration COLD_AGE_THRESHOLD = Duration.ofDays(90);       // Max age for cold
    private static final Duration ARCHIVE_AGE_THRESHOLD = Duration.ofDays(180);  // Auto-archive age

    // Directory paths
    private static final String STORAGE_DIR = "data/memory-archive";
    private static final String HOT_DIR = STORAGE_DIR + "/hot";
    private static final String WARM_DIR = STORAGE_DIR + "/warm";
    private static final String COLD_DIR = STORAGE_DIR + "/cold";
    private static final String ARCHIVED_DIR = STORAGE_DIR + "/archived";

    // Optimization schedule (run every 6 hours)
    private static final int OPTIMIZATION_INTERVAL_HOURS = 6;

    private final MemorySystem.Memory memory;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Memory item metadata tracking
    private final Map<String, MemoryItemMetadata> itemMetadata = new ConcurrentHashMap<>();
    // Tiered storage index
    private final Map<String, StorageTier> tierIndex = new ConcurrentHashMap<>();

    /**
     * Storage tier enumeration
     */
    public enum StorageTier {
        HOT,    // In-memory, frequently accessed
        WARM,   // Disk, moderately accessed
        COLD,   // Disk compressed, rarely accessed
        ARCHIVED // Long-term archive, compressed
    }

    /**
     * Storage statistics record
     */
    public record StorageStats(
        int hotCount,
        int warmCount,
        int coldCount,
        int archivedCount,
        long totalSize,
        Instant lastOptimized
    ) {
        public int totalCount() {
            return hotCount + warmCount + coldCount + archivedCount;
        }
    }

    /**
     * Memory item metadata for tier management
     */
    public record MemoryItemMetadata(
        String id,
        String type,          // EPISODIC, SEMANTIC, PROCEDURAL, PERCEPTIVE
        StorageTier tier,
        int accessCount,
        Instant createdAt,
        Instant lastAccessed,
        Instant lastModified,
        long estimatedSize
    ) {
        public MemoryItemMetadata withTier(StorageTier newTier) {
            return new MemoryItemMetadata(id, type, newTier, accessCount, createdAt, lastAccessed, lastModified, estimatedSize);
        }

        public MemoryItemMetadata withAccess(int newAccessCount, Instant newLastAccessed) {
            return new MemoryItemMetadata(id, type, tier, newAccessCount, createdAt, newLastAccessed, lastModified, estimatedSize);
        }

        public Duration age() {
            return Duration.between(createdAt, Instant.now());
        }

        public Duration idleTime() {
            return Duration.between(lastAccessed, Instant.now());
        }
    }

    /**
     * Generic archived memory entry
     */
    public record ArchivedMemory(
        String id,
        String type,
        String content,
        Instant archivedAt,
        Map<String, Object> metadata
    ) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Constructor with dependency injection
     */
    public MemoryArchiveService(@Autowired MemorySystem.Memory memory) {
        this.memory = memory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ensureDirectories();
        loadMetadataIndex();
        startPeriodicOptimization();

        logger.info("MemoryArchiveService initialized - Storage tiers: HOT(<{}d, >{} accesses), WARM(<{}d, >{} accesses), COLD(<{}d), ARCHIVED(>{}d)",
            HOT_AGE_THRESHOLD.toDays(), HOT_ACCESS_THRESHOLD,
            WARM_AGE_THRESHOLD.toDays(), WARM_ACCESS_THRESHOLD,
            COLD_AGE_THRESHOLD.toDays(),
            ARCHIVE_AGE_THRESHOLD.toDays());
    }

    /**
     * Ensure all storage directories exist
     */
    private void ensureDirectories() {
        try {
            Files.createDirectories(Paths.get(HOT_DIR));
            Files.createDirectories(Paths.get(WARM_DIR));
            Files.createDirectories(Paths.get(COLD_DIR));
            Files.createDirectories(Paths.get(ARCHIVED_DIR));
            logger.info("Archive storage directories initialized");
        } catch (IOException e) {
            logger.error("Failed to create archive directories: {}", e.getMessage());
        }
    }

    /**
     * Load metadata index from disk
     */
    private void loadMetadataIndex() {
        try {
            Path indexFile = Paths.get(STORAGE_DIR, "metadata-index.json");
            if (Files.exists(indexFile)) {
                List<MemoryItemMetadata> loaded = objectMapper.readValue(
                    indexFile.toFile(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, MemoryItemMetadata.class)
                );
                for (MemoryItemMetadata meta : loaded) {
                    itemMetadata.put(meta.id(), meta);
                    tierIndex.put(meta.id(), meta.tier());
                }
                logger.info("Loaded {} memory items from archive index", loaded.size());
            }
        } catch (IOException e) {
            logger.warn("Failed to load archive metadata index: {}", e.getMessage());
        }
    }

    /**
     * Save metadata index to disk
     */
    private void saveMetadataIndex() {
        try {
            Path indexFile = Paths.get(STORAGE_DIR, "metadata-index.json");
            List<MemoryItemMetadata> allMetadata = new ArrayList<>(itemMetadata.values());
            objectMapper.writeValue(indexFile.toFile(), allMetadata);
            logger.debug("Saved metadata index with {} entries", allMetadata.size());
        } catch (IOException e) {
            logger.error("Failed to save archive metadata index: {}", e.getMessage());
        }
    }

    /**
     * Start periodic storage optimization
     */
    private void startPeriodicOptimization() {
        scheduler.scheduleAtFixedRate(
            this::optimizeStorage,
            OPTIMIZATION_INTERVAL_HOURS,
            OPTIMIZATION_INTERVAL_HOURS,
            TimeUnit.HOURS
        );
        logger.info("Periodic storage optimization started (every {} hours)", OPTIMIZATION_INTERVAL_HOURS);
    }

    // ==================== S28-1: Archive Memory ====================

    /**
     * S28-1: Archive a specific memory by ID
     *
     * @param memoryId The ID of the memory to archive
     */
    public void archiveMemory(String memoryId) {
        MemoryItemMetadata metadata = itemMetadata.get(memoryId);
        if (metadata == null) {
            logger.warn("Cannot archive unknown memory: {}", memoryId);
            return;
        }

        StorageTier currentTier = metadata.tier();
        if (currentTier == StorageTier.ARCHIVED) {
            logger.debug("Memory {} is already archived", memoryId);
            return;
        }

        try {
            // Get memory content based on type
            Object content = retrieveMemoryContent(memoryId, metadata.type());
            if (content == null) {
                logger.error("Failed to retrieve memory content for archiving: {}", memoryId);
                return;
            }

            // Create archive entry
            ArchivedMemory archived = new ArchivedMemory(
                memoryId,
                metadata.type(),
                serializeContent(content),
                Instant.now(),
                Map.of(
                    "createdAt", metadata.createdAt().toString(),
                    "accessCount", metadata.accessCount(),
                    "originalTier", currentTier.name()
                )
            );

            // Compress and save to archive
            String archivePath = ARCHIVED_DIR + "/" + memoryId + ".json.gz";
            compressAndSave(archived, archivePath);

            // Update metadata
            MemoryItemMetadata updated = metadata.withTier(StorageTier.ARCHIVED);
            itemMetadata.put(memoryId, updated);
            tierIndex.put(memoryId, StorageTier.ARCHIVED);

            // Remove from current tier storage
            removeFromTierStorage(memoryId, currentTier);

            logger.info("Archived memory {} to tier {}", memoryId, StorageTier.ARCHIVED);
        } catch (Exception e) {
            logger.error("Failed to archive memory {}: {}", memoryId, e.getMessage());
        }
    }

    // ==================== S28-1: Restore Memory ====================

    /**
     * S28-1: Restore a memory from archive
     *
     * @param memoryId The ID of the memory to restore
     * @return The restored memory object, or null if not found
     */
    public Memory restoreMemory(String memoryId) {
        MemoryItemMetadata metadata = itemMetadata.get(memoryId);
        if (metadata == null) {
            logger.warn("Cannot restore unknown memory: {}", memoryId);
            return null;
        }

        if (metadata.tier() != StorageTier.ARCHIVED) {
            logger.debug("Memory {} is not archived, no need to restore", memoryId);
            return null;
        }

        try {
            // Load and decompress from archive
            String archivePath = ARCHIVED_DIR + "/" + memoryId + ".json.gz";
            ArchivedMemory archived = loadAndDecompress(archivePath);
            if (archived == null) {
                logger.error("Failed to load archived memory: {}", memoryId);
                return null;
            }

            // Determine best tier based on current stats
            StorageTier targetTier = determineOptimalTier(metadata.withAccess(0, Instant.now()));

            // Restore to appropriate tier
            Object content = deserializeContent(archived.content(), archived.type());
            saveToTierStorage(memoryId, archived.type(), content, targetTier);

            // Update metadata
            MemoryItemMetadata updated = metadata.withTier(targetTier);
            itemMetadata.put(memoryId, updated);
            tierIndex.put(memoryId, targetTier);

            // Remove from archive
            Files.deleteIfExists(Paths.get(archivePath));

            logger.info("Restored memory {} from archive to tier {}", memoryId, targetTier);
            return content;
        } catch (Exception e) {
            logger.error("Failed to restore memory {}: {}", memoryId, e.getMessage());
            return null;
        }
    }

    /**
     * S28-1: Get storage tier statistics
     *
     * @return Storage statistics for all tiers
     */
    public StorageStats getStorageStats() {
        int hotCount = 0, warmCount = 0, coldCount = 0, archivedCount = 0;
        long totalSize = 0;

        for (MemoryItemMetadata meta : itemMetadata.values()) {
            switch (meta.tier()) {
                case HOT -> hotCount++;
                case WARM -> warmCount++;
                case COLD -> coldCount++;
                case ARCHIVED -> archivedCount++;
            }
            totalSize += meta.estimatedSize();
        }

        // Add actual file sizes for archived/cold tiers
        totalSize += calculateDirectorySize(ARCHIVED_DIR);
        totalSize += calculateDirectorySize(COLD_DIR);
        totalSize += calculateDirectorySize(WARM_DIR);

        return new StorageStats(hotCount, warmCount, coldCount, archivedCount, totalSize, Instant.now());
    }

    // ==================== S28-1: Optimize Storage ====================

    /**
     * S28-1: Optimize storage by rebalancing tiers and cleaning up
     */
    public void optimizeStorage() {
        logger.info("Starting storage optimization...");
        Instant startTime = Instant.now();

        int promoted = 0, demoted = 0, archived = 0;

        try {
            // Update access patterns from long-term memory
            updateAccessPatterns();

            // Evaluate each memory item for tier placement
            List<MemoryItemMetadata> toUpdate = new ArrayList<>();

            for (MemoryItemMetadata meta : itemMetadata.values()) {
                StorageTier optimalTier = determineOptimalTier(meta);
                if (optimalTier != meta.tier()) {
                    rebalanceTier(meta.id(), meta, optimalTier);
                    toUpdate.add(meta.withTier(optimalTier));

                    if (optimalTier.ordinal() > meta.tier().ordinal()) {
                        demoted++;
                    } else {
                        promoted++;
                    }
                }
            }

            // Update metadata
            for (MemoryItemMetadata updated : toUpdate) {
                itemMetadata.put(updated.id(), updated);
                tierIndex.put(updated.id(), updated.tier());
            }

            // Save updated index
            saveMetadataIndex();

            // Clean up orphaned files
            int cleaned = cleanupOrphanedFiles();

            Duration duration = Duration.between(startTime, Instant.now());
            logger.info("Storage optimization complete: {} promoted, {} demoted, {} archived, {} cleaned, took {}ms",
                promoted, demoted, archived, cleaned, duration.toMillis());

        } catch (Exception e) {
            logger.error("Storage optimization failed: {}", e.getMessage());
        }
    }

    /**
     * Update access patterns from long-term memory entries
     */
    private void updateAccessPatterns() {
        LongTermMemory longTerm = memory.getLongTerm();

        // Track episodic access (simplified - real impl would track actual access)
        for (EpisodicEntry entry : longTerm.getAllEpisodic()) {
            String id = entry.id();
            MemoryItemMetadata meta = itemMetadata.get(id);
            if (meta != null) {
                Instant lastAccessed = entry.timestamp(); // Simplified
                itemMetadata.put(id, meta.withAccess(meta.accessCount(), lastAccessed));
            }
        }
    }

    /**
     * Determine optimal tier for a memory item
     */
    private StorageTier determineOptimalTier(MemoryItemMetadata meta) {
        Duration age = meta.age();
        Duration idle = meta.idleTime();
        int accessCount = meta.accessCount();

        // Very old memories get archived
        if (age.compareTo(ARCHIVE_AGE_THRESHOLD) > 0) {
            return StorageTier.ARCHIVED;
        }

        // High access count keeps it hot
        if (accessCount >= HOT_ACCESS_THRESHOLD && idle.compareTo(HOT_AGE_THRESHOLD) < 0) {
            return StorageTier.HOT;
        }

        // Moderate access keeps it warm
        if (accessCount >= WARM_ACCESS_THRESHOLD || idle.compareTo(WARM_AGE_THRESHOLD) < 0) {
            return StorageTier.WARM;
        }

        // Old idle memories go cold
        if (idle.compareTo(COLD_AGE_THRESHOLD) > 0 || age.compareTo(COLD_AGE_THRESHOLD) > 0) {
            return StorageTier.COLD;
        }

        // Default to warm
        return StorageTier.WARM;
    }

    /**
     * Rebalance a memory item to a different tier
     */
    private void rebalanceTier(String memoryId, MemoryItemMetadata meta, StorageTier targetTier) {
        StorageTier sourceTier = meta.tier();

        if (sourceTier == targetTier) {
            return;
        }

        try {
            // Get content from source tier
            Object content = retrieveMemoryContent(memoryId, meta.type());
            if (content == null) {
                logger.warn("Cannot rebalance {} - content not found", memoryId);
                return;
            }

            // Remove from source tier
            removeFromTierStorage(memoryId, sourceTier);

            // Save to target tier
            saveToTierStorage(memoryId, meta.type(), content, targetTier);

            // Compress if moving to cold or archived
            if (targetTier == StorageTier.COLD || targetTier == StorageTier.ARCHIVED) {
                compressInPlace(memoryId, targetTier);
            }

            logger.debug("Rebalanced memory {} from {} to {}", memoryId, sourceTier, targetTier);
        } catch (Exception e) {
            logger.error("Failed to rebalance memory {}: {}", memoryId, e.getMessage());
        }
    }

    /**
     * Retrieve memory content by ID and type
     */
    private Object retrieveMemoryContent(String memoryId, String type) {
        LongTermMemory longTerm = memory.getLongTerm();

        return switch (type) {
            case "EPISODIC" -> longTerm.getAllEpisodic().stream()
                .filter(e -> e.id().equals(memoryId))
                .findFirst()
                .orElse(null);
            case "SEMANTIC" -> longTerm.getAllSemantic().stream()
                .filter(e -> e.id().equals(memoryId))
                .findFirst()
                .orElse(null);
            case "PROCEDURAL" -> longTerm.getAllProcedural().stream()
                .filter(e -> e.id().equals(memoryId))
                .findFirst()
                .orElse(null);
            case "PERCEPTIVE" -> longTerm.getAllPerceptive().stream()
                .filter(e -> e.id().equals(memoryId))
                .findFirst()
                .orElse(null);
            default -> null;
        };
    }

    /**
     * Save memory content to a specific tier
     */
    private void saveToTierStorage(String memoryId, String type, Object content, StorageTier tier) {
        String dir = getTierDirectory(tier);
        String filePath = dir + "/" + memoryId + ".json";

        try {
            if (tier == StorageTier.COLD || tier == StorageTier.ARCHIVED) {
                // Compress before saving
                compressAndSave(content, filePath);
            } else {
                // Direct JSON save for hot/warm
                objectMapper.writeValue(Paths.get(filePath).toFile(), content);
            }

            // Update metadata
            MemoryItemMetadata meta = itemMetadata.get(memoryId);
            if (meta != null) {
                itemMetadata.put(memoryId, meta.withTier(tier));
                tierIndex.put(memoryId, tier);
            }
        } catch (IOException e) {
            logger.error("Failed to save memory {} to tier {}: {}", memoryId, tier, e.getMessage());
        }
    }

    /**
     * Remove memory from tier storage
     */
    private void removeFromTierStorage(String memoryId, StorageTier tier) {
        String dir = getTierDirectory(tier);
        String jsonPath = dir + "/" + memoryId + ".json";
        String gzPath = dir + "/" + memoryId + ".json.gz";

        try {
            Files.deleteIfExists(Paths.get(jsonPath));
            Files.deleteIfExists(Paths.get(gzPath));
        } catch (IOException e) {
            logger.warn("Failed to remove {} from tier {}: {}", memoryId, tier, e.getMessage());
        }
    }

    /**
     * Get directory path for a tier
     */
    private String getTierDirectory(StorageTier tier) {
        return switch (tier) {
            case HOT -> HOT_DIR;
            case WARM -> WARM_DIR;
            case COLD -> COLD_DIR;
            case ARCHIVED -> ARCHIVED_DIR;
        };
    }

    /**
     * Compress content and save to file
     */
    private <T> void compressAndSave(T content, String filePath) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            objectMapper.writeValue(new OutputStreamWriter(gzos, StandardCharsets.UTF_8), content);
            gzos.finish();
            Files.write(Paths.get(filePath), baos.toByteArray());
        }
    }

    /**
     * Compress a file in place
     */
    private void compressInPlace(String memoryId, StorageTier tier) throws IOException {
        String dir = getTierDirectory(tier);
        Path jsonPath = Paths.get(dir, memoryId + ".json");
        Path gzPath = Paths.get(dir, memoryId + ".json.gz");

        if (Files.exists(jsonPath)) {
            byte[] content = Files.readAllBytes(jsonPath);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(content);
                gzos.finish();
                Files.write(gzPath, baos.toByteArray());
            }
            Files.delete(jsonPath);
        }
    }

    /**
     * Load and decompress archived memory
     */
    private <T> T loadAndDecompress(String filePath) throws IOException {
        byte[] compressed = Files.readAllBytes(Paths.get(filePath));
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             InputStreamReader reader = new InputStreamReader(gzis, StandardCharsets.UTF_8)) {
            return objectMapper.readValue(reader, (Class<T>) ArchivedMemory.class);
        }
    }

    /**
     * Serialize content to JSON string
     */
    private String serializeContent(Object content) throws IOException {
        return objectMapper.writeValueAsString(content);
    }

    /**
     * Deserialize content from JSON string
     */
    private Object deserializeContent(String json, String type) throws IOException {
        return switch (type) {
            case "EPISODIC" -> objectMapper.readValue(json, EpisodicEntry.class);
            case "SEMANTIC" -> objectMapper.readValue(json, SemanticEntry.class);
            case "PROCEDURAL" -> objectMapper.readValue(json, ProceduralEntry.class);
            case "PERCEPTIVE" -> objectMapper.readValue(json, PerceptiveEntry.class);
            default -> objectMapper.readValue(json, Object.class);
        };
    }

    /**
     * Calculate total size of a directory
     */
    private long calculateDirectorySize(String dirPath) {
        try {
            return Files.walk(Paths.get(dirPath))
                .filter(Files::isRegularFile)
                .mapToLong(f -> {
                    try {
                        return f.toFile().length();
                    } catch (Exception e) {
                        return 0L;
                    }
                })
                .sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * Cleanup orphaned files not in metadata index
     */
    private int cleanupOrphanedFiles() {
        int cleaned = 0;

        for (String dir : List.of(HOT_DIR, WARM_DIR, COLD_DIR, ARCHIVED_DIR)) {
            try (var stream = Files.list(Paths.get(dir))) {
                var files = stream.toList();
                for (Path file : files) {
                    String fileName = file.getFileName().toString();
                    String memoryId = fileName.replace(".json", "").replace(".gz", "");
                    if (!itemMetadata.containsKey(memoryId)) {
                        Files.delete(file);
                        cleaned++;
                        logger.debug("Cleaned up orphaned file: {}", file);
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to cleanup directory {}: {}", dir, e.getMessage());
            }
        }

        return cleaned;
    }

    // ==================== Helper Methods ====================

    /**
     * Register a new memory item with the archive service
     */
    public void registerMemory(String id, String type, Instant createdAt) {
        if (itemMetadata.containsKey(id)) {
            logger.debug("Memory {} already registered", id);
            return;
        }

        MemoryItemMetadata meta = new MemoryItemMetadata(
            id, type, StorageTier.HOT, 0, createdAt, createdAt, createdAt, estimateSize(type)
        );
        itemMetadata.put(id, meta);
        tierIndex.put(id, StorageTier.HOT);

        logger.debug("Registered new memory {} of type {} in HOT tier", id, type);
    }

    /**
     * Record memory access
     */
    public void recordAccess(String memoryId) {
        MemoryItemMetadata meta = itemMetadata.get(memoryId);
        if (meta != null) {
            MemoryItemMetadata updated = meta.withAccess(meta.accessCount() + 1, Instant.now());
            itemMetadata.put(memoryId, updated);
        }
    }

    /**
     * Estimate memory size based on type
     */
    private long estimateSize(String type) {
        return switch (type) {
            case "EPISODIC" -> 500;       // ~500 bytes avg
            case "SEMANTIC" -> 1000;      // ~1KB avg
            case "PROCEDURAL" -> 800;     // ~800 bytes avg
            case "PERCEPTIVE" -> 300;     // ~300 bytes avg
            default -> 500;
        };
    }

    /**
     * Shutdown the service gracefully
     */
    public void shutdown() {
        logger.info("Shutting down MemoryArchiveService...");
        scheduler.shutdown();
        saveMetadataIndex();
        logger.info("MemoryArchiveService shutdown complete");
    }
}
