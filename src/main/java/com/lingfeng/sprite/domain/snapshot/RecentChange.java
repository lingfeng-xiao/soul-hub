package com.lingfeng.sprite.domain.snapshot;

import java.time.Instant;

/**
 * RecentChange - 最近变化记录
 *
 * 记录 Sprite 最近的变化，用于 LifeSnapshot 中的最近变化展示。
 */
public final class RecentChange {

    /**
     * 变化类型
     */
    public enum ChangeType {
        IDENTITY,      // 身份变化
        SELF_STATE,    // 自我状态变化
        RELATIONSHIP,  // 关系变化
        GOAL,          // 目标变化
        BEHAVIOR,      // 行为模式变化
        CAPABILITY,    // 能力变化
        PREFERENCE     // 偏好变化
    }

    private final String changeId;
    private final ChangeType type;
    private final String description;
    private final String previousState;
    private final String newState;
    private final Instant occurredAt;
    private final String trigger;
    private final String significance;

    private RecentChange(Builder builder) {
        this.changeId = builder.changeId;
        this.type = builder.type;
        this.description = builder.description;
        this.previousState = builder.previousState;
        this.newState = builder.newState;
        this.occurredAt = builder.occurredAt;
        this.trigger = builder.trigger;
        this.significance = builder.significance;
    }

    public static RecentChange create(ChangeType type, String description, String previousState, String newState, String trigger) {
        return new RecentChange.Builder()
                .changeId("change-" + System.currentTimeMillis())
                .type(type)
                .description(description)
                .previousState(previousState)
                .newState(newState)
                .occurredAt(Instant.now())
                .trigger(trigger)
                .significance("MEDIUM")
                .build();
    }

    // Getters
    public String getChangeId() { return changeId; }
    public ChangeType getType() { return type; }
    public String getDescription() { return description; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getTrigger() { return trigger; }
    public String getSignificance() { return significance; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String changeId = "";
        private ChangeType type = ChangeType.SELF_STATE;
        private String description = "";
        private String previousState = "";
        private String newState = "";
        private Instant occurredAt = Instant.now();
        private String trigger = "";
        private String significance = "MEDIUM";

        public Builder changeId(String changeId) { this.changeId = changeId; return this; }
        public Builder type(ChangeType type) { this.type = type; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder previousState(String previousState) { this.previousState = previousState; return this; }
        public Builder newState(String newState) { this.newState = newState; return this; }
        public Builder occurredAt(Instant occurredAt) { this.occurredAt = occurredAt; return this; }
        public Builder trigger(String trigger) { this.trigger = trigger; return this; }
        public Builder significance(String significance) { this.significance = significance; return this; }

        public RecentChange build() { return new RecentChange(this); }
    }
}
