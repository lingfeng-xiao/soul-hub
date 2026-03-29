package com.lingfeng.sprite.domain.self;

import java.time.Instant;
import java.util.Objects;

/**
 * AttentionFocus - 当前注意力焦点
 *
 * 代表 Sprite 当前正在关注的对象或事项。
 * 这个焦点会在不同的时间粒度和认知活动间切换。
 *
 * 对应旧: SelfModel 中的焦点的显式建模
 */
public final class AttentionFocus {

    /**
     * 焦点类型
     */
    public enum FocusType {
        TASK,         // 正在执行任务
        CONVERSATION, // 正在对话
        OBSERVATION,  // 正在观察
        REFLECTION,   // 正在反思
        LEARNING,     // 正在学习
        IDLE          // 空闲状态
    }

    /**
     * 焦点类型
     */
    private final FocusType type;

    /**
     * 焦点描述
     */
    private final String description;

    /**
     * 关联的实体 ID (如对话 ID, 任务 ID)
     */
    private final String relatedEntityId;

    /**
     * 焦点强度 (0-1)
     */
    private final float intensity;

    /**
     * 开始时间
     */
    private final Instant startedAt;

    /**
     * 预期持续时间 (毫秒)
     */
    private final long expectedDurationMs;

    private AttentionFocus(Builder builder) {
        this.type = builder.type;
        this.description = builder.description;
        this.relatedEntityId = builder.relatedEntityId;
        this.intensity = builder.intensity;
        this.startedAt = builder.startedAt;
        this.expectedDurationMs = builder.expectedDurationMs;
    }

    /**
     * 创建空闲焦点
     */
    public static AttentionFocus idle() {
        return new AttentionFocus.Builder()
                .type(FocusType.IDLE)
                .description("当前没有特定关注事项")
                .intensity(0.0f)
                .startedAt(Instant.now())
                .expectedDurationMs(0)
                .build();
    }

    /**
     * 创建任务焦点
     */
    public static AttentionFocus task(String taskId, String description, long expectedDurationMs) {
        return new AttentionFocus.Builder()
                .type(FocusType.TASK)
                .description(description)
                .relatedEntityId(taskId)
                .intensity(1.0f)
                .startedAt(Instant.now())
                .expectedDurationMs(expectedDurationMs)
                .build();
    }

    /**
     * 创建对话焦点
     */
    public static AttentionFocus conversation(String conversationId, String topic) {
        return new AttentionFocus.Builder()
                .type(FocusType.CONVERSATION)
                .description(topic)
                .relatedEntityId(conversationId)
                .intensity(0.9f)
                .startedAt(Instant.now())
                .expectedDurationMs(300000) // 默认 5 分钟
                .build();
    }

    /**
     * 检查焦点是否已超时
     */
    public boolean isTimedOut() {
        if (expectedDurationMs <= 0) {
            return false;
        }
        long elapsed = Instant.now().toEpochMilli() - startedAt.toEpochMilli();
        return elapsed > expectedDurationMs * 1.5; // 允许 50% 超时缓冲
    }

    /**
     * 获取焦点持续时间 (毫秒)
     */
    public long getDurationMs() {
        return Instant.now().toEpochMilli() - startedAt.toEpochMilli();
    }

    // Getters
    public FocusType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getRelatedEntityId() {
        return relatedEntityId;
    }

    public float getIntensity() {
        return intensity;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public long getExpectedDurationMs() {
        return expectedDurationMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttentionFocus that = (AttentionFocus) o;
        return type == that.type &&
                Float.compare(that.intensity, intensity) == 0 &&
                Objects.equals(description, that.description) &&
                Objects.equals(relatedEntityId, that.relatedEntityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, description, relatedEntityId, intensity);
    }

    @Override
    public String toString() {
        return "AttentionFocus{" +
                "type=" + type +
                ", description='" + description + '\'' +
                ", intensity=" + intensity +
                ", startedAt=" + startedAt +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .type(this.type)
                .description(this.description)
                .relatedEntityId(this.relatedEntityId)
                .intensity(this.intensity)
                .startedAt(this.startedAt)
                .expectedDurationMs(this.expectedDurationMs);
    }

    public static final class Builder {
        private FocusType type = FocusType.IDLE;
        private String description = "";
        private String relatedEntityId = "";
        private float intensity = 0.0f;
        private Instant startedAt = Instant.now();
        private long expectedDurationMs = 0;

        public Builder type(FocusType type) {
            this.type = type;
            return this;
        }

        public Builder description(String description) {
            this.description = description != null ? description : "";
            return this;
        }

        public Builder relatedEntityId(String relatedEntityId) {
            this.relatedEntityId = relatedEntityId != null ? relatedEntityId : "";
            return this;
        }

        public Builder intensity(float intensity) {
            this.intensity = Math.max(0f, Math.min(1f, intensity));
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder expectedDurationMs(long expectedDurationMs) {
            this.expectedDurationMs = expectedDurationMs;
            return this;
        }

        public AttentionFocus build() {
            return new AttentionFocus(this);
        }
    }
}
