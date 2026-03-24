package com.lingfeng.sprite.memory;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.perception.Stimulus;
import com.lingfeng.sprite.perception.StimulusType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Memory Write Pipeline - Processes raw stimuli through cleaning, classification,
 * deduplication, summarization, and embedding stages before writing to memory.
 * ME-003: Memory write pipeline implementation
 */
public final class MemoryWritePipeline {

    private final SensoryMemory sensory;
    private final WorkingMemory working;
    private final LongTermMemory longTerm;
    private final MemoryRepository repository;
    private Consumer<PipelineStage> stageListeners;
    private float deduplicationThreshold = 0.85f;
    private int maxSummaryLength = 200;

    public MemoryWritePipeline() {
        this.sensory = new SensoryMemory();
        this.working = new WorkingMemory();
        this.longTerm = new LongTermMemory();
        this.repository = new MemoryRepository();
    }

    public MemoryWritePipeline(MemoryRepository repository) {
        this.sensory = new SensoryMemory();
        this.working = new WorkingMemory();
        this.longTerm = new LongTermMemory();
        this.repository = Objects.requireNonNull(repository);
    }

    /**
     * Process a raw stimulus through the complete pipeline.
     */
    public PipelineResult processStimulus(Stimulus rawStimulus) {
        Objects.requireNonNull(rawStimulus);
        long startTime = System.currentTimeMillis();
        PipelineContext ctx = new PipelineContext(rawStimulus);

        notifyStage(PipelineStage.CLEANING);
        ctx = stageClean(ctx);
        if (ctx.isRejected()) return createRejectedResult(ctx, startTime);

        notifyStage(PipelineStage.CLASSIFYING);
        ctx = stageClassify(ctx);

        notifyStage(PipelineStage.DEDUPLICATING);
        ctx = stageDeduplicate(ctx);
        if (ctx.isDuplicate()) return createDuplicateResult(ctx, startTime);

        notifyStage(PipelineStage.SUMMARIZING);
        ctx = stageSummarize(ctx);

        notifyStage(PipelineStage.WRITING);
        ctx = stageWrite(ctx);

        notifyStage(PipelineStage.EMBEDDING);
        ctx = stageEmbed(ctx);

        return new PipelineResult(ctx, true, System.currentTimeMillis() - startTime, null);
    }

    /**
     * Consolidate high-relevance working memory items to long-term memory.
     */
    public List<LongTermMemoryEntry> consolidateToLongTerm(float retentionThreshold) {
        List<LongTermMemoryEntry> consolidated = new ArrayList<>();
        for (WorkingMemory.WorkingMemoryItem item : working.getAll()) {
            if (item.relevance() >= retentionThreshold) {
                LongTermMemoryEntry entry = consolidateItem(item);
                if (entry != null) consolidated.add(entry);
            }
        }
        return consolidated;
    }

    private LongTermMemoryEntry consolidateItem(WorkingMemory.WorkingMemoryItem item) {
        MemorySystem.StoreType storeType = classifyForLongTerm(item);
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();

        switch (storeType) {
            case EPISODIC:
                LongTermMemory.EpisodicEntry episodic = new LongTermMemory.EpisodicEntry(
                    id, item.source().timestamp(), null, List.of(), item.abstraction(), extractEmotion(item), null, null);
                longTerm.storeEpisodic(episodic);
                return new LongTermMemoryEntry(id, storeType, episodic);
            case SEMANTIC:
                LongTermMemory.SemanticEntry semantic = new LongTermMemory.SemanticEntry(
                    id, item.abstraction(), item.content().toString());
                longTerm.storeSemantic(semantic);
                return new LongTermMemoryEntry(id, storeType, semantic);
            case PROCEDURAL:
                LongTermMemory.ProceduralEntry procedural = new LongTermMemory.ProceduralEntry(
                    id, item.abstraction(), item.content().toString());
                longTerm.storeProcedural(procedural);
                return new LongTermMemoryEntry(id, storeType, procedural);
            case PERCEPTIVE:
                LongTermMemory.PerceptiveEntry perceptive = new LongTermMemory.PerceptiveEntry(
                    id, item.source().type().name(), item.abstraction(), item.source().source());
                longTerm.storePerceptive(perceptive);
                return new LongTermMemoryEntry(id, storeType, perceptive);
            default:
                return null;
        }
    }

    private MemorySystem.StoreType classifyForLongTerm(WorkingMemory.WorkingMemoryItem item) {
        String abs = item.abstraction().toLowerCase();
        if (item.content() instanceof String str) {
            if (str.contains("skill") || str.contains("how to")) return MemorySystem.StoreType.PROCEDURAL;
            if (abs.contains("pattern") || abs.contains("trigger")) return MemorySystem.StoreType.PERCEPTIVE;
            if (str.length() < 50 && abs.length() < 30) return MemorySystem.StoreType.SEMANTIC;
        }
        return MemorySystem.StoreType.EPISODIC;
    }

    private String extractEmotion(WorkingMemory.WorkingMemoryItem item) {
        var ctx = item.source().contextFragments();
        return ctx != null && ctx.containsKey("emotion") ? ctx.get("emotion") : null;
    }

    private PipelineContext stageClean(PipelineContext ctx) {
        Object content = ctx.stimulus().content();
        if (content instanceof String str) {
            String cleaned = str.trim().replaceAll("\\s+", " ");
            if (cleaned.isEmpty()) ctx.reject("Empty content after cleaning");
            ctx.cleanedContent(cleaned);
        } else {
            ctx.cleanedContent(content);
        }
        return ctx;
    }

    private PipelineContext stageClassify(PipelineContext ctx) {
        ctx.classifiedSalience(calculateBaseSalience(ctx.stimulus().type()));
        return ctx;
    }

    private PipelineContext stageDeduplicate(PipelineContext ctx) {
        for (Stimulus existing : sensory.getRecentStimuli()) {
            if (existing.id().equals(ctx.stimulus().id())) continue;
            float sim = calculateSimilarity(ctx.stimulus().content().toString(), existing.content().toString());
            if (sim >= deduplicationThreshold) { ctx.markDuplicate(existing.id()); return ctx; }
        }
        return ctx;
    }

    private PipelineContext stageSummarize(PipelineContext ctx) {
        Object content = ctx.cleanedContent();
        if (content instanceof String str && str.length() > maxSummaryLength) {
            ctx.summarizedContent(str.substring(0, maxSummaryLength) + "...");
        } else {
            ctx.summarizedContent(content);
        }
        return ctx;
    }

    private PipelineContext stageWrite(PipelineContext ctx) {
        sensory.add(ctx.stimulus());
        ctx.writtenToSensory(true);
        if (ctx.classifiedSalience() > 0.6f) {
            WorkingMemory.WorkingMemoryItem wmItem = new WorkingMemory.WorkingMemoryItem(
                UUID.randomUUID().toString(), ctx.summarizedContent(), generateAbstraction(ctx),
                ctx.stimulus(), ctx.classifiedSalience());
            working.add(wmItem);
            ctx.createdWorkingItem(wmItem);
        }
        return ctx;
    }

    private PipelineContext stageEmbed(PipelineContext ctx) {
        ctx.embedding(Integer.toHexString(ctx.summarizedContent().toString().hashCode()));
        return ctx;
    }

    private float calculateBaseSalience(StimulusType type) {
        return switch (type) {
            case EMOTIONAL -> 0.9f; case COMMAND -> 0.8f; case SYSTEM -> 0.7f;
            case ENVIRONMENT -> 0.6f; case TEXT -> 0.5f; case VISUAL, AUDITORY -> 0.4f;
        };
    }

    private float calculateSimilarity(String a, String b) {
        if (a == null || b == null) return 0f;
        if (a.equals(b)) return 1f;
        java.util.Set<String> wordsA = java.util.Arrays.stream(a.split("\\s+")).map(String::toLowerCase).collect(Collectors.toSet());
        java.util.Set<String> wordsB = java.util.Arrays.stream(b.split("\\s+")).map(String::toLowerCase).collect(Collectors.toSet());
        java.util.Set<String> intersection = new java.util.HashSet<>(wordsA); intersection.retainAll(wordsB);
        java.util.Set<String> union = new java.util.HashSet<>(wordsA); union.addAll(wordsB);
        return union.isEmpty() ? 0f : (float) intersection.size() / union.size();
    }

    private String generateAbstraction(PipelineContext ctx) {
        Object content = ctx.summarizedContent();
        if (content instanceof String str) {
            String[] words = str.split("\\s+");
            if (words.length > 0) return words[0] + (words.length > 1 ? "..." : "");
        }
        return "stimulus-" + ctx.stimulus().type().name().toLowerCase();
    }

    private void notifyStage(PipelineStage stage) { if (stageListeners != null) stageListeners.accept(stage); }
    private PipelineResult createRejectedResult(PipelineContext ctx, long startTime) {
        return new PipelineResult(ctx, false, System.currentTimeMillis() - startTime, ctx.rejectionReason());
    }
    private PipelineResult createDuplicateResult(PipelineContext ctx, long startTime) {
        return new PipelineResult(ctx, true, System.currentTimeMillis() - startTime, "Duplicate of " + ctx.duplicateOf());
    }

    public SensoryMemory getSensory() { return sensory; }
    public WorkingMemory getWorking() { return working; }
    public LongTermMemory getLongTerm() { return longTerm; }
    public MemoryRepository getRepository() { return repository; }
    public void setDeduplicationThreshold(float threshold) { this.deduplicationThreshold = Math.max(0f, Math.min(1f, threshold)); }
    public void setMaxSummaryLength(int maxLength) { this.maxSummaryLength = Math.max(10, maxLength); }
    public void addStageListener(Consumer<PipelineStage> listener) { this.stageListeners = listener; }

    private static class PipelineContext {
        private final Stimulus stimulus;
        private Object cleanedContent, summarizedContent, workingItem;
        private float classifiedSalience;
        private boolean isDuplicate, writtenToSensory, rejected;
        private String duplicateOf, rejectionReason, embedding;

        PipelineContext(Stimulus stimulus) { this.stimulus = stimulus; }
        Stimulus stimulus() { return stimulus; }
        Object cleanedContent() { return cleanedContent; }
        void cleanedContent(Object c) { this.cleanedContent = c; }
        float classifiedSalience() { return classifiedSalience; }
        void classifiedSalience(float s) { this.classifiedSalience = s; }
        boolean isDuplicate() { return isDuplicate; }
        void markDuplicate(String id) { this.isDuplicate = true; this.duplicateOf = id; }
        String duplicateOf() { return duplicateOf; }
        Object summarizedContent() { return summarizedContent; }
        void summarizedContent(Object c) { this.summarizedContent = c; }
        boolean writtenToSensory() { return writtenToSensory; }
        void writtenToSensory(boolean w) { this.writtenToSensory = w; }
        Object workingItem() { return workingItem; }
        void createdWorkingItem(Object i) { this.workingItem = i; }
        String embedding() { return embedding; }
        void embedding(String e) { this.embedding = e; }
        boolean isRejected() { return rejected; }
        String rejectionReason() { return rejectionReason; }
        void reject(String reason) { this.rejected = true; this.rejectionReason = reason; }
    }

    public record PipelineResult(PipelineContext context, boolean success, long processingTimeMs, String message) {
        public boolean isDuplicate() { return context.isDuplicate(); }
        public String duplicateOf() { return context.duplicateOf(); }
        public Stimulus processedStimulus() { return context.stimulus(); }
        public Object workingMemoryItem() { return context.workingItem(); }
    }

    public record LongTermMemoryEntry(String id, MemorySystem.StoreType storeType, Object entry) {}
    public enum PipelineStage { CLEANING, CLASSIFYING, DEDUPLICATING, SUMMARIZING, WRITING, EMBEDDING }
}
