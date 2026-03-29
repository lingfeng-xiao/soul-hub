package com.lingfeng.sprite.evolution;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * EvolutionProposal - 进化提案
 * 代表一个变更提案的完整生命周期
 */
public final class EvolutionProposal {

    public enum ProposalType {
        RETRIEVAL_WEIGHT_ADJUST,   // 检索权重调整
        PROMPT_TEMPLATE_OPTIMIZE,  // 提示模板优化
        DECISION_RANKING_TWEAK,    // 决策排序微调
        MEMORY_SUMMARY_RULE,       // 记忆摘要规则
        FEEDBACK_MAPPING_TWEAK,    // 反馈映射规则
        RULE_UPDATE,              // 规则更新
        CONFIG_CHANGE              // 配置变更
    }

    public enum ProposalStatus {
        DRAFT,              // 草稿
        PENDING_APPROVAL,   // 待审批
        APPROVED,           // 已批准
        REJECTED,           // 已拒绝
        RELEASED,           // 已发布
        ROLLED_BACK         // 已回滚
    }

    public enum RiskLevel {
        LOW,    // 低风险 - 可自动批准
        MEDIUM, // 中风险 - 需要人工审批
        HIGH    // 高风险 - 需要多级审批
    }

    private final String proposalId;
    private final ProposalType type;
    private final String title;
    private final String description;
    private final String targetScope;
    private final String motivation;
    private final String evidenceSummary;
    private final String expectedBenefit;
    private final String potentialRisk;
    private final String patchContent;
    private final Map<String, Object> baselineMetrics;
    private final Map<String, Object> targetMetrics;
    private final ProposalStatus status;
    private final RiskLevel riskLevel;
    private final ApprovalInfo approvalInfo;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String createdBy;

    private EvolutionProposal(Builder builder) {
        this.proposalId = builder.proposalId;
        this.type = builder.type;
        this.title = builder.title;
        this.description = builder.description;
        this.targetScope = builder.targetScope;
        this.motivation = builder.motivation;
        this.evidenceSummary = builder.evidenceSummary;
        this.expectedBenefit = builder.expectedBenefit;
        this.potentialRisk = builder.potentialRisk;
        this.patchContent = builder.patchContent;
        this.baselineMetrics = builder.baselineMetrics;
        this.targetMetrics = builder.targetMetrics;
        this.status = builder.status;
        this.riskLevel = builder.riskLevel;
        this.approvalInfo = builder.approvalInfo;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
    }

    // Static factory method
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getProposalId() {
        return proposalId;
    }

    public ProposalType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTargetScope() {
        return targetScope;
    }

    public String getMotivation() {
        return motivation;
    }

    public String getEvidenceSummary() {
        return evidenceSummary;
    }

    public String getExpectedBenefit() {
        return expectedBenefit;
    }

    public String getPotentialRisk() {
        return potentialRisk;
    }

    public String getPatchContent() {
        return patchContent;
    }

    public Map<String, Object> getBaselineMetrics() {
        return baselineMetrics;
    }

    public Map<String, Object> getTargetMetrics() {
        return targetMetrics;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public ApprovalInfo getApprovalInfo() {
        return approvalInfo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    // With methods for immutable updates
    public Builder with() {
        return new Builder()
                .proposalId(this.proposalId)
                .type(this.type)
                .title(this.title)
                .description(this.description)
                .targetScope(this.targetScope)
                .motivation(this.motivation)
                .evidenceSummary(this.evidenceSummary)
                .expectedBenefit(this.expectedBenefit)
                .potentialRisk(this.potentialRisk)
                .patchContent(this.patchContent)
                .baselineMetrics(this.baselineMetrics)
                .targetMetrics(this.targetMetrics)
                .status(this.status)
                .riskLevel(this.riskLevel)
                .approvalInfo(this.approvalInfo)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .createdBy(this.createdBy);
    }

    /**
     * ApprovalInfo - 审批信息
     */
    public record ApprovalInfo(
            String approvalFlowId,
            Instant submittedAt,
            Instant approvedAt,
            String approvedBy
    ) {
        public static ApprovalInfo of(String approvalFlowId) {
            return new ApprovalInfo(approvalFlowId, null, null, null);
        }

        public ApprovalInfo withSubmittedAt(Instant submittedAt) {
            return new ApprovalInfo(approvalFlowId, submittedAt, approvedAt, approvedBy);
        }

        public ApprovalInfo withApprovedAt(Instant approvedAt, String approvedBy) {
            return new ApprovalInfo(approvalFlowId, submittedAt, approvedAt, approvedBy);
        }
    }

    /**
     * Builder for EvolutionProposal
     */
    public static final class Builder {
        private String proposalId;
        private ProposalType type;
        private String title;
        private String description;
        private String targetScope;
        private String motivation;
        private String evidenceSummary;
        private String expectedBenefit;
        private String potentialRisk;
        private String patchContent;
        private Map<String, Object> baselineMetrics;
        private Map<String, Object> targetMetrics;
        private ProposalStatus status;
        private RiskLevel riskLevel;
        private ApprovalInfo approvalInfo;
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;

        private Builder() {}

        public Builder proposalId(String proposalId) {
            this.proposalId = proposalId;
            return this;
        }

        public Builder type(ProposalType type) {
            this.type = type;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder targetScope(String targetScope) {
            this.targetScope = targetScope;
            return this;
        }

        public Builder motivation(String motivation) {
            this.motivation = motivation;
            return this;
        }

        public Builder evidenceSummary(String evidenceSummary) {
            this.evidenceSummary = evidenceSummary;
            return this;
        }

        public Builder expectedBenefit(String expectedBenefit) {
            this.expectedBenefit = expectedBenefit;
            return this;
        }

        public Builder potentialRisk(String potentialRisk) {
            this.potentialRisk = potentialRisk;
            return this;
        }

        public Builder patchContent(String patchContent) {
            this.patchContent = patchContent;
            return this;
        }

        public Builder baselineMetrics(Map<String, Object> baselineMetrics) {
            this.baselineMetrics = baselineMetrics;
            return this;
        }

        public Builder targetMetrics(Map<String, Object> targetMetrics) {
            this.targetMetrics = targetMetrics;
            return this;
        }

        public Builder status(ProposalStatus status) {
            this.status = status;
            return this;
        }

        public Builder riskLevel(RiskLevel riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public Builder approvalInfo(ApprovalInfo approvalInfo) {
            this.approvalInfo = approvalInfo;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public EvolutionProposal build() {
            return new EvolutionProposal(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvolutionProposal that = (EvolutionProposal) o;
        return Objects.equals(proposalId, that.proposalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proposalId);
    }

    @Override
    public String toString() {
        return "EvolutionProposal{" +
                "proposalId='" + proposalId + '\'' +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", riskLevel=" + riskLevel +
                '}';
    }
}
