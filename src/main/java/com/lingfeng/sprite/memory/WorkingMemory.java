package com.lingfeng.sprite.memory;

import com.lingfeng.sprite.perception.Stimulus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Working Memory - Current session memory with 7-item limit (Miller's Law).
 */
public final class WorkingMemory {
    public static final int MAX_ITEMS = 7;
    private final List<WorkingMemoryItem> items = new ArrayList<>();
    private final int maxItems;

    public WorkingMemory() { this(MAX_ITEMS); }
    public WorkingMemory(int maxItems) { this.maxItems = maxItems; }

    public void add(WorkingMemoryItem item) {
        Objects.requireNonNull(item);
        items.removeIf(it -> it.id().equals(item.id()));
        items.add(item);
        pruneIfNeeded();
    }

    public WorkingMemoryItem consolidate(SensoryMemory.DetectedPattern pattern, String abstraction, Object content) {
        Stimulus source = Stimulus.create("sensory-memory", pattern.type(), content);
        WorkingMemoryItem item = new WorkingMemoryItem(UUID.randomUUID().toString(), content, abstraction, source, 0, Instant.now(), 0.7f, Instant.now());
        add(item);
        return item;
    }

    private void pruneIfNeeded() {
        while (items.size() > maxItems) {
            items.sort((a, b) -> { int cmp = Float.compare(a.relevance(), b.relevance()); return cmp != 0 ? cmp : Integer.compare(a.accessCount(), b.accessCount()); });
            items.remove(0);
        }
    }

    public WorkingMemoryItem access(String id) {
        for (int i = 0; i < items.size(); i++) {
            WorkingMemoryItem item = items.get(i);
            if (item.id().equals(id)) {
                WorkingMemoryItem updated = new WorkingMemoryItem(item.id(), item.content(), item.abstraction(), item.source(), item.accessCount() + 1, Instant.now(), item.relevance(), item.createdAt());
                items.set(i, updated);
                return updated;
            }
        }
        return null;
    }

    public void updateRelevance(String id, float relevance) {
        for (int i = 0; i < items.size(); i++) {
            WorkingMemoryItem item = items.get(i);
            if (item.id().equals(id)) {
                items.set(i, new WorkingMemoryItem(item.id(), item.content(), item.abstraction(), item.source(), item.accessCount(), item.lastAccessed(), relevance, item.createdAt()));
                break;
            }
        }
    }

    public List<WorkingMemoryItem> recall(String query) {
        Pattern p = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
        return items.stream().filter(it -> p.matcher(it.abstraction()).find() || p.matcher(it.content().toString()).find()).sorted((a, b) -> Float.compare(b.relevance(), a.relevance())).collect(Collectors.toList());
    }

    public List<WorkingMemoryItem> getAll() { return new ArrayList<>(items); }
    public int size() { return items.size(); }
    public void clear() { items.clear(); }

    public record WorkingMemoryItem(String id, Object content, String abstraction, Stimulus source, int accessCount, Instant lastAccessed, float relevance, Instant createdAt) {
        public WorkingMemoryItem { Objects.requireNonNull(id); Objects.requireNonNull(content); Objects.requireNonNull(abstraction); Objects.requireNonNull(source); Objects.requireNonNull(lastAccessed); Objects.requireNonNull(createdAt); }
        public WorkingMemoryItem(String id, Object content, String abstraction, Stimulus source) { this(id, content, abstraction, source, 0, Instant.now(), 0.5f, Instant.now()); }
        public WorkingMemoryItem(String id, Object content, String abstraction, Stimulus source, float relevance) { this(id, content, abstraction, source, 0, Instant.now(), relevance, Instant.now()); }
    }
}
