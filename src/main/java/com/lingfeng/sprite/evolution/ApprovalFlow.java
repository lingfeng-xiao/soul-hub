package com.lingfeng.sprite.evolution;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * ApprovalFlow - 审批流程
 */
public final class ApprovalFlow {

    public enum ApprovalStatus {
        PENDING,     // 待处理
        IN_PROGRESS, // 进行中
        APPROVED,    // 已批准
        REJECTED     // 已拒绝
    }

    public enum ApprovalType {
        AUTO_CHECK,   // 系统自动检查
        HUMAN_REVIEW  // 人工审批
    }

    private final String flowId;
    private final String proposalId;
    private final List<ApprovalStage> stages;
    private final ApprovalStatus status;
    private final Instant startedAt;
    private final Instant completedAt;

    private ApprovalFlow(Builder builder) {
        this.flowId = builder.flowId;
        this.proposalId = builder.proposalId;
        this.stages = builder.stages;
        this.status = builder.status;
        this.startedAt = builder.startedAt;
        this.completedAt = builder.completedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getFlowId() {
        return flowId;
    }

    public String getProposalId() {
        return proposalId;
    }

    public List<ApprovalStage> getStages() {
        return stages;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Builder with() {
        return new Builder()
                .flowId(this.flowId)
                .proposalId(this.proposalId)
                .stages(this.stages)
                .status(this.status)
                .startedAt(this.startedAt)
                .completedAt(this.completedAt);
    }

    /**
     * Builder for ApprovalFlow
     */
    public static final class Builder {
        private String flowId;
        private String proposalId;
        private List<ApprovalStage> stages;
        private ApprovalStatus status;
        private Instant startedAt;
        private Instant completedAt;

        private Builder() {}

        public Builder flowId(String flowId) {
            this.flowId = flowId;
            return this;
        }

        public Builder proposalId(String proposalId) {
            this.proposalId = proposalId;
            return this;
        }

        public Builder stages(List<ApprovalStage> stages) {
            this.stages = stages;
            return this;
        }

        public Builder status(ApprovalStatus status) {
            this.status = status;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public ApprovalFlow build() {
            return new ApprovalFlow(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalFlow that = (ApprovalFlow) o;
        return Objects.equals(flowId, that.flowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowId);
    }

    @Override
    public String toString() {
        return "ApprovalFlow{" +
                "flowId='" + flowId + '\'' +
                ", proposalId='" + proposalId + '\'' +
                ", status=" + status +
                ", stageCount=" + (stages != null ? stages.size() : 0) +
                '}';
    }
}
