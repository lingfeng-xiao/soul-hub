package com.lingfeng.sprite.feedback;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * FeedbackEvent - 反馈事件
 */
public final class FeedbackEvent {

    public enum FeedbackType {
        USER_EXPLICIT,     // 用户明确反馈
        SYSTEM_ASSESSMENT,  // 系统评估反馈
        ENVIRONMENT_RESULT  // 环境结果反馈
    }

    public enum FeedbackSource {
        USER,        // 用户
        OWNER,       // 主人
        SYSTEM,      // 系统
        ENVIRONMENT  // 环境
    }

    private final String eventId;
    private final FeedbackType type;
    private final FeedbackSource source;
    private final String content;
    private final String targetCycleId;
    private final String targetActionId;
    private final EmotionalContext emotionalContext;
    private final Instant timestamp;
    private final Map<String, Object> metadata;

    private FeedbackEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.type = builder.type;
        this.source = builder.source;
        this.content = builder.content;
        this.targetCycleId = builder.targetCycleId;
        this.targetActionId = builder.targetActionId;
        this.emotionalContext = builder.emotionalContext;
        this.timestamp = builder.timestamp;
        this.metadata = builder.metadata;
    }

    public String eventId() {
        return eventId;
    }

    public FeedbackType type() {
        return type;
    }

    public FeedbackSource source() {
        return source;
    }

    public String content() {
        return content;
    }

    public String targetCycleId() {
        return targetCycleId;
    }

    public String targetActionId() {
        return targetActionId;
    }

    public EmotionalContext emotionalContext() {
        return emotionalContext;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(FeedbackEvent other) {
        return new Builder()
                .eventId(other.eventId)
                .type(other.type)
                .source(other.source)
                .content(other.content)
                .targetCycleId(other.targetCycleId)
                .targetActionId(other.targetActionId)
                .emotionalContext(other.emotionalContext)
                .timestamp(other.timestamp)
                .metadata(other.metadata);
    }

    public static class Builder {
        private String eventId = UUID.randomUUID().toString();
        private FeedbackType type;
        private FeedbackSource source;
        private String content;
        private String targetCycleId;
        private String targetActionId;
        private EmotionalContext emotionalContext;
        private Instant timestamp = Instant.now();
        private Map<String, Object> metadata;

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder type(FeedbackType type) {
            this.type = type;
            return this;
        }

        public Builder source(FeedbackSource source) {
            this.source = source;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder targetCycleId(String targetCycleId) {
            this.targetCycleId = targetCycleId;
            return this;
        }

        public Builder targetActionId(String targetActionId) {
            this.targetActionId = targetActionId;
            return this;
        }

        public Builder emotionalContext(EmotionalContext emotionalContext) {
            this.emotionalContext = emotionalContext;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public FeedbackEvent build() {
            return new FeedbackEvent(this);
        }
    }
}
