package com.lingfeng.sprite.cognition;

/**
 * CandidateDecision - 候选决策
 *
 * CG-001: 认知可解释性 - CandidateDecision
 *
 * 表示一个可能的决策选项：
 * - decisionId: 决策ID
 * - intent: 关联的意图
 * - action: 建议的动作
 * - confidence: 置信度 (0.0 - 1.0)
 * - reasoning: 推理过程
 */
public record CandidateDecision(
    String decisionId,
    CandidateIntent intent,
    String action,
    float confidence,
    String reasoning
) {
    /**
     * 创建有效候选决策
     */
    public static CandidateDecision of(
        String decisionId,
        CandidateIntent intent,
        String action,
        float confidence,
        String reasoning
    ) {
        if (decisionId == null || intent == null || action == null) {
            throw new IllegalArgumentException("decisionId, intent, and action cannot be null");
        }
        return new CandidateDecision(decisionId, intent, action, confidence, reasoning);
    }

    /**
     * 检查是否达到行动阈值
     */
    public boolean isActionable(float threshold) {
        return confidence >= threshold && intent != null && intent.confidence() >= threshold;
    }

    /**
     * 获取决策优先级描述
     */
    public String priorityLevel() {
        if (confidence >= 0.8f) return "HIGH";
        if (confidence >= 0.5f) return "MEDIUM";
        return "LOW";
    }
}
