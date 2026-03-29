package com.lingfeng.sprite.domain.memory.life;

import java.time.Instant;
import java.util.Objects;

/**
 * GrowthMemory - 成长记忆
 *
 * 与成长、反思、变化有关的关键记忆。
 * 这些记忆记录 Sprite 的学习和进化过程。
 *
 * 对应 IGN-030
 */
public final class GrowthMemory implements LifeMemory {

    /**
     * 成长模式类型
     */
    public enum GrowthPattern {
        SKILL_ACQUIRED,       // 技能习得
        PATTERN_RECOGNIZED,   // 模式识别
        BEHAVIOR_CORRECTED,   // 行为修正
        INSIGHT_GAINED,       // 洞察获得
        LIMITATION_UNDERSTOOD, // 局限理解
        PREFERENCE_LEARNED,   // 偏好学习
        STRATEGY_ADOPTED,     // 策略采纳
        BELIEF_UPDATED        // 信念更新
    }

    /**
     * 成长变化方向
     */
    public enum GrowthDirection {
        POSITIVE,   // 正面成长
        NEGATIVE,   // 负面退化
        NEUTRAL     // 中性变化
    }

    private final String memoryId;
    private final String content;
    private final Instant occurredAt;
    private final GrowthPattern pattern;
    private final GrowthDirection direction;
    private final String significance;
    private final boolean stillRelevant;
    private final float confidence;  // 这个记忆的置信度 0-1

    private GrowthMemory(Builder builder) {
        this.memoryId = builder.memoryId;
        this.content = builder.content;
        this.occurredAt = builder.occurredAt;
        this.pattern = builder.pattern;
        this.direction = builder.direction;
        this.significance = builder.significance;
        this.stillRelevant = builder.stillRelevant;
        this.confidence = builder.confidence;
    }

    public static GrowthMemory create(String content, GrowthPattern pattern,
            GrowthDirection direction, String significance) {
        return new Builder()
                .memoryId("growth-memory-" + System.currentTimeMillis())
                .content(content)
                .occurredAt(Instant.now())
                .pattern(pattern)
                .direction(direction)
                .significance(significance)
                .stillRelevant(true)
                .confidence(0.8f)
                .build();
    }

    @Override
    public String getMemoryId() { return memoryId; }

    @Override
    public String getContent() { return content; }

    @Override
    public Instant getOccurredAt() { return occurredAt; }

    public GrowthPattern getPattern() { return pattern; }

    public GrowthDirection getDirection() { return direction; }

    @Override
    public String getSignificance() { return significance; }

    @Override
    public boolean isStillRelevant() { return stillRelevant; }

    public float getConfidence() { return confidence; }

    @Override
    public LifeMemory withRelevance(boolean relevant) {
        return new Builder()
                .memoryId(this.memoryId)
                .content(this.content)
                .occurredAt(this.occurredAt)
                .pattern(this.pattern)
                .direction(this.direction)
                .significance(this.significance)
                .stillRelevant(relevant)
                .confidence(this.confidence)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GrowthMemory that = (GrowthMemory) o;
        return Objects.equals(memoryId, that.memoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memoryId);
    }

    @Override
    public String toString() {
        return String.format(
                "GrowthMemory{id=%s, pattern=%s, direction=%s, content='%s', confidence=%.2f}",
                memoryId, pattern, direction, content, confidence
        );
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String memoryId = "";
        private String content = "";
        private Instant occurredAt = Instant.now();
        private GrowthPattern pattern = GrowthPattern.INSIGHT_GAINED;
        private GrowthDirection direction = GrowthDirection.POSITIVE;
        private String significance = "";
        private boolean stillRelevant = true;
        private float confidence = 0.8f;

        public Builder memoryId(String memoryId) { this.memoryId = memoryId; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder occurredAt(Instant occurredAt) { this.occurredAt = occurredAt; return this; }
        public Builder pattern(GrowthPattern pattern) { this.pattern = pattern; return this; }
        public Builder direction(GrowthDirection direction) { this.direction = direction; return this; }
        public Builder significance(String significance) { this.significance = significance; return this; }
        public Builder stillRelevant(boolean stillRelevant) { this.stillRelevant = stillRelevant; return this; }
        public Builder confidence(float confidence) { this.confidence = confidence; return this; }

        public GrowthMemory build() { return new GrowthMemory(this); }
    }
}
