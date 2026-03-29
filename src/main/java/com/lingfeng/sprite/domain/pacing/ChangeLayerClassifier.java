package com.lingfeng.sprite.domain.pacing;

import java.util.Set;

/**
 * ChangeLayerClassifier - 变化分层分类器
 *
 * 根据变化类型和受影响的生命核心，确定变化的节速层级。
 *
 * 对应 IGN-091
 */
public final class ChangeLayerClassifier {

    private static final Set<String> FAST_KEYWORDS = Set.of(
            "prompt", "workflow", "task_strategy", "research_strategy",
            "temperature", "max_tokens", "response_format"
    );

    private static final Set<String> MEDIUM_KEYWORDS = Set.of(
            "reminder_style", "reminder_tone", "collaboration_rhythm",
            "learning_support", "initiative_strategy", "proactive_interval",
            "suggestion_style", "feedback_approach"
    );

    private static final Set<String> SLOW_KEYWORDS = Set.of(
            "identity_narrative", "personality", "relationship_pattern",
            "value", "belief", "core_identity", "essential_character",
            "relationship_style", "trust_approach", "communication_preference"
    );

    /**
     * 根据变化类型分类
     */
    public ChangeLayer classify(String changeType, String affectedCore) {
        String key = (changeType + "_" + affectedCore).toLowerCase();

        // First check by keywords in change type
        if (containsAny(key, FAST_KEYWORDS)) {
            return ChangeLayer.FAST;
        }
        if (containsAny(key, MEDIUM_KEYWORDS)) {
            return ChangeLayer.MEDIUM;
        }
        if (containsAny(key, SLOW_KEYWORDS)) {
            return ChangeLayer.SLOW;
        }

        // Then classify by affected core
        return classifyByCore(affectedCore);
    }

    /**
     * 根据受影响的生命核心分类
     */
    public ChangeLayer classifyByCore(String core) {
        if (core == null) {
            return ChangeLayer.FAST; // Default to fast for unknown
        }

        return switch (core.toLowerCase()) {
            case "identity", "self_narrative" -> ChangeLayer.SLOW;
            case "personality", "behavior_pattern", "relationship" -> ChangeLayer.SLOW;
            case "goal", "intention", "direction" -> ChangeLayer.MEDIUM;
            case "memory", "reflection", "learning" -> ChangeLayer.MEDIUM;
            case "skill", "capability", "action" -> ChangeLayer.FAST;
            case "boundary", "constraint" -> ChangeLayer.MEDIUM;
            default -> ChangeLayer.FAST;
        };
    }

    /**
     * 获取分类理由
     */
    public String getReason(ChangeLayer layer) {
        return switch (layer) {
            case FAST -> "快速层变化 - 可立即应用，无需额外确认";
            case MEDIUM -> "中速层变化 - 将解释变化内容后应用";
            case SLOW -> "慢速层变化 - 需要您的确认后才会应用";
        };
    }

    private boolean containsAny(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
