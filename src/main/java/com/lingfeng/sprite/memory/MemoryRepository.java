package com.lingfeng.sprite.memory;

import com.lingfeng.sprite.perception.Stimulus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Memory Repository - Repository pattern for memory persistence operations.
 * ME-003: Database persistence layer for memory operations
 */
public final class MemoryRepository {
    private static final Logger logger = Logger.getLogger(MemoryRepository.class.getName());

    private final CopyOnWriteArrayList<StimulusRecord> sensoryStore = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<WorkingMemoryRecord> workingStore = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<EpisodicRecord> episodicStore = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<SemanticRecord> semanticStore = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ProceduralRecord> proceduralStore = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<PerceptiveRecord> perceptiveStore = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<MemoryLinkRecord> linkStore = new CopyOnWriteArrayList<>();

    // Sensory Memory
    public void saveSensory(Stimulus stimulus, Instant expiresAt) {
        sensoryStore.add(new StimulusRecord(stimulus.id(), stimulus.source(), stimulus.type().name(),
            stimulus.content().toString(), stimulus.timestamp(), stimulus.salience(), stimulus.rawRef(), expiresAt, Instant.now()));
        logger.fine("Saved sensory: " + stimulus.id());
    }

    public List<StimulusRecord> findSensoryByType(String type) {
        return sensoryStore.stream().filter(r -> r.type().equals(type)).toList();
    }

    public List<StimulusRecord> findSensoryExpired(Instant now) {
        return sensoryStore.stream().filter(r -> r.expiresAt().isBefore(now)).toList();
    }

    public void deleteSensory(String id) { sensoryStore.removeIf(r -> r.id().equals(id)); }
    public void deleteSensoryExpired(Instant now) { sensoryStore.removeIf(r -> r.expiresAt().isBefore(now)); }

    // Working Memory
    public void saveWorking(WorkingMemory.WorkingMemoryItem item, Instant expiresAt) {
        workingStore.add(new WorkingMemoryRecord(item.id(), item.content().toString(), item.abstraction(),
            item.source().id(), item.accessCount(), item.lastAccessed(), item.relevance(), item.createdAt(), expiresAt));
        logger.fine("Saved working: " + item.id());
    }

    public Optional<WorkingMemoryRecord> findWorkingById(String id) {
        return workingStore.stream().filter(r -> r.id().equals(id)).findFirst();
    }

    public List<WorkingMemoryRecord> findWorkingAll() { return List.copyOf(workingStore); }

    public void updateWorkingRelevance(String id, float relevance) {
        for (int i = 0; i < workingStore.size(); i++) {
            WorkingMemoryRecord r = workingStore.get(i);
            if (r.id().equals(id)) {
                workingStore.set(i, new WorkingMemoryRecord(r.id(), r.content(), r.abstraction(), r.sourceId(),
                    r.accessCount(), r.lastAccessed(), relevance, r.createdAt(), r.expiresAt()));
                break;
            }
        }
    }

    public void deleteWorking(String id) { workingStore.removeIf(r -> r.id().equals(id)); }

    // Episodic Memory
    public void saveEpisodic(LongTermMemory.EpisodicEntry entry) {
        episodicStore.add(new EpisodicRecord(entry.id(), entry.timestamp(), entry.location(), entry.people(),
            entry.experience(), entry.emotion(), entry.outcome(), entry.lesson(), Instant.now()));
        logger.fine("Saved episodic: " + entry.id());
    }

    public List<EpisodicRecord> findEpisodicByQuery(String query, int limit) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(query, java.util.regex.Pattern.CASE_INSENSITIVE);
        return episodicStore.stream().filter(e -> p.matcher(e.experience()).find() ||
            (e.lesson() != null && p.matcher(e.lesson()).find())).sorted((a, b) -> b.timestamp().compareTo(a.timestamp())).limit(limit).toList();
    }

    public List<EpisodicRecord> findEpisodicRecent(int days) {
        Instant cutoff = Instant.now().minus(java.time.Duration.ofDays(days));
        return episodicStore.stream().filter(e -> e.timestamp().isAfter(cutoff)).sorted((a, b) -> b.timestamp().compareTo(a.timestamp())).toList();
    }

    public List<EpisodicRecord> findEpisodicAll() { return List.copyOf(episodicStore); }

    // Semantic Memory
    public void saveSemantic(LongTermMemory.SemanticEntry entry) {
        semanticStore.removeIf(r -> r.concept().equals(entry.concept()));
        semanticStore.add(new SemanticRecord(entry.id(), entry.concept(), entry.definition(), entry.examples(),
            entry.relatedConcepts(), entry.confidence(), entry.createdAt(), entry.lastAccessed(), Instant.now()));
        logger.fine("Saved semantic: " + entry.id());
    }

    public Optional<SemanticRecord> findSemanticByConcept(String concept) {
        return semanticStore.stream().filter(r -> r.concept().equals(concept)).findFirst();
    }

    public List<SemanticRecord> findSemanticByQuery(String query) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(query, java.util.regex.Pattern.CASE_INSENSITIVE);
        return semanticStore.stream().filter(e -> p.matcher(e.concept()).find() || p.matcher(e.definition()).find()).toList();
    }

    public List<SemanticRecord> findSemanticAll() { return List.copyOf(semanticStore); }

    // Procedural Memory
    public void saveProcedural(LongTermMemory.ProceduralEntry entry) {
        proceduralStore.removeIf(r -> r.skillName().equals(entry.skillName()));
        proceduralStore.add(new ProceduralRecord(entry.id(), entry.skillName(), entry.procedure(), entry.level(),
            entry.lastPracticed(), entry.timesPerformed(), entry.successRate(), Instant.now()));
        logger.fine("Saved procedural: " + entry.id());
    }

    public Optional<ProceduralRecord> findProceduralBySkill(String skillName) {
        return proceduralStore.stream().filter(r -> r.skillName().equals(skillName)).findFirst();
    }

    public List<ProceduralRecord> findProceduralAll() { return List.copyOf(proceduralStore); }

    // Perceptive Memory
    public void savePerceptive(LongTermMemory.PerceptiveEntry entry) {
        perceptiveStore.removeIf(r -> r.pattern().equals(entry.pattern()) && r.trigger().equals(entry.trigger()));
        perceptiveStore.add(new PerceptiveRecord(entry.id(), entry.pattern(), entry.association(), entry.trigger(),
            entry.strength(), entry.timesTriggered(), Instant.now()));
        logger.fine("Saved perceptive: " + entry.id());
    }

    public List<PerceptiveRecord> findPerceptiveByPattern(String pattern) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        return perceptiveStore.stream().filter(e -> p.matcher(e.pattern()).find()).toList();
    }

    public List<PerceptiveRecord> findPerceptiveAll() { return List.copyOf(perceptiveStore); }

    // Memory Links
    public void saveLink(String sourceType, String sourceId, String targetType, String targetId, String linkType, float strength) {
        linkStore.removeIf(r -> r.sourceType().equals(sourceType) && r.sourceId().equals(sourceId) &&
            r.targetType().equals(targetType) && r.targetId().equals(targetId) && r.linkType().equals(linkType));
        linkStore.add(new MemoryLinkRecord(UUID.randomUUID().toString(), sourceType, sourceId, targetType, targetId, linkType, strength, Instant.now()));
    }

    public List<MemoryLinkRecord> findLinksBySource(String sourceType, String sourceId) {
        return linkStore.stream().filter(r -> r.sourceType().equals(sourceType) && r.sourceId().equals(sourceId)).toList();
    }

    public List<MemoryLinkRecord> findLinksByTarget(String targetType, String targetId) {
        return linkStore.stream().filter(r -> r.targetType().equals(targetType) && r.targetId().equals(targetId)).toList();
    }

    // Record Types
    public record StimulusRecord(String id, String source, String type, String content, Instant timestamp, float salience, String rawRef, Instant expiresAt, Instant createdAt) {}
    public record WorkingMemoryRecord(String id, String content, String abstraction, String sourceId, int accessCount, Instant lastAccessed, float relevance, Instant createdAt, Instant expiresAt) {}
    public record EpisodicRecord(String id, Instant timestamp, String location, List<String> people, String experience, String emotion, String outcome, String lesson, Instant createdAt) {
        public EpisodicRecord { if (people == null) people = List.of(); }
    }
    public record SemanticRecord(String id, String concept, String definition, List<String> examples, List<String> relatedConcepts, float confidence, Instant createdAt, Instant lastAccessed, Instant updatedAt) {
        public SemanticRecord { if (examples == null) examples = List.of(); if (relatedConcepts == null) relatedConcepts = List.of(); }
    }
    public record ProceduralRecord(String id, String skillName, String procedure, String level, Instant lastPracticed, int timesPerformed, float successRate, Instant updatedAt) {}
    public record PerceptiveRecord(String id, String pattern, String association, String trigger, float strength, int timesTriggered, Instant updatedAt) {}
    public record MemoryLinkRecord(String id, String sourceType, String sourceId, String targetType, String targetId, String linkType, float strength, Instant createdAt) {}
}
