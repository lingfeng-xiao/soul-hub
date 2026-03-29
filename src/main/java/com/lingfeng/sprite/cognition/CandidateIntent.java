package com.lingfeng.sprite.cognition;

/**
 * CandidateIntent - 候选意图
 *
 * CG-001: 认知可解释性 - CandidateIntent
 *
 * 表示一个可能的用户意图：
 * - intentId: 意图ID
 * - description: 意图描述
 * - confidence: 置信度 (0.0 - 1.0)
 * - source: 来源 (REASONING, MEMORY, PATTERN, EMOTION)
 */
public record CandidateIntent(
    String intentId,
    String description,
    float confidence,
    Source source
) {
    /**
     * 意图来源
     */
    public enum Source {
        REASONING,   // 推理引擎
        MEMORY,      // 记忆检索
        PATTERN,     // 行为模式
        EMOTION,     // 情绪推断
        HEURISTIC    // 启发式
    }

    /**
     * 创建高置信度意图
     */
    public static CandidateIntent highConfidence(String id, String description, Source source) {
        return new CandidateIntent(id, description, 0.8f, source);
    }

    /**
     * 创建低置信度意图
     */
    public static CandidateIntent lowConfidence(String id, String description, Source source) {
        return new CandidateIntent(id, description, 0.3f, source);
    }

    /**
     * 检查是否达到阈值
     */
    public boolean meetsThreshold(float threshold) {
        return confidence >= threshold;
    }
}
