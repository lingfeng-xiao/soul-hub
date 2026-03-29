package com.lingfeng.sprite.cognition;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * ReasoningFrame - 认知输入/输出结构化记录
 *
 * CG-001: 认知可解释性 - ReasoningFrame
 *
 * 代表完整认知周期的输入/输出结构：
 * - frameId: 唯一帧ID
 * - cycleId: 认知周期ID
 * - triggerType: 触发类型
 * - stimuli: 刺激列表
 * - context: 上下文
 * - recalledMemories: 召回的记忆
 * - goal: 目标
 * - riskProfile: 风险画像
 */
public record ReasoningFrame(
    String frameId,
    int cycleId,
    TriggerType triggerType,
    List<Stimulus> stimuli,
    Map<String, Object> context,
    List<MemoryRetrievalService.RecalledMemory> recalledMemories,
    String goal,
    RiskProfile riskProfile
) {
    /**
     * 触发类型枚举
     */
    public enum TriggerType {
        PERCEPTION,    // 感知触发
        MEMORY,        // 记忆触发
        EMOTION,       // 情绪触发
        SCHEDULED,     // 定时触发
        EXTERNAL       // 外部触发
    }

    /**
     * 刺激结构
     */
    public record Stimulus(
        String stimulusId,
        String type,
        String content,
        float salience,
        Instant timestamp
    ) {}

    /**
     * 风险画像
     */
    public record RiskProfile(
        float overallRisk,
        List<RiskFactor> factors,
        String mitigation
    ) {
        public record RiskFactor(
            String factor,
            float probability,
            float impact
        ) {}
    }

    /**
     * 创建空帧
     */
    public static ReasoningFrame empty(String frameId, int cycleId) {
        return new ReasoningFrame(
            frameId,
            cycleId,
            TriggerType.PERCEPTION,
            List.of(),
            Map.of(),
            List.of(),
            null,
            null
        );
    }

    /**
     * 检查帧是否有效
     */
    public boolean isValid() {
        return frameId != null && !frameId.isBlank();
    }
}
