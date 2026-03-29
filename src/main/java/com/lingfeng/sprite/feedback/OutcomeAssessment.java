package com.lingfeng.sprite.feedback;

import java.time.Instant;
import java.util.UUID;

/**
 * OutcomeAssessment - 结果评估
 */
public final class OutcomeAssessment {

    public enum AssessmentType {
        IMMEDIATE,   // 即时评估
        DELAYED       // 延迟评估
    }

    public enum OutcomeResult {
        SUCCESS,              // 成功
        PARTIAL_SUCCESS,      // 部分成功
        FAILURE,              // 失败
        NEGATIVE_SIDEEFFECT   // 负面副作用
    }

    private final String assessmentId;
    private final String actionId;
    private final AssessmentType type;
    private final OutcomeResult result;
    private final String outcome;
    private final float rewardSignal;
    private final String lesson;
    private final Instant assessedAt;

    private OutcomeAssessment(Builder builder) {
        this.assessmentId = builder.assessmentId;
        this.actionId = builder.actionId;
        this.type = builder.type;
        this.result = builder.result;
        this.outcome = builder.outcome;
        this.rewardSignal = builder.rewardSignal;
        this.lesson = builder.lesson;
        this.assessedAt = builder.assessedAt;
    }

    public String assessmentId() {
        return assessmentId;
    }

    public String actionId() {
        return actionId;
    }

    public AssessmentType type() {
        return type;
    }

    public OutcomeResult result() {
        return result;
    }

    public String outcome() {
        return outcome;
    }

    public float rewardSignal() {
        return rewardSignal;
    }

    public String lesson() {
        return lesson;
    }

    public Instant assessedAt() {
        return assessedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(OutcomeAssessment other) {
        return new Builder()
                .assessmentId(other.assessmentId)
                .actionId(other.actionId)
                .type(other.type)
                .result(other.result)
                .outcome(other.outcome)
                .rewardSignal(other.rewardSignal)
                .lesson(other.lesson)
                .assessedAt(other.assessedAt);
    }

    public static class Builder {
        private String assessmentId = UUID.randomUUID().toString();
        private String actionId;
        private AssessmentType type;
        private OutcomeResult result;
        private String outcome;
        private float rewardSignal;
        private String lesson;
        private Instant assessedAt = Instant.now();

        public Builder assessmentId(String assessmentId) {
            this.assessmentId = assessmentId;
            return this;
        }

        public Builder actionId(String actionId) {
            this.actionId = actionId;
            return this;
        }

        public Builder type(AssessmentType type) {
            this.type = type;
            return this;
        }

        public Builder result(OutcomeResult result) {
            this.result = result;
            return this;
        }

        public Builder outcome(String outcome) {
            this.outcome = outcome;
            return this;
        }

        public Builder rewardSignal(float rewardSignal) {
            this.rewardSignal = rewardSignal;
            return this;
        }

        public Builder lesson(String lesson) {
            this.lesson = lesson;
            return this;
        }

        public Builder assessedAt(Instant assessedAt) {
            this.assessedAt = assessedAt;
            return this;
        }

        public OutcomeAssessment build() {
            return new OutcomeAssessment(this);
        }
    }
}
