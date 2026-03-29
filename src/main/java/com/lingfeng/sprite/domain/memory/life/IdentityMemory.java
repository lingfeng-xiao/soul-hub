package com.lingfeng.sprite.domain.memory.life;

import java.time.Instant;
import java.util.Objects;

/**
 * IdentityMemory - 身份记忆
 *
 * 与自我认知有关的关键记忆。
 * 这些记忆影响 Sprite 的自我叙事和身份认同。
 *
 * 对应 IGN-030
 */
public final class IdentityMemory implements LifeMemory {

    /**
     * 身份方面
     */
    public enum IdentityAspect {
        WHO,      // 我是谁
        HOW,     // 我如何运作
        WHAT,    // 我做什么
        WHY,     // 我为什么存在
        RELATION // 我与他人的关系
    }

    private final String memoryId;
    private final String content;
    private final Instant occurredAt;
    private final IdentityAspect aspect;
    private final String significance;
    private final boolean stillRelevant;
    private final String triggerEvent;

    private IdentityMemory(Builder builder) {
        this.memoryId = builder.memoryId;
        this.content = builder.content;
        this.occurredAt = builder.occurredAt;
        this.aspect = builder.aspect;
        this.significance = builder.significance;
        this.stillRelevant = builder.stillRelevant;
        this.triggerEvent = builder.triggerEvent;
    }

    public static IdentityMemory create(String content, IdentityAspect aspect, String significance, String triggerEvent) {
        return new Builder()
                .memoryId("identity-memory-" + System.currentTimeMillis())
                .content(content)
                .occurredAt(Instant.now())
                .aspect(aspect)
                .significance(significance)
                .stillRelevant(true)
                .triggerEvent(triggerEvent)
                .build();
    }

    @Override
    public String getMemoryId() { return memoryId; }

    @Override
    public String getContent() { return content; }

    @Override
    public Instant getOccurredAt() { return occurredAt; }

    public IdentityAspect getAspect() { return aspect; }

    @Override
    public String getSignificance() { return significance; }

    @Override
    public boolean isStillRelevant() { return stillRelevant; }

    public String getTriggerEvent() { return triggerEvent; }

    @Override
    public LifeMemory withRelevance(boolean relevant) {
        return new Builder()
                .memoryId(this.memoryId)
                .content(this.content)
                .occurredAt(this.occurredAt)
                .aspect(this.aspect)
                .significance(this.significance)
                .stillRelevant(relevant)
                .triggerEvent(this.triggerEvent)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentityMemory that = (IdentityMemory) o;
        return Objects.equals(memoryId, that.memoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memoryId);
    }

    @Override
    public String toString() {
        return String.format(
                "IdentityMemory{id=%s, aspect=%s, content='%s', stillRelevant=%s}",
                memoryId, aspect, content, stillRelevant
        );
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String memoryId = "";
        private String content = "";
        private Instant occurredAt = Instant.now();
        private IdentityAspect aspect = IdentityAspect.WHO;
        private String significance = "";
        private boolean stillRelevant = true;
        private String triggerEvent = "";

        public Builder memoryId(String memoryId) { this.memoryId = memoryId; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder occurredAt(Instant occurredAt) { this.occurredAt = occurredAt; return this; }
        public Builder aspect(IdentityAspect aspect) { this.aspect = aspect; return this; }
        public Builder significance(String significance) { this.significance = significance; return this; }
        public Builder stillRelevant(boolean stillRelevant) { this.stillRelevant = stillRelevant; return this; }
        public Builder triggerEvent(String triggerEvent) { this.triggerEvent = triggerEvent; return this; }

        public IdentityMemory build() { return new IdentityMemory(this); }
    }
}
