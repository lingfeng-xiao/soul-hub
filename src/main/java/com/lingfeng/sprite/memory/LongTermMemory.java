package com.lingfeng.sprite.memory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Long-Term Memory - Persistent storage for episodic, semantic, procedural, and perceptive memories.
 */
public final class LongTermMemory {
    public static final long RETENTION_DAYS = 365L;
    private final List<EpisodicEntry> episodic = new ArrayList<>();
    private final List<SemanticEntry> semantic = new ArrayList<>();
    private final List<ProceduralEntry> procedural = new ArrayList<>();
    private final List<PerceptiveEntry> perceptive = new ArrayList<>();
    private final Map<String, List<Integer>> episodicIndex = new HashMap<>();

    public void storeEpisodic(EpisodicEntry entry) {
        Objects.requireNonNull(entry);
        episodic.add(entry);
        String key = entry.timestamp().atZone(java.time.ZoneOffset.UTC).getYear() + "-" + entry.timestamp().atZone(java.time.ZoneOffset.UTC).getMonthValue();
        List<Integer> indices = episodicIndex.getOrDefault(key, new ArrayList<>());
        indices.add(episodic.size() - 1);
        episodicIndex.put(key, indices);
    }

    public void storeSemantic(SemanticEntry entry) {
        Objects.requireNonNull(entry);
        semantic.removeIf(e -> e.concept().equals(entry.concept()));
        semantic.add(entry);
    }

    public void storeProcedural(ProceduralEntry entry) {
        Objects.requireNonNull(entry);
        procedural.removeIf(e -> e.skillName().equals(entry.skillName()));
        procedural.add(entry);
    }

    public void storePerceptive(PerceptiveEntry entry) {
        Objects.requireNonNull(entry);
        perceptive.removeIf(e -> e.pattern().equals(entry.pattern()) && e.trigger().equals(entry.trigger()));
        perceptive.add(entry);
    }

    public List<EpisodicEntry> recallEpisodic(String query, int limit) {
        Pattern p = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
        return episodic.stream().filter(e -> p.matcher(e.experience()).find() || (e.lesson() != null && p.matcher(e.lesson()).find()) || (e.emotion() != null && p.matcher(e.emotion()).find())).sorted((a, b) -> b.timestamp().compareTo(a.timestamp())).limit(limit).collect(Collectors.toList());
    }

    public List<EpisodicEntry> recallEpisodic(String query) { return recallEpisodic(query, 10); }

    public List<SemanticEntry> recallSemantic(String query) {
        Pattern p = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
        return semantic.stream().filter(e -> p.matcher(e.concept()).find() || p.matcher(e.definition()).find()).collect(Collectors.toList());
    }

    public ProceduralEntry recallProcedural(String skill) { return procedural.stream().filter(e -> e.skillName().equals(skill)).findFirst().orElse(null); }

    public List<PerceptiveEntry> recallPerceptive(String pattern) {
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        return perceptive.stream().filter(e -> p.matcher(e.pattern()).find()).collect(Collectors.toList());
    }

    public List<EpisodicEntry> getRecentEpisodic(int days) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        return episodic.stream().filter(e -> e.timestamp().isAfter(cutoff)).sorted((a, b) -> b.timestamp().compareTo(a.timestamp())).collect(Collectors.toList());
    }

    public List<EpisodicEntry> getRecentEpisodic() { return getRecentEpisodic(7); }

    public void updateSkillLevel(String skillName, String level, boolean success) {
        ProceduralEntry entry = recallProcedural(skillName);
        if (entry != null) {
            float newRate = entry.timesPerformed() > 0 ? (entry.successRate() * entry.timesPerformed() + (success ? 1f : 0f)) / (entry.timesPerformed() + 1) : (success ? 1f : 0f);
            ProceduralEntry updated = new ProceduralEntry(entry.id(), entry.skillName(), entry.procedure(), level, Instant.now(), entry.timesPerformed() + 1, newRate);
            procedural.set(procedural.indexOf(entry), updated);
        }
    }

    public MemoryStats getStats() { return new MemoryStats(episodic.size(), semantic.size(), procedural.size(), perceptive.size()); }

    public void pruneOldEntries(long retentionDays) { episodic.removeIf(e -> e.timestamp().isBefore(Instant.now().minus(Duration.ofDays(retentionDays)))); }
    public void pruneOldEntries() { pruneOldEntries(RETENTION_DAYS); }

    public List<EpisodicEntry> getAllEpisodic() { return new ArrayList<>(episodic); }
    public List<SemanticEntry> getAllSemantic() { return new ArrayList<>(semantic); }
    public List<ProceduralEntry> getAllProcedural() { return new ArrayList<>(procedural); }
    public List<PerceptiveEntry> getAllPerceptive() { return new ArrayList<>(perceptive); }

    public record EpisodicEntry(String id, Instant timestamp, String location, List<String> people, String experience, String emotion, String outcome, String lesson) {
        public EpisodicEntry { Objects.requireNonNull(id); Objects.requireNonNull(timestamp); Objects.requireNonNull(experience); if (location == null) location = null; people = people != null ? List.copyOf(people) : List.of(); if (emotion == null) emotion = null; if (outcome == null) outcome = null; if (lesson == null) lesson = null; }
        public EpisodicEntry(String id, Instant timestamp, String experience) { this(id, timestamp, null, List.of(), experience, null, null, null); }
    }

    public record SemanticEntry(String id, String concept, String definition, List<String> examples, List<String> relatedConcepts, float confidence, Instant createdAt, Instant lastAccessed) {
        public SemanticEntry { Objects.requireNonNull(id); Objects.requireNonNull(concept); Objects.requireNonNull(definition); examples = examples != null ? List.copyOf(examples) : List.of(); relatedConcepts = relatedConcepts != null ? List.copyOf(relatedConcepts) : List.of(); Objects.requireNonNull(createdAt); if (lastAccessed == null) lastAccessed = null; }
        public SemanticEntry(String id, String concept, String definition) { this(id, concept, definition, List.of(), List.of(), 0.5f, Instant.now(), null); }
    }

    public record ProceduralEntry(String id, String skillName, String procedure, String level, Instant lastPracticed, int timesPerformed, float successRate) {
        public ProceduralEntry { Objects.requireNonNull(id); Objects.requireNonNull(skillName); Objects.requireNonNull(procedure); if (level == null) level = "BASIC"; if (lastPracticed == null) lastPracticed = null; }
        public ProceduralEntry(String id, String skillName, String procedure) { this(id, skillName, procedure, "BASIC", null, 0, 0.5f); }
    }

    public record PerceptiveEntry(String id, String pattern, String association, String trigger, float strength, int timesTriggered) {
        public PerceptiveEntry { Objects.requireNonNull(id); Objects.requireNonNull(pattern); Objects.requireNonNull(association); Objects.requireNonNull(trigger); }
        public PerceptiveEntry(String id, String pattern, String association, String trigger) { this(id, pattern, association, trigger, 0.5f, 0); }
    }

    public record MemoryStats(int episodicCount, int semanticCount, int proceduralCount, int perceptiveCount) {}
}
