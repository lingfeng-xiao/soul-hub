package com.lingfeng.sprite.memory;

import com.lingfeng.sprite.perception.Stimulus;
import com.lingfeng.sprite.perception.Stimulus.StimulusType;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Sensory Memory - 30-second rolling window of raw stimuli.
 */
public final class SensoryMemory {
    public static final long WINDOW_SECONDS = 30L;
    private final ArrayDeque<Stimulus> recentStimuli = new ArrayDeque<>();
    private final long maxWindowSeconds;

    public SensoryMemory() { this(WINDOW_SECONDS); }
    public SensoryMemory(long maxWindowSeconds) { this.maxWindowSeconds = maxWindowSeconds; }

    public void add(Stimulus stimulus) {
        Objects.requireNonNull(stimulus);
        recentStimuli.addLast(stimulus);
        pruneExpired();
    }

    private void pruneExpired() {
        Instant cutoff = Instant.now().minusSeconds(maxWindowSeconds);
        while (!recentStimuli.isEmpty() && recentStimuli.peekFirst().timestamp().isBefore(cutoff)) {
            recentStimuli.removeFirst();
        }
    }

    public List<Stimulus> getRecentStimuli() { pruneExpired(); return new ArrayList<>(recentStimuli); }

    public List<Stimulus> getRecentByType(StimulusType type) {
        pruneExpired();
        return recentStimuli.stream().filter(s -> s.type() == type).collect(Collectors.toList());
    }

    public List<DetectedPattern> detectPatterns() {
        pruneExpired();
        List<DetectedPattern> patterns = new ArrayList<>();
        var byType = recentStimuli.stream().collect(Collectors.groupingBy(Stimulus::type));
        for (var entry : byType.entrySet()) {
            List<Stimulus> stimuli = entry.getValue();
            if (stimuli.size() >= 3) {
                Instant min = stimuli.stream().map(Stimulus::timestamp).min(Instant::compareTo).orElse(Instant.now());
                Instant max = stimuli.stream().map(Stimulus::timestamp).max(Instant::compareTo).orElse(Instant.now());
                patterns.add(new DetectedPattern(entry.getKey(), stimuli.size(), min, max, "Detected " + stimuli.size() + " stimuli of type " + entry.getKey()));
            }
        }
        return patterns;
    }

    public int size() { pruneExpired(); return recentStimuli.size(); }
    public void clear() { recentStimuli.clear(); }

    public record DetectedPattern(StimulusType type, int frequency, Instant firstSeen, Instant lastSeen, String description) {}
}
