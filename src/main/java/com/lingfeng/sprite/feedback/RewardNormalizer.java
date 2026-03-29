package com.lingfeng.sprite.feedback;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

/**
 * RewardNormalizer - 奖励信号归一化
 */
public class RewardNormalizer {

    private final Random random = new Random();

    /**
     * 归一化规则:
     * - 用户明确表扬 → POSITIVE 0.8~1.0
     * - 用户明确批评 → NEGATIVE -0.8~-1.0
     * - 系统评估成功 → POSITIVE 0.5~0.7
     * - 系统评估失败 → NEGATIVE -0.5~-0.7
     * - 环境正面结果 → POSITIVE 0.3~0.5
     * - 环境负面结果 → NEGATIVE -0.3~-0.5
     */
    public RewardSignal normalize(FeedbackEvent event) {
        RewardSignal.SignalType type;
        float value;
        String reason;

        switch (event.type()) {
            case USER_EXPLICIT -> {
                // 根据情感上下文判断
                float valence = event.emotionalContext() != null
                        ? event.emotionalContext().valence()
                        : 0.0f;

                if (valence > 0.1) {
                    type = RewardSignal.SignalType.POSITIVE;
                    value = 0.8f + random.nextFloat() * 0.2f; // 0.8~1.0
                    reason = "用户明确表扬";
                } else if (valence < -0.1) {
                    type = RewardSignal.SignalType.NEGATIVE;
                    value = -0.8f - random.nextFloat() * 0.2f; // -0.8~-1.0
                    reason = "用户明确批评";
                } else {
                    type = RewardSignal.SignalType.NEUTRAL;
                    value = 0.0f;
                    reason = "用户中性反馈";
                }
            }
            case SYSTEM_ASSESSMENT -> {
                // 从元数据中获取评估结果
                Object assessmentResult = event.metadata() != null
                        ? event.metadata().get("assessmentResult")
                        : null;

                if ("success".equals(assessmentResult)) {
                    type = RewardSignal.SignalType.POSITIVE;
                    value = 0.5f + random.nextFloat() * 0.2f; // 0.5~0.7
                    reason = "系统评估成功";
                } else {
                    type = RewardSignal.SignalType.NEGATIVE;
                    value = -0.5f - random.nextFloat() * 0.2f; // -0.5~-0.7
                    reason = "系统评估失败";
                }
            }
            case ENVIRONMENT_RESULT -> {
                // 从元数据中获取环境结果
                Object envResult = event.metadata() != null
                        ? event.metadata().get("envResult")
                        : null;

                if ("positive".equals(envResult)) {
                    type = RewardSignal.SignalType.POSITIVE;
                    value = 0.3f + random.nextFloat() * 0.2f; // 0.3~0.5
                    reason = "环境正面结果";
                } else {
                    type = RewardSignal.SignalType.NEGATIVE;
                    value = -0.3f - random.nextFloat() * 0.2f; // -0.3~-0.5
                    reason = "环境负面结果";
                }
            }
            default -> {
                type = RewardSignal.SignalType.NEUTRAL;
                value = 0.0f;
                reason = "未知反馈类型";
            }
        }

        return RewardSignal.builder()
                .signalId(UUID.randomUUID().toString())
                .feedbackId(event.eventId())
                .type(type)
                .value(value)
                .reason(reason)
                .createdAt(Instant.now())
                .build();
    }
}
