package com.lingfeng.sprite.skill;

import java.time.Instant;
import java.util.Map;

/**
 * Result of skill execution
 */
public record SkillResult(
    boolean success,
    String message,
    Object data,
    Map<String, String> metadata,
    Instant completedAt,
    long durationMs
) {
    /**
     * Create a successful result
     */
    public static SkillResult success(String message) {
        return new SkillResult(true, message, null, Map.of(), Instant.now(), 0);
    }

    /**
     * Create a successful result with data
     */
    public static SkillResult success(String message, Object data) {
        return new SkillResult(true, message, data, Map.of(), Instant.now(), 0);
    }

    /**
     * Create a successful result with data and metadata
     */
    public static SkillResult success(String message, Object data, Map<String, String> metadata) {
        return new SkillResult(true, message, data, metadata, Instant.now(), 0);
    }

    /**
     * Create a failed result
     */
    public static SkillResult failure(String message) {
        return new SkillResult(false, message, null, Map.of(), Instant.now(), 0);
    }

    /**
     * Create a failed result with data
     */
    public static SkillResult failure(String message, Object data) {
        return new SkillResult(false, message, data, Map.of(), Instant.now(), 0);
    }

    /**
     * Create a result with duration
     */
    public SkillResult withDuration(long durationMs) {
        return new SkillResult(success, message, data, metadata, completedAt, durationMs);
    }

    /**
     * Add metadata
     */
    public SkillResult withMetadata(String key, String value) {
        Map<String, String> newMeta = new java.util.HashMap<>(metadata);
        newMeta.put(key, value);
        return new SkillResult(success, message, data, newMeta, completedAt, durationMs);
    }
}
