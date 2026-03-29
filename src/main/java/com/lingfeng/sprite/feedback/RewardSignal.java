package com.lingfeng.sprite.feedback;

import java.time.Instant;
import java.util.UUID;

/**
 * RewardSignal - 奖励信号归一化
 */
public final class RewardSignal {

    public enum SignalType {
        POSITIVE,  // 正向奖励
        NEGATIVE,  // 负向奖励
        NEUTRAL    // 中性
    }

    private final String signalId;
    private final String feedbackId;
    private final SignalType type;
    private final float value;  // -1.0 到 1.0
    private final String reason;
    private final Instant createdAt;

    private RewardSignal(Builder builder) {
        this.signalId = builder.signalId;
        this.feedbackId = builder.feedbackId;
        this.type = builder.type;
        this.value = builder.value;
        this.reason = builder.reason;
        this.createdAt = builder.createdAt;
    }

    public String signalId() {
        return signalId;
    }

    public String feedbackId() {
        return feedbackId;
    }

    public SignalType type() {
        return type;
    }

    public float value() {
        return value;
    }

    public String reason() {
        return reason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(RewardSignal other) {
        return new Builder()
                .signalId(other.signalId)
                .feedbackId(other.feedbackId)
                .type(other.type)
                .value(other.value)
                .reason(other.reason)
                .createdAt(other.createdAt);
    }

    public static class Builder {
        private String signalId = UUID.randomUUID().toString();
        private String feedbackId;
        private SignalType type;
        private float value;
        private String reason;
        private Instant createdAt = Instant.now();

        public Builder signalId(String signalId) {
            this.signalId = signalId;
            return this;
        }

        public Builder feedbackId(String feedbackId) {
            this.feedbackId = feedbackId;
            return this;
        }

        public Builder type(SignalType type) {
            this.type = type;
            return this;
        }

        public Builder value(float value) {
            this.value = value;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RewardSignal build() {
            return new RewardSignal(this);
        }
    }
}
