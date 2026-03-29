package com.lingfeng.sprite.feedback;

import java.util.Map;

/**
 * EmotionalContext - 情感上下文
 */
public record EmotionalContext(
        float valence,      // 效价: 正面/负面 (-1.0 到 1.0)
        float arousal,      // 唤醒度 (0.0 到 1.0)
        float dominance,    // 支配度 (0.0 到 1.0)
        String sentiment,    // 情感标签 (positive, negative, neutral)
        Map<String, Object> additionalData
) {

    public static EmotionalContext neutral() {
        return new EmotionalContext(0.0f, 0.0f, 0.5f, "neutral", null);
    }

    public static EmotionalContext of(float valence, float arousal, float dominance) {
        String sentiment = valence > 0.1 ? "positive" : valence < -0.1 ? "negative" : "neutral";
        return new EmotionalContext(valence, arousal, dominance, sentiment, null);
    }

    public static EmotionalContext of(float valence, float arousal, float dominance, Map<String, Object> additionalData) {
        String sentiment = valence > 0.1 ? "positive" : valence < -0.1 ? "negative" : "neutral";
        return new EmotionalContext(valence, arousal, dominance, sentiment, additionalData);
    }
}
