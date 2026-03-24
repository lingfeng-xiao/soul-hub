package com.lingfeng.sprite.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * S29-1: Emotion State Machine
 *
 * 数字生命情绪状态机 - 管理Sprite自身的情绪状态转换
 *
 * 核心功能：
 * 1. 情绪状态管理 - 追踪当前情绪和强度
 * 2. 情绪转换 - 基于有效规则的状态转换
 * 3. 情绪混合 - 支持复合情绪（如 bittersweet = happy-sad）
 * 4. 时间衰减 - 随时间自动降低情绪强度
 * 5. 情绪历史 - 记录情绪变化供分析
 *
 * 情绪模型基于 Plutchik 情绪轮：
 * - JOY (喜悦)
 * - SADNESS (悲伤)
 * - ANGER (愤怒)
 * - FEAR (恐惧)
 * - SURPRISE (惊讶)
 * - DISGUST (厌恶)
 * - TRUST (信任)
 * - ANTICIPATION (期待)
 */
@Service
public class EmotionStateMachine {

    private static final Logger logger = LoggerFactory.getLogger(EmotionStateMachine.class);

    /**
     * 情绪枚举 - 基于 Plutchik 情绪轮
     */
    public enum Emotion {
        JOY,         // 喜悦
        SADNESS,     // 悲伤
        ANGER,       // 愤怒
        FEAR,        // 恐惧
        SURPRISE,    // 惊讶
        DISGUST,     // 厌恶
        TRUST,       // 信任
        ANTICIPATION // 期待
    }

    /**
     * 情绪状态记录
     *
     * @param emotion      当前情绪
     * @param intensity    情绪强度 (0.0 - 10.0)
     * @param timestamp    状态开始时间
     * @param decayRate    每秒衰减率 (0.0 - 1.0)
     */
    public record EmotionState(
            Emotion emotion,
            double intensity,
            Instant timestamp,
            double decayRate
    ) {
        public EmotionState {
            if (intensity < 0) intensity = 0;
            if (intensity > 10) intensity = 10;
            if (decayRate < 0) decayRate = 0;
            if (decayRate > 1) decayRate = 1;
        }

        /**
         * 创建默认情绪状态
         */
        public static EmotionState defaultState() {
            return new EmotionState(Emotion.JOY, 5.0, Instant.now(), 0.1);
        }

        /**
         * 创建指定情绪状态
         */
        public static EmotionState of(Emotion emotion, double intensity) {
            return new EmotionState(emotion, intensity, Instant.now(), getDefaultDecayRate(emotion));
        }

        /**
         * 获取默认衰减率
         */
        private static double getDefaultDecayRate(Emotion emotion) {
            return switch (emotion) {
                case ANGER, FEAR -> 0.15;  // 强烈情绪快速衰减
                case SURPRISE -> 0.2;       // 惊讶快速消退
                case JOY, SADNESS -> 0.1;   // 基础情绪正常衰减
                case TRUST, ANTICIPATION -> 0.08; // 持续情绪较慢衰减
                case DISGUST -> 0.12;       // 厌恶中等速度衰减
            };
        }

        /**
         * 检查状态是否已过期（强度降至接近0）
         */
        public boolean isExpired() {
            return intensity < 0.1;
        }
    }

    /**
     * 混合情绪记录 - 用于表示复合情绪
     *
     * @param primaryEmotion    主要情绪
     * @param secondaryEmotion 次要情绪
     * @param blendRatio       混合比例 (0.0 = 完全次要, 1.0 = 完全主要)
     * @param intensity        整体强度
     * @param timestamp         状态开始时间
     * @param decayRate        衰减率
     */
    public record BlendedEmotionState(
            Emotion primaryEmotion,
            Emotion secondaryEmotion,
            double blendRatio,
            double intensity,
            Instant timestamp,
            double decayRate
    ) {
        public static BlendedEmotionState of(Emotion primary, Emotion secondary, double ratio, double intensity) {
            return new BlendedEmotionState(
                    primary,
                    secondary,
                    ratio,
                    intensity,
                    Instant.now(),
                    calculateBlendedDecayRate(primary, secondary, ratio)
            );
        }

        private static double calculateBlendedDecayRate(Emotion primary, Emotion secondary, double ratio) {
            double primaryRate = EmotionState.of(primary, 5.0).decayRate();
            double secondaryRate = EmotionState.of(secondary, 5.0).decayRate();
            return primaryRate * ratio + secondaryRate * (1 - ratio);
        }

        /**
         * 获取混合情绪的描述名称
         */
        public String getBlendName() {
            return primaryEmotion().name().toLowerCase() + "-" + secondaryEmotion().name().toLowerCase();
        }
    }

    /**
     * 情绪转换事件
     */
    public record EmotionTransition(
            Emotion from,
            Emotion to,
            double intensity,
            Instant timestamp,
            String reason
    ) {}

    // 当前情绪状态
    private volatile EmotionState currentState;

    // 混合情绪状态（如果有）
    private volatile BlendedEmotionState blendedState;

    // 情绪历史
    private final List<EmotionTransition> transitionHistory = new ArrayList<>();

    // 情绪强度历史（用于趋势分析）
    private final Map<Emotion, List<Double>> intensityHistory = new ConcurrentHashMap<>();

    // 有效转换映射
    private static final Map<Emotion, Set<Emotion>> VALID_TRANSITIONS = new EnumMap<>(Emotion.class);

    // 情绪兼容矩阵（哪些情绪可以混合）
    private static final Map<Emotion, Set<Emotion>> EMOTION_COMBINATIONS = new EnumMap<>(Emotion.class);

    static {
        // 定义有效情绪转换规则
        // 相邻情绪可以转换（基于 Plutchik 情绪轮）
        VALID_TRANSITIONS.put(Emotion.JOY, Set.of(Emotion.SADNESS, Emotion.ANTICIPATION, Emotion.TRUST));
        VALID_TRANSITIONS.put(Emotion.SADNESS, Set.of(Emotion.JOY, Emotion.DISGUST, Emotion.FEAR));
        VALID_TRANSITIONS.put(Emotion.ANGER, Set.of(Emotion.FEAR, Emotion.DISGUST, Emotion.JOY));
        VALID_TRANSITIONS.put(Emotion.FEAR, Set.of(Emotion.ANGER, Emotion.SURPRISE, Emotion.JOY));
        VALID_TRANSITIONS.put(Emotion.SURPRISE, Set.of(Emotion.FEAR, Emotion.ANGER, Emotion.DISGUST));
        VALID_TRANSITIONS.put(Emotion.DISGUST, Set.of(Emotion.SADNESS, Emotion.ANGER, Emotion.SURPRISE));
        VALID_TRANSITIONS.put(Emotion.TRUST, Set.of(Emotion.JOY, Emotion.ANTICIPATION, Emotion.FEAR));
        VALID_TRANSITIONS.put(Emotion.ANTICIPATION, Set.of(Emotion.JOY, Emotion.SURPRISE, Emotion.TRUST));

        // 定义可混合的情绪组合
        EMOTION_COMBINATIONS.put(Emotion.JOY, Set.of(Emotion.SADNESS, Emotion.ANTICIPATION));
        EMOTION_COMBINATIONS.put(Emotion.SADNESS, Set.of(Emotion.JOY, Emotion.ANTICIPATION));
        EMOTION_COMBINATIONS.put(Emotion.ANGER, Set.of(Emotion.FEAR, Emotion.DISGUST));
        EMOTION_COMBINATIONS.put(Emotion.FEAR, Set.of(Emotion.ANGER, Emotion.SURPRISE));
        EMOTION_COMBINATIONS.put(Emotion.SURPRISE, Set.of(Emotion.FEAR, Emotion.JOY));
        EMOTION_COMBINATIONS.put(Emotion.DISGUST, Set.of(Emotion.ANGER, Emotion.SADNESS));
        EMOTION_COMBINATIONS.put(Emotion.TRUST, Set.of(Emotion.JOY, Emotion.ANTICIPATION));
        EMOTION_COMBINATIONS.put(Emotion.ANTICIPATION, Set.of(Emotion.JOY, Emotion.TRUST));
    }

    public EmotionStateMachine() {
        this.currentState = EmotionState.defaultState();
        logger.info("EmotionStateMachine initialized with default state: {}", currentState);
    }

    /**
     * S29-1: 获取当前情绪
     *
     * @return 当前情绪状态
     */
    public EmotionState getCurrentEmotion() {
        return currentState;
    }

    /**
     * S29-1: 转移情绪到新状态
     *
     * @param emotion   目标情绪
     * @param intensity 情绪强度 (0.0 - 10.0)
     * @throws IllegalArgumentException 如果情绪转换无效
     */
    public void transitionTo(Emotion emotion, double intensity) {
        if (emotion == null) {
            throw new IllegalArgumentException("Emotion cannot be null");
        }

        Emotion previousEmotion = currentState.emotion();

        // 检查转换是否有效
        if (!isValidTransition(previousEmotion, emotion)) {
            logger.warn("Invalid emotion transition attempted: {} -> {}", previousEmotion, emotion);
            // 不抛出异常，而是记录并强制转换
        }

        EmotionState newState = new EmotionState(emotion, intensity, Instant.now(), getDecayRate(emotion));

        // 记录转换历史
        EmotionTransition transition = new EmotionTransition(
                previousEmotion,
                emotion,
                intensity,
                Instant.now(),
                "direct_transition"
        );
        transitionHistory.add(transition);
        if (transitionHistory.size() > 100) {
            transitionHistory.remove(0);
        }

        // 记录强度历史
        recordIntensity(emotion, intensity);

        // 清除混合状态
        blendedState = null;

        this.currentState = newState;
        logger.debug("Emotion transitioned: {} -> {} (intensity={})", previousEmotion, emotion, intensity);
    }

    /**
     * S29-1: 混合两种情绪
     *
     * @param primary    主要情绪
     * @param secondary  次要情绪
     * @param blendRatio 混合比例 (0.0 = 完全次要, 1.0 = 完全主要)
     */
    public void blendEmotions(Emotion primary, Emotion secondary, double blendRatio) {
        if (primary == null || secondary == null) {
            throw new IllegalArgumentException("Emotions cannot be null");
        }

        // 验证混合比例
        blendRatio = Math.max(0.0, Math.min(1.0, blendRatio));

        // 检查是否支持混合
        if (!canBlend(primary, secondary)) {
            logger.warn("Cannot blend {} with {}", primary, secondary);
            // 不支持混合时，切换到主情绪
            transitionTo(primary, currentState.intensity());
            return;
        }

        // 计算混合后的强度
        double primaryIntensity = currentState.intensity() * blendRatio;
        double secondaryIntensity = currentState.intensity() * (1 - blendRatio);
        double blendedIntensity = Math.sqrt(primaryIntensity * primaryIntensity + secondaryIntensity * secondaryIntensity);
        blendedIntensity = Math.min(blendedIntensity, 10.0);

        blendedState = BlendedEmotionState.of(primary, secondary, blendRatio, blendedIntensity);

        logger.debug("Emotions blended: {}-{} (ratio={}, intensity={})",
                primary, secondary, blendRatio, blendedIntensity);
    }

    /**
     * S29-1: 更新情绪状态（时间衰减）
     *
     * @param elapsed 经过的时间
     */
    public void updateEmotion(Duration elapsed) {
        if (elapsed == null || elapsed.isNegative() || elapsed.isZero()) {
            return;
        }

        double seconds = elapsed.toMillis() / 1000.0;

        if (blendedState != null) {
            // 更新混合情绪
            updateBlendedEmotion(seconds);
        } else {
            // 更新普通情绪
            updateSimpleEmotion(seconds);
        }
    }

    /**
     * 更新简单情绪状态
     */
    private void updateSimpleEmotion(double seconds) {
        double decay = currentState.decayRate() * seconds;
        double newIntensity = Math.max(0, currentState.intensity() - decay);

        if (newIntensity != currentState.intensity()) {
            this.currentState = new EmotionState(
                    currentState.emotion(),
                    newIntensity,
                    currentState.timestamp(),
                    currentState.decayRate()
            );

            if (currentState.isExpired()) {
                // 情绪过期，回归默认状态
                logger.debug("Emotion expired, returning to default state");
                this.currentState = EmotionState.defaultState();
            }
        }
    }

    /**
     * 更新混合情绪状态
     */
    private void updateBlendedEmotion(double seconds) {
        double decay = blendedState.decayRate() * seconds;
        double newIntensity = Math.max(0, blendedState.intensity() - decay);

        if (newIntensity < 0.1) {
            // 混合情绪过期，回归简单状态
            logger.debug("Blended emotion expired, returning to simple state");
            transitionTo(blendedState.primaryEmotion(), blendedState.intensity() * blendedState.blendRatio());
            blendedState = null;
        } else {
            this.blendedState = new BlendedEmotionState(
                    blendedState.primaryEmotion(),
                    blendedState.secondaryEmotion(),
                    blendedState.blendRatio(),
                    newIntensity,
                    blendedState.timestamp(),
                    blendedState.decayRate()
            );
        }
    }

    /**
     * S29-1: 获取有效转移列表
     *
     * @return 从当前情绪可以转移到的情绪列表
     */
    public List<Emotion> getValidTransitions() {
        Set<Emotion> validSet = VALID_TRANSITIONS.getOrDefault(currentState.emotion(), Set.of());
        return new ArrayList<>(validSet);
    }

    /**
     * 检查情绪转换是否有效
     */
    public boolean isValidTransition(Emotion from, Emotion to) {
        if (from == null || to == null) {
            return false;
        }
        Set<Emotion> validTargets = VALID_TRANSITIONS.get(from);
        return validTargets != null && validTargets.contains(to);
    }

    /**
     * 检查两种情绪是否可以混合
     */
    public boolean canBlend(Emotion primary, Emotion secondary) {
        if (primary == null || secondary == null) {
            return false;
        }
        if (primary == secondary) {
            return false;
        }
        Set<Emotion> combinable = EMOTION_COMBINATIONS.get(primary);
        return combinable != null && combinable.contains(secondary);
    }

    /**
     * 获取当前混合状态
     */
    public BlendedEmotionState getBlendedState() {
        return blendedState;
    }

    /**
     * 检查是否处于混合情绪状态
     */
    public boolean isBlended() {
        return blendedState != null;
    }

    /**
     * 获取情绪转换历史
     */
    public List<EmotionTransition> getTransitionHistory() {
        return new ArrayList<>(transitionHistory);
    }

    /**
     * 获取特定情绪的强度历史
     */
    public List<Double> getIntensityHistory(Emotion emotion) {
        return new ArrayList<>(intensityHistory.getOrDefault(emotion, List.of()));
    }

    /**
     * 记录情绪强度到历史
     */
    private void recordIntensity(Emotion emotion, double intensity) {
        intensityHistory.computeIfAbsent(emotion, k -> new ArrayList<>()).add(intensity);
        // 限制历史长度
        List<Double> history = intensityHistory.get(emotion);
        if (history.size() > 50) {
            history.remove(0);
        }
    }

    /**
     * 获取情绪衰减率
     */
    private double getDecayRate(Emotion emotion) {
        return switch (emotion) {
            case ANGER, FEAR -> 0.15;
            case SURPRISE -> 0.2;
            case JOY, SADNESS -> 0.1;
            case TRUST, ANTICIPATION -> 0.08;
            case DISGUST -> 0.12;
        };
    }

    /**
     * 获取情绪描述
     */
    public String getEmotionDescription() {
        if (blendedState != null) {
            return String.format("%s (%.1f) - blended: %s",
                    blendedState.primaryEmotion(),
                    blendedState.intensity(),
                    blendedState.getBlendName());
        }
        return String.format("%s (%.1f)", currentState.emotion(), currentState.intensity());
    }

    /**
     * 获取情绪强度等级描述
     */
    public String getIntensityLevel() {
        double intensity = blendedState != null ? blendedState.intensity() : currentState.intensity();
        if (intensity < 2) return "very_low";
        if (intensity < 4) return "low";
        if (intensity < 6) return "medium";
        if (intensity < 8) return "high";
        return "very_high";
    }

    /**
     * 重置情绪状态到默认
     */
    public void reset() {
        this.currentState = EmotionState.defaultState();
        this.blendedState = null;
        logger.info("Emotion state reset to default");
    }

    /**
     * 增强当前情绪强度
     *
     * @param amount 增强量
     */
    public void amplifyEmotion(double amount) {
        double newIntensity = Math.min(10.0, currentState.intensity() + amount);
        this.currentState = new EmotionState(
                currentState.emotion(),
                newIntensity,
                Instant.now(),
                currentState.decayRate() * 0.9 // 增强后衰减变慢
        );
    }

    /**
     * 抑制当前情绪强度
     *
     * @param amount 抑制量
     */
    public void suppressEmotion(double amount) {
        double newIntensity = Math.max(0, currentState.intensity() - amount);
        this.currentState = new EmotionState(
                currentState.emotion(),
                newIntensity,
                Instant.now(),
                currentState.decayRate() * 1.2 // 抑制后衰减变快
        );
    }
}
