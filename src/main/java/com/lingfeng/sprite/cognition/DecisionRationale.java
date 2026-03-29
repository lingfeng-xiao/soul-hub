package com.lingfeng.sprite.cognition;

import java.util.List;
import java.util.Map;

/**
 * DecisionRationale - 决策理由记录
 *
 * CG-001: 认知可解释性 - DecisionRationale
 *
 * 记录决策的完整理由：
 * - rationaleId: 理由ID
 * - frameId: 关联的推理帧ID
 * - selectedDecision: 最终选择的决策
 * - alternativeDecisions: 备选决策列表
 * - triggerSummary: 触发总结
 * - recalledMemories: 召回的记忆
 * - criticalRulesHit: 触发的关键规则
 * - rationaleText: 理由文本
 * - confidenceScore: 置信度评分
 * - expectedOutcome: 预期结果
 * - fallbackPlan: 备用计划
 */
public record DecisionRationale(
    String rationaleId,
    String frameId,
    CandidateDecision selectedDecision,
    List<CandidateDecision> alternativeDecisions,
    String triggerSummary,
    List<MemoryRetrievalService.RecalledMemory> recalledMemories,
    List<String> criticalRulesHit,
    String rationaleText,
    float confidenceScore,
    String expectedOutcome,
    String fallbackPlan
) {
    /**
     * 创建决策理由构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 决策理由构建器
     */
    public static class Builder {
        private String rationaleId;
        private String frameId;
        private CandidateDecision selectedDecision;
        private List<CandidateDecision> alternativeDecisions = List.of();
        private String triggerSummary = "";
        private List<MemoryRetrievalService.RecalledMemory> recalledMemories = List.of();
        private List<String> criticalRulesHit = List.of();
        private String rationaleText = "";
        private float confidenceScore = 0.0f;
        private String expectedOutcome = "";
        private String fallbackPlan = "";

        public Builder rationaleId(String rationaleId) {
            this.rationaleId = rationaleId;
            return this;
        }

        public Builder frameId(String frameId) {
            this.frameId = frameId;
            return this;
        }

        public Builder selectedDecision(CandidateDecision selectedDecision) {
            this.selectedDecision = selectedDecision;
            return this;
        }

        public Builder alternativeDecisions(List<CandidateDecision> alternativeDecisions) {
            this.alternativeDecisions = alternativeDecisions;
            return this;
        }

        public Builder triggerSummary(String triggerSummary) {
            this.triggerSummary = triggerSummary;
            return this;
        }

        public Builder recalledMemories(List<MemoryRetrievalService.RecalledMemory> recalledMemories) {
            this.recalledMemories = recalledMemories;
            return this;
        }

        public Builder criticalRulesHit(List<String> criticalRulesHit) {
            this.criticalRulesHit = criticalRulesHit;
            return this;
        }

        public Builder rationaleText(String rationaleText) {
            this.rationaleText = rationaleText;
            return this;
        }

        public Builder confidenceScore(float confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public Builder expectedOutcome(String expectedOutcome) {
            this.expectedOutcome = expectedOutcome;
            return this;
        }

        public Builder fallbackPlan(String fallbackPlan) {
            this.fallbackPlan = fallbackPlan;
            return this;
        }

        public DecisionRationale build() {
            if (rationaleId == null || frameId == null) {
                throw new IllegalStateException("rationaleId and frameId are required");
            }
            return new DecisionRationale(
                rationaleId,
                frameId,
                selectedDecision,
                alternativeDecisions,
                triggerSummary,
                recalledMemories,
                criticalRulesHit,
                rationaleText,
                confidenceScore,
                expectedOutcome,
                fallbackPlan
            );
        }
    }

    /**
     * 获取决策摘要
     */
    public String getDecisionSummary() {
        if (selectedDecision == null) {
            return "No decision selected";
        }
        return String.format("[%s] %s (confidence=%.2f)",
            selectedDecision.decisionId(),
            selectedDecision.action(),
            selectedDecision.confidence());
    }

    /**
     * 获取置信度等级
     */
    public String getConfidenceLevel() {
        if (confidenceScore >= 0.8f) return "HIGH";
        if (confidenceScore >= 0.5f) return "MEDIUM";
        return "LOW";
    }
}
