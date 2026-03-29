package com.lingfeng.sprite.evolution;

import java.time.Instant;
import java.util.Objects;

/**
 * ApprovalStage - 审批阶段
 */
public final class ApprovalStage {

    private final int stageNumber;
    private final ApprovalFlow.ApprovalType type;
    private final String approver;
    private final String decision;
    private final String comment;
    private final Instant decidedAt;

    private ApprovalStage(Builder builder) {
        this.stageNumber = builder.stageNumber;
        this.type = builder.type;
        this.approver = builder.approver;
        this.decision = builder.decision;
        this.comment = builder.comment;
        this.decidedAt = builder.decidedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getStageNumber() {
        return stageNumber;
    }

    public ApprovalFlow.ApprovalType getType() {
        return type;
    }

    public String getApprover() {
        return approver;
    }

    public String getDecision() {
        return decision;
    }

    public String getComment() {
        return comment;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public Builder with() {
        return new Builder()
                .stageNumber(this.stageNumber)
                .type(this.type)
                .approver(this.approver)
                .decision(this.decision)
                .comment(this.comment)
                .decidedAt(this.decidedAt);
    }

    /**
     * Builder for ApprovalStage
     */
    public static final class Builder {
        private int stageNumber;
        private ApprovalFlow.ApprovalType type;
        private String approver;
        private String decision;
        private String comment;
        private Instant decidedAt;

        private Builder() {}

        public Builder stageNumber(int stageNumber) {
            this.stageNumber = stageNumber;
            return this;
        }

        public Builder type(ApprovalFlow.ApprovalType type) {
            this.type = type;
            return this;
        }

        public Builder approver(String approver) {
            this.approver = approver;
            return this;
        }

        public Builder decision(String decision) {
            this.decision = decision;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder decidedAt(Instant decidedAt) {
            this.decidedAt = decidedAt;
            return this;
        }

        public ApprovalStage build() {
            return new ApprovalStage(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalStage that = (ApprovalStage) o;
        return stageNumber == that.stageNumber &&
                Objects.equals(type, that.type) &&
                Objects.equals(approver, that.approver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stageNumber, type, approver);
    }

    @Override
    public String toString() {
        return "ApprovalStage{" +
                "stageNumber=" + stageNumber +
                ", type=" + type +
                ", approver='" + approver + '\'' +
                ", decision='" + decision + '\'' +
                ", decidedAt=" + decidedAt +
                '}';
    }
}
