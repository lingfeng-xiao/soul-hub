package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.MemorySystem;

/**
 * S31-3: Knowledge Sharing Mechanism
 *
 * Enables knowledge transfer between Sprites with privacy-aware selective sharing,
 * knowledge versioning, and conflict resolution capabilities.
 *
 * Features:
 * - Share knowledge packages between Sprites
 * - Request knowledge from other Sprites based on queries
 * - Merge incoming knowledge with privacy controls
 * - Detect and resolve conflicts in shared knowledge
 */
@Service
public class KnowledgeSharingService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeSharingService.class);

    // Integration with MemorySystem for accessing knowledge
    private final MemorySystem.Memory memory;

    // Local knowledge cache for sharing (maps spriteId to shared knowledge)
    private final ConcurrentHashMap<String, List<KnowledgePackage>> sharedKnowledgeCache = new ConcurrentHashMap<>();

    // Privacy settings per sprite (spriteId -> privacy rules)
    private final ConcurrentHashMap<String, PrivacySettings> spritePrivacySettings = new ConcurrentHashMap<>();

    // Version tracking for knowledge items
    private final ConcurrentHashMap<String, KnowledgeVersion> knowledgeVersions = new ConcurrentHashMap<>();

    // Share history for auditing
    private final CopyOnWriteArrayList<ShareRecord> shareHistory = new CopyOnWriteArrayList<>();

    // ==================== Records ====================

    /**
     * KnowledgePackage: A bundle of knowledge to be shared between Sprites
     *
     * @param id Unique identifier for this knowledge package
     * @param sourceSpriteId The Sprite that created this package
     * @param knowledge The actual knowledge content (semantic entries, procedural skills, etc.)
     * @param timestamp When this knowledge was packaged
     * @param version Version number for conflict resolution
     */
    public record KnowledgePackage(
        String id,
        String sourceSpriteId,
        List<KnowledgeItem> knowledge,
        Instant timestamp,
        int version
    ) {
        public KnowledgePackage {
            Objects.requireNonNull(id);
            Objects.requireNonNull(sourceSpriteId);
            knowledge = knowledge != null ? List.copyOf(knowledge) : List.of();
            Objects.requireNonNull(timestamp);
        }

        public KnowledgePackage withVersion(int newVersion) {
            return new KnowledgePackage(id, sourceSpriteId, knowledge, timestamp, newVersion);
        }
    }

    /**
     * KnowledgeItem: Individual piece of knowledge within a package
     *
     * @param itemId Unique identifier for this item
     * @param type The type of knowledge (SEMANTIC, PROCEDURAL, EPISODIC, PERCEPTIVE)
     * @param content The knowledge content
     * @param metadata Additional metadata (tags, source, confidence, etc.)
     * @param privacyLevel Privacy classification of this item
     */
    public record KnowledgeItem(
        String itemId,
        KnowledgeType type,
        Object content,
        Map<String, Object> metadata,
        PrivacyLevel privacyLevel
    ) {
        public KnowledgeItem {
            Objects.requireNonNull(itemId);
            Objects.requireNonNull(type);
            Objects.requireNonNull(content);
            metadata = metadata != null ? Collections.unmodifiableMap(new HashMap<>(metadata)) : Collections.emptyMap();
            privacyLevel = privacyLevel != null ? privacyLevel : PrivacyLevel.SHAREABLE;
        }

        public KnowledgeItem(String itemId, KnowledgeType type, Object content) {
            this(itemId, type, content, Collections.emptyMap(), PrivacyLevel.SHAREABLE);
        }
    }

    /**
     * KnowledgeQuery: Query parameters for requesting knowledge
     *
     * @param type Filter by knowledge type (can be null for all types)
     * @param keywords Keywords to search for
     * @param filters Additional filters for the query
     */
    public record KnowledgeQuery(
        KnowledgeType type,
        List<String> keywords,
        Map<String, Object> filters
    ) {
        public KnowledgeQuery {
            keywords = keywords != null ? List.copyOf(keywords) : List.of();
            filters = filters != null ? Collections.unmodifiableMap(new HashMap<>(filters)) : Collections.emptyMap();
        }

        public KnowledgeQuery(List<String> keywords) {
            this(null, keywords, Collections.emptyMap());
        }
    }

    /**
     * KnowledgeConflict: Represents a conflict between local and incoming knowledge
     *
     * @param localItem The local knowledge item
     * @param incomingItem The incoming knowledge item that conflicts
     * @param conflictType The type of conflict detected
     * @param resolution The resolved version (if resolved)
     */
    public record KnowledgeConflict(
        KnowledgeItem localItem,
        KnowledgeItem incomingItem,
        ConflictType conflictType,
        KnowledgeItem resolution
    ) {
        public boolean isResolved() {
            return resolution != null;
        }
    }

    /**
     * KnowledgeVersion: Tracks version history of knowledge items
     */
    public record KnowledgeVersion(
        String itemId,
        int currentVersion,
        List<VersionEntry> history
    ) {
        public KnowledgeVersion(String itemId, int currentVersion) {
            this(itemId, currentVersion, new CopyOnWriteArrayList<>());
        }

        public KnowledgeVersion incrementVersion(Object content, Instant timestamp) {
            int newVersion = currentVersion + 1;
            history.add(new VersionEntry(newVersion, content, timestamp));
            return new KnowledgeVersion(itemId, newVersion, history);
        }
    }

    /**
     * VersionEntry: A single version in the history
     */
    public record VersionEntry(
        int version,
        Object content,
        Instant timestamp
    ) {}

    // ==================== Enums ====================

    /**
     * Types of knowledge that can be shared
     */
    public enum KnowledgeType {
        SEMANTIC,   // Conceptual knowledge (facts, definitions)
        PROCEDURAL,  // Skills and processes
        EPISODIC,   // Experiences and events
        PERCEPTIVE  // Pattern associations
    }

    /**
     * Privacy levels for knowledge items
     */
    public enum PrivacyLevel {
        PRIVATE,       // Never share
        RESTRICTED,    // Share only with explicit permission
        SHAREABLE,     // Share with trusted sprites
        PUBLIC         // Share with any sprite
    }

    /**
     * Types of conflicts that can occur
     */
    public enum ConflictType {
        VERSION_CONFLICT,    // Same item with different versions
        SEMANTIC_CONFLICT,  // Contradictory knowledge
        DUPLICATE,          // Duplicate entries
        UPDATED,            // Incoming is newer version
        ORPHANED            // Local item no longer exists in source
    }

    /**
     * Resolution strategies for conflicts
     */
    public enum ResolutionStrategy {
        KEEP_LOCAL,         // Preserve local version
        ACCEPT_INCOMING,    // Accept incoming version
        MERGE_BOTH,         // Merge both versions
        NEWEST_WINS,        // Keep the newest version
        MANUAL_REVIEW       // Requires manual resolution
    }

    // ==================== Privacy Settings ====================

    /**
     * Privacy settings for a sprite
     */
    public record PrivacySettings(
        String spriteId,
        Set<String> allowedRecipients,
        Set<String> blockedRecipients,
        PrivacyLevel defaultLevel,
        Set<KnowledgeType> allowedTypes,
        Instant lastUpdated
    ) {
        public PrivacySettings(String spriteId) {
            this(spriteId, new HashSet<>(), new HashSet<>(), PrivacyLevel.SHAREABLE,
                 Set.of(KnowledgeType.values()), Instant.now());
        }

        public boolean canShareWith(String targetSpriteId, KnowledgeItem item) {
            // Check if blocked
            if (blockedRecipients.contains(targetSpriteId)) {
                return false;
            }
            // Check if specifically allowed
            if (!allowedRecipients.isEmpty() && !allowedRecipients.contains(targetSpriteId)) {
                return false;
            }
            // Check privacy level
            if (item.privacyLevel() == PrivacyLevel.PRIVATE) {
                return false;
            }
            // Check knowledge type
            if (!allowedTypes.contains(item.type())) {
                return false;
            }
            return true;
        }
    }

    // ==================== Share Record ====================

    /**
     * Record of a knowledge share operation
     */
    public record ShareRecord(
        String packageId,
        String sourceSpriteId,
        String targetSpriteId,
        int itemCount,
        Instant timestamp,
        boolean privacyChecked
    ) {}

    // ==================== Constructor ====================

    public KnowledgeSharingService(@Autowired MemorySystem.Memory memory) {
        this.memory = memory;
        logger.info("KnowledgeSharingService initialized");
    }

    // ==================== Core Methods ====================

    /**
     * S31-3: Share knowledge with a target Sprite
     *
     * @param targetSpriteId The Sprite to share knowledge with
     * @param package The knowledge package to share
     */
    public void shareKnowledge(String targetSpriteId, KnowledgePackage package_) {
        logger.info("Sharing knowledge package {} with sprite {}", package_.id(), targetSpriteId);

        // Validate target
        if (targetSpriteId == null || targetSpriteId.isBlank()) {
            throw new IllegalArgumentException("Target sprite ID cannot be null or blank");
        }

        // Filter knowledge items based on privacy settings
        List<KnowledgeItem> shareableItems = filterByPrivacy(package_.sourceSpriteId(), targetSpriteId, package_.knowledge());

        if (shareableItems.isEmpty()) {
            logger.warn("No shareable items in package {} for target {}", package_.id(), targetSpriteId);
            return;
        }

        // Create filtered package
        KnowledgePackage filteredPackage = new KnowledgePackage(
            package_.id(),
            package_.sourceSpriteId(),
            shareableItems,
            package_.timestamp(),
            package_.version()
        );

        // Store in cache
        sharedKnowledgeCache.computeIfAbsent(targetSpriteId, k -> new CopyOnWriteArrayList<>())
            .add(filteredPackage);

        // Record share history
        shareHistory.add(new ShareRecord(
            filteredPackage.id(),
            filteredPackage.sourceSpriteId(),
            targetSpriteId,
            shareableItems.size(),
            Instant.now(),
            true
        ));

        logger.info("Successfully shared {} items with sprite {}", shareableItems.size(), targetSpriteId);
    }

    /**
     * S31-3: Request knowledge from a source Sprite
     *
     * @param sourceSpriteId The Sprite to request knowledge from
     * @param query The query parameters
     * @return Matching knowledge package, or empty if none found
     */
    public KnowledgePackage requestKnowledge(String sourceSpriteId, KnowledgeQuery query) {
        logger.info("Requesting knowledge from sprite {} with query type={}, keywords={}",
            sourceSpriteId, query.type(), query.keywords());

        // Find cached knowledge from source
        List<KnowledgePackage> cached = sharedKnowledgeCache.get(sourceSpriteId);
        if (cached == null || cached.isEmpty()) {
            logger.debug("No cached knowledge found from sprite {}", sourceSpriteId);
            return null;
        }

        // Search local memory if no cached results
        List<KnowledgeItem> matchingItems = searchLocalKnowledge(query);

        if (matchingItems.isEmpty()) {
            logger.debug("No matching knowledge found for query from sprite {}", sourceSpriteId);
            return null;
        }

        // Build response package
        KnowledgePackage response = new KnowledgePackage(
            UUID.randomUUID().toString(),
            sourceSpriteId,
            matchingItems,
            Instant.now(),
            1
        );

        logger.info("Found {} matching knowledge items for request", matchingItems.size());
        return response;
    }

    /**
     * S31-3: Merge incoming knowledge into local memory
     *
     * @param incoming The knowledge package to merge
     */
    public void mergeKnowledge(KnowledgePackage incoming) {
        logger.info("Merging knowledge package {} from sprite {}", incoming.id(), incoming.sourceSpriteId());

        // Check for conflicts first
        List<KnowledgeConflict> conflicts = detectConflicts(incoming);

        if (!conflicts.isEmpty()) {
            logger.warn("Detected {} conflicts when merging package {}", conflicts.size(), incoming.id());
            // Auto-resolve if possible, otherwise keep local
            for (KnowledgeConflict conflict : conflicts) {
                if (!conflict.isResolved()) {
                    resolveConflict(conflict, ResolutionStrategy.KEEP_LOCAL);
                }
            }
        }

        // Merge non-conflicting items
        for (KnowledgeItem item : incoming.knowledge()) {
            if (!hasConflict(incoming.sourceSpriteId(), item, conflicts)) {
                mergeItem(item);
            }
        }

        // Update version tracking
        for (KnowledgeItem item : incoming.knowledge()) {
            updateVersion(item);
        }

        logger.info("Successfully merged {} items from package {}",
            incoming.knowledge().size(), incoming.id());
    }

    /**
     * S31-3: Detect conflicts between incoming and local knowledge
     *
     * @param incoming The knowledge package to check
     * @return List of detected conflicts
     */
    public List<KnowledgeConflict> detectConflicts(KnowledgePackage incoming) {
        logger.debug("Detecting conflicts for incoming package {} from {}",
            incoming.id(), incoming.sourceSpriteId());

        List<KnowledgeConflict> conflicts = new ArrayList<>();

        for (KnowledgeItem incomingItem : incoming.knowledge()) {
            // Check for version conflicts
            KnowledgeVersion localVersion = knowledgeVersions.get(incomingItem.itemId());
            if (localVersion != null && localVersion.currentVersion() != incoming.version()) {
                // Find local item
                KnowledgeItem localItem = findLocalItem(incomingItem.itemId());
                if (localItem != null) {
                    conflicts.add(new KnowledgeConflict(
                        localItem,
                        incomingItem,
                        ConflictType.VERSION_CONFLICT,
                        null
                    ));
                }
            }

            // Check for duplicates
            if (localVersion != null) {
                for (VersionEntry entry : localVersion.history()) {
                    if (entry.content().equals(incomingItem.content())) {
                        conflicts.add(new KnowledgeConflict(
                            incomingItem,
                            incomingItem,
                            ConflictType.DUPLICATE,
                            incomingItem
                        ));
                        break;
                    }
                }
            }
        }

        logger.debug("Detected {} potential conflicts", conflicts.size());
        return conflicts;
    }

    // ==================== Privacy Methods ====================

    /**
     * Set privacy settings for a sprite
     *
     * @param spriteId The sprite ID
     * @param settings The privacy settings
     */
    public void setPrivacySettings(String spriteId, PrivacySettings settings) {
        spritePrivacySettings.put(spriteId, settings);
        logger.info("Updated privacy settings for sprite {}", spriteId);
    }

    /**
     * Get privacy settings for a sprite
     *
     * @param spriteId The sprite ID
     * @return Privacy settings (default if not set)
     */
    public PrivacySettings getPrivacySettings(String spriteId) {
        return spritePrivacySettings.computeIfAbsent(spriteId, PrivacySettings::new);
    }

    /**
     * Filter knowledge items based on privacy settings
     */
    private List<KnowledgeItem> filterByPrivacy(String sourceSpriteId, String targetSpriteId,
                                                  List<KnowledgeItem> items) {
        PrivacySettings settings = getPrivacySettings(sourceSpriteId);

        return items.stream()
            .filter(item -> settings.canShareWith(targetSpriteId, item))
            .collect(Collectors.toList());
    }

    // ==================== Knowledge Search ====================

    /**
     * Search local memory for knowledge matching the query
     */
    private List<KnowledgeItem> searchLocalKnowledge(KnowledgeQuery query) {
        List<KnowledgeItem> results = new ArrayList<>();

        // Search semantic memory
        if (query.type() == null || query.type() == KnowledgeType.SEMANTIC) {
            List<MemorySystem.SemanticEntry> semanticEntries = memory.getLongTerm().recallSemantic(
                query.keywords().isEmpty() ? "" : String.join(" ", query.keywords())
            );
            for (MemorySystem.SemanticEntry entry : semanticEntries) {
                if (matchesFilters(entry, query)) {
                    results.add(semanticToKnowledgeItem(entry));
                }
            }
        }

        // Search procedural memory
        if (query.type() == null || query.type() == KnowledgeType.PROCEDURAL) {
            for (MemorySystem.ProceduralEntry entry : memory.getLongTerm().getAllProcedural()) {
                if (matchesProceduralFilters(entry, query)) {
                    results.add(proceduralToKnowledgeItem(entry));
                }
            }
        }

        // Search episodic memory
        if (query.type() == null || query.type() == KnowledgeType.EPISODIC) {
            List<MemorySystem.EpisodicEntry> episodicEntries = memory.getLongTerm().recallEpisodic(
                query.keywords().isEmpty() ? "" : String.join(" ", query.keywords())
            );
            for (MemorySystem.EpisodicEntry entry : episodicEntries) {
                results.add(episodicToKnowledgeItem(entry));
            }
        }

        return results;
    }

    private boolean matchesFilters(MemorySystem.SemanticEntry entry, KnowledgeQuery query) {
        if (query.keywords().isEmpty()) return true;
        String text = (entry.concept() + " " + entry.definition()).toLowerCase();
        return query.keywords().stream()
            .anyMatch(kw -> text.contains(kw.toLowerCase()));
    }

    private boolean matchesProceduralFilters(MemorySystem.ProceduralEntry entry, KnowledgeQuery query) {
        if (query.keywords().isEmpty()) return true;
        String text = (entry.skillName() + " " + entry.procedure()).toLowerCase();
        return query.keywords().stream()
            .anyMatch(kw -> text.contains(kw.toLowerCase()));
    }

    // ==================== Conversion Methods ====================

    private KnowledgeItem semanticToKnowledgeItem(MemorySystem.SemanticEntry entry) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("confidence", entry.confidence());
        metadata.put("examples", entry.examples());
        metadata.put("relatedConcepts", entry.relatedConcepts());

        return new KnowledgeItem(
            entry.id(),
            KnowledgeType.SEMANTIC,
            entry.definition(),
            metadata,
            PrivacyLevel.SHAREABLE
        );
    }

    private KnowledgeItem proceduralToKnowledgeItem(MemorySystem.ProceduralEntry entry) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("level", entry.level());
        metadata.put("successRate", entry.successRate());
        metadata.put("timesPerformed", entry.timesPerformed());

        return new KnowledgeItem(
            entry.id(),
            KnowledgeType.PROCEDURAL,
            entry.procedure(),
            metadata,
            PrivacyLevel.SHAREABLE
        );
    }

    private KnowledgeItem episodicToKnowledgeItem(MemorySystem.EpisodicEntry entry) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("location", entry.location());
        metadata.put("people", entry.people());
        metadata.put("emotion", entry.emotion());
        metadata.put("outcome", entry.outcome());

        return new KnowledgeItem(
            entry.id(),
            KnowledgeType.EPISODIC,
            entry.experience(),
            metadata,
            PrivacyLevel.RESTRICTED
        );
    }

    // ==================== Conflict Resolution ====================

    /**
     * Resolve a detected conflict using the specified strategy
     *
     * @param conflict The conflict to resolve
     * @param strategy The resolution strategy to use
     */
    public void resolveConflict(KnowledgeConflict conflict, ResolutionStrategy strategy) {
        logger.info("Resolving conflict {} using strategy {}", conflict.conflictType(), strategy);

        KnowledgeItem resolvedItem = switch (strategy) {
            case KEEP_LOCAL -> conflict.localItem();
            case ACCEPT_INCOMING -> conflict.incomingItem();
            case MERGE_BOTH -> mergeItems(conflict.localItem(), conflict.incomingItem());
            case NEWEST_WINS -> resolveByTimestamp(conflict);
            case MANUAL_REVIEW -> null; // Cannot auto-resolve
        };

        if (resolvedItem != null) {
            // Update the conflict with resolution
            updateVersion(resolvedItem);
            mergeItem(resolvedItem);
        }
    }

    /**
     * Merge two knowledge items
     */
    private KnowledgeItem mergeItems(KnowledgeItem local, KnowledgeItem incoming) {
        Map<String, Object> mergedMetadata = new HashMap<>(local.metadata());
        mergedMetadata.putAll(incoming.metadata());
        mergedMetadata.put("merged", true);
        mergedMetadata.put("localVersion", local.content().hashCode());
        mergedMetadata.put("incomingVersion", incoming.content().hashCode());

        return new KnowledgeItem(
            local.itemId(),
            local.type(),
            local.content() + " [Merged with: " + incoming.content() + "]",
            mergedMetadata,
            local.privacyLevel()
        );
    }

    /**
     * Resolve conflict by timestamp
     */
    private KnowledgeItem resolveByTimestamp(KnowledgeConflict conflict) {
        Object localTs = conflict.localItem().metadata().get("timestamp");
        Object incomingTs = conflict.incomingItem().metadata().get("timestamp");

        if (localTs instanceof Instant && incomingTs instanceof Instant) {
            return ((Instant) incomingTs).isAfter((Instant) localTs)
                ? conflict.incomingItem()
                : conflict.localItem();
        }
        return conflict.localItem();
    }

    /**
     * Check if an item has a conflict
     */
    private boolean hasConflict(String sourceSpriteId, KnowledgeItem item, List<KnowledgeConflict> conflicts) {
        return conflicts.stream()
            .anyMatch(c -> c.incomingItem().itemId().equals(item.itemId()));
    }

    // ==================== Version Management ====================

    /**
     * Update version tracking for an item
     */
    private void updateVersion(KnowledgeItem item) {
        KnowledgeVersion version = knowledgeVersions.computeIfAbsent(
            item.itemId(),
            id -> new KnowledgeVersion(id, 0)
        );
        knowledgeVersions.put(item.itemId(), version.incrementVersion(item.content(), Instant.now()));
    }

    /**
     * Get version info for an item
     */
    public KnowledgeVersion getVersion(String itemId) {
        return knowledgeVersions.get(itemId);
    }

    // ==================== Local Storage ====================

    /**
     * Find a local item by ID
     */
    private KnowledgeItem findLocalItem(String itemId) {
        // Search semantic memory
        for (MemorySystem.SemanticEntry entry : memory.getLongTerm().getAllSemantic()) {
            if (entry.id().equals(itemId)) {
                return semanticToKnowledgeItem(entry);
            }
        }
        // Search procedural memory
        for (MemorySystem.ProceduralEntry entry : memory.getLongTerm().getAllProcedural()) {
            if (entry.id().equals(itemId)) {
                return proceduralToKnowledgeItem(entry);
            }
        }
        return null;
    }

    /**
     * Merge a knowledge item into local memory
     */
    private void mergeItem(KnowledgeItem item) {
        switch (item.type()) {
            case SEMANTIC -> {
                MemorySystem.SemanticEntry entry = new MemorySystem.SemanticEntry(
                    item.itemId(),
                    extractConcept(item.content()),
                    item.content().toString(),
                    extractExamples(item.metadata()),
                    extractRelated(item.metadata()),
                    extractConfidence(item.metadata()),
                    Instant.now(),
                    Instant.now()
                );
                memory.getLongTerm().storeSemantic(entry);
            }
            case PROCEDURAL -> {
                MemorySystem.ProceduralEntry entry = new MemorySystem.ProceduralEntry(
                    item.itemId(),
                    extractSkillName(item.content()),
                    item.content().toString(),
                    extractLevel(item.metadata()),
                    Instant.now(),
                    extractTimesPerformed(item.metadata()),
                    extractSuccessRate(item.metadata())
                );
                memory.getLongTerm().storeProcedural(entry);
            }
            case EPISODIC -> {
                MemorySystem.EpisodicEntry entry = new MemorySystem.EpisodicEntry(
                    item.itemId(),
                    Instant.now(),
                    extractLocation(item.metadata()),
                    extractPeople(item.metadata()),
                    item.content().toString(),
                    extractEmotion(item.metadata()),
                    extractOutcome(item.metadata()),
                    null
                );
                memory.getLongTerm().storeEpisodic(entry);
            }
            case PERCEPTIVE -> {
                MemorySystem.PerceptiveEntry entry = new MemorySystem.PerceptiveEntry(
                    item.itemId(),
                    item.type().name(),
                    item.content().toString(),
                    "shared",
                    0.5f,
                    1
                );
                memory.getLongTerm().storePerceptive(entry);
            }
        }
    }

    // ==================== Metadata Extractors ====================

    private String extractConcept(Object content) {
        String text = content.toString();
        return text.length() > 100 ? text.substring(0, 100) : text;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractExamples(Map<String, Object> metadata) {
        Object examples = metadata.get("examples");
        if (examples instanceof List) {
            return (List<String>) examples;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRelated(Map<String, Object> metadata) {
        Object related = metadata.get("relatedConcepts");
        if (related instanceof List) {
            return (List<String>) related;
        }
        return List.of();
    }

    private float extractConfidence(Map<String, Object> metadata) {
        Object conf = metadata.get("confidence");
        if (conf instanceof Float) return (Float) conf;
        if (conf instanceof Number) return ((Number) conf).floatValue();
        return 0.5f;
    }

    private String extractSkillName(Object content) {
        String text = content.toString();
        return text.length() > 50 ? text.substring(0, 50) : text;
    }

    private String extractLevel(Map<String, Object> metadata) {
        Object level = metadata.get("level");
        return level != null ? level.toString() : "BASIC";
    }

    private int extractTimesPerformed(Map<String, Object> metadata) {
        Object times = metadata.get("timesPerformed");
        if (times instanceof Number) return ((Number) times).intValue();
        return 0;
    }

    private float extractSuccessRate(Map<String, Object> metadata) {
        Object rate = metadata.get("successRate");
        if (rate instanceof Float) return (Float) rate;
        if (rate instanceof Number) return ((Number) rate).floatValue();
        return 0.5f;
    }

    private String extractLocation(Map<String, Object> metadata) {
        Object loc = metadata.get("location");
        return loc != null ? loc.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractPeople(Map<String, Object> metadata) {
        Object people = metadata.get("people");
        if (people instanceof List) {
            return (List<String>) people;
        }
        return List.of();
    }

    private String extractEmotion(Map<String, Object> metadata) {
        Object emotion = metadata.get("emotion");
        return emotion != null ? emotion.toString() : null;
    }

    private String extractOutcome(Map<String, Object> metadata) {
        Object outcome = metadata.get("outcome");
        return outcome != null ? outcome.toString() : null;
    }

    // ==================== Utility Methods ====================

    /**
     * Get share history for a sprite
     */
    public List<ShareRecord> getShareHistory(String spriteId) {
        return shareHistory.stream()
            .filter(r -> r.sourceSpriteId().equals(spriteId) || r.targetSpriteId().equals(spriteId))
            .collect(Collectors.toList());
    }

    /**
     * Get all shared knowledge for a sprite
     */
    public List<KnowledgePackage> getSharedKnowledge(String spriteId) {
        return sharedKnowledgeCache.getOrDefault(spriteId, List.of());
    }

    /**
     * Clear old shared knowledge cache
     */
    public void clearOldCache(long maxAgeMinutes) {
        Instant cutoff = Instant.now().minusSeconds(maxAgeMinutes * 60);
        for (Map.Entry<String, List<KnowledgePackage>> entry : sharedKnowledgeCache.entrySet()) {
            entry.getValue().removeIf(pkg -> pkg.timestamp().isBefore(cutoff));
        }
        logger.info("Cleared old shared knowledge cache entries older than {} minutes", maxAgeMinutes);
    }

    /**
     * Create a knowledge package from local memory
     */
    public KnowledgePackage createPackage(String sourceSpriteId, List<KnowledgeItem> items) {
        return new KnowledgePackage(
            UUID.randomUUID().toString(),
            sourceSpriteId,
            items,
            Instant.now(),
            1
        );
    }

    /**
     * Get statistics about knowledge sharing
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSharedPackages", sharedKnowledgeCache.values().stream()
            .mapToInt(List::size).sum());
        stats.put("trackedVersions", knowledgeVersions.size());
        stats.put("shareHistorySize", shareHistory.size());
        stats.put("spritePrivacySettings", spritePrivacySettings.size());
        return stats;
    }
}
