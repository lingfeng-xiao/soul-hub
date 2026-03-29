package com.lingfeng.sprite.domain.memory.life;

import java.time.Instant;
import java.util.Objects;

/**
 * RelationshipMemory - 关系记忆
 *
 * 与主人关系塑造有关的关键记忆。
 * 这些记忆影响 Sprite 如何与主人互动和建立关系。
 *
 * 对应 IGN-030
 */
public final class RelationshipMemory implements LifeMemory {

    /**
     * 关系事件类型
     */
    public enum RelationshipEventType {
        FIRST_CONTACT,        // 首次接触
        SHARED_SUCCESS,        // 共同成功
        SHARED_FAILURE,       // 共同失败
        DEEP_CONVERSATION,    // 深入对话
        TRUST_BUILDING,        // 信任建立
        TRUST_BETRAYAL,       // 信任破裂
        GROWTH_MOMENT,        // 共同成长时刻
        CONFLICT,             // 冲突
        RESOLUTION,           // 解决
        LEARNING_SHARED,      // 共同学习
        GOAL_ACHIEVED         // 目标达成
    }

    private final String memoryId;
    private final String content;
    private final Instant occurredAt;
    private final RelationshipEventType eventType;
    private final String significance;
    private final boolean stillRelevant;
    private final float emotionalImpact;  // -1 to 1

    private RelationshipMemory(Builder builder) {
        this.memoryId = builder.memoryId;
        this.content = builder.content;
        this.occurredAt = builder.occurredAt;
        this.eventType = builder.eventType;
        this.significance = builder.significance;
        this.stillRelevant = builder.stillRelevant;
        this.emotionalImpact = builder.emotionalImpact;
    }

    public static RelationshipMemory create(String content, RelationshipEventType eventType,
            String significance, float emotionalImpact) {
        return new Builder()
                .memoryId("relationship-memory-" + System.currentTimeMillis())
                .content(content)
                .occurredAt(Instant.now())
                .eventType(eventType)
                .significance(significance)
                .stillRelevant(true)
                .emotionalImpact(emotionalImpact)
                .build();
    }

    @Override
    public String getMemoryId() { return memoryId; }

    @Override
    public String getContent() { return content; }

    @Override
    public Instant getOccurredAt() { return occurredAt; }

    public RelationshipEventType getEventType() { return eventType; }

    @Override
    public String getSignificance() { return significance; }

    @Override
    public boolean isStillRelevant() { return stillRelevant; }

    public float getEmotionalImpact() { return emotionalImpact; }

    @Override
    public LifeMemory withRelevance(boolean relevant) {
        return new Builder()
                .memoryId(this.memoryId)
                .content(this.content)
                .occurredAt(this.occurredAt)
                .eventType(this.eventType)
                .significance(this.significance)
                .stillRelevant(relevant)
                .emotionalImpact(this.emotionalImpact)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationshipMemory that = (RelationshipMemory) o;
        return Objects.equals(memoryId, that.memoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memoryId);
    }

    @Override
    public String toString() {
        return String.format(
                "RelationshipMemory{id=%s, eventType=%s, content='%s', emotionalImpact=%.2f}",
                memoryId, eventType, content, emotionalImpact
        );
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String memoryId = "";
        private String content = "";
        private Instant occurredAt = Instant.now();
        private RelationshipEventType eventType = RelationshipEventType.FIRST_CONTACT;
        private String significance = "";
        private boolean stillRelevant = true;
        private float emotionalImpact = 0f;

        public Builder memoryId(String memoryId) { this.memoryId = memoryId; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder occurredAt(Instant occurredAt) { this.occurredAt = occurredAt; return this; }
        public Builder eventType(RelationshipEventType eventType) { this.eventType = eventType; return this; }
        public Builder significance(String significance) { this.significance = significance; return this; }
        public Builder stillRelevant(boolean stillRelevant) { this.stillRelevant = stillRelevant; return this; }
        public Builder emotionalImpact(float emotionalImpact) { this.emotionalImpact = emotionalImpact; return this; }

        public RelationshipMemory build() { return new RelationshipMemory(this); }
    }
}
