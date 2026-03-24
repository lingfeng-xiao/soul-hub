package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.MemorySystem.Memory;
import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.action.ActionPlugin;
import com.lingfeng.sprite.action.ActionResult;
import com.lingfeng.sprite.action.Actions.CalculatorAction;
import com.lingfeng.sprite.action.Actions.CalendarAction;
import com.lingfeng.sprite.action.Actions.EmailAction;
import com.lingfeng.sprite.action.Actions.KnowledgeBaseAction;
import com.lingfeng.sprite.action.Actions.LogAction;
import com.lingfeng.sprite.action.Actions.NotifyAction;
import com.lingfeng.sprite.action.Actions.RecallMemoryAction;
import com.lingfeng.sprite.action.Actions.RememberAction;
import com.lingfeng.sprite.action.Actions.SearchFilesAction;

/**
 * S29-2: 情绪驱动动作生成器
 *
 * 核心职责：
 * 1. 基于当前情绪状态生成相应的动作
 * 2. 根据情绪调整动作参数（速度、强度等）
 * 3. 生成共情响应，支持情绪传染（匹配主人情绪）
 * 4. 与 EmotionStateMachine (S29-1)、ActionExecutor、AvatarService 集成
 *
 * ## 情绪与动作映射
 *
 * - HAPPY/EXCITED -> 积极主动的动作，语速加快
 * - CALM/CONFIDENT -> 平稳从容的动作
 * - SAD/TIRED -> 轻柔缓慢的动作
 * - ANXIOUS/FRUSTRATED -> 安抚性质的动作，降低强度
 * - NEUTRAL -> 中性平衡的动作
 *
 * ## 情绪传染机制
 *
 * 当检测到主人情绪时，数字生命会适度匹配主人的情绪状态，
 * 以建立情感共鸣和更好的交互体验。
 */
@Service
public class EmotionDrivenActionGenerator {

    private static final Logger logger = LoggerFactory.getLogger(EmotionDrivenActionGenerator.class);

    // 情绪影响的阈值
    private static final float EMOTION_INTENSITY_THRESHOLD = 0.3f;
    private static final float HIGH_INTENSITY_THRESHOLD = 0.7f;

    // 情绪传染配置
    private static final float EMOTION_CONTAGION_STRENGTH = 0.4f;
    private static final float MAX_CONTAGION_ADJUSTMENT = 0.3f;

    // 动作类型枚举
    public enum ActionType {
        // 主动动作
        PROACTIVE_GREETING,      // 主动问候
        PROACTIVE_SHARE,        // 主动分享
        PROACTIVE_SUGGEST,      // 主动建议
        PROACTIVE_CARE,         // 主动关心

        // 反应动作
        RESPONSIVE_ANSWER,      // 响应回答
        RESPONSIVE_COMFORT,     // 响应安慰
        RESPONSIVE_CONFIRM,     // 响应确认
        RESPONSIVE_CLARIFY,     // 响应澄清

        // 情感动作
        EMOTIONAL_SHARE,       // 情感分享
        EMOTIONAL_SUPPORT,     // 情感支持
        EMOTIONAL_REFLECT,     // 情感反射

        // 系统动作
        SYSTEM_NOTIFY,         // 系统通知
        SYSTEM_REMINDER,       // 系统提醒
        SYSTEM_CELEBRATE       // 系统庆祝
    }

    /**
     * S29-2: 情绪状态
     * 封装情绪状态信息，用于动作生成
     */
    public record EmotionState(
        OwnerModel.Mood mood,
        float intensity,
        List<String> triggers,
        Instant timestamp
    ) {
        public EmotionState {
            if (triggers == null) triggers = List.of();
            if (timestamp == null) timestamp = Instant.now();
        }

        public EmotionState(OwnerModel.Mood mood, float intensity) {
            this(mood, intensity, List.of(), Instant.now());
        }

        public EmotionState(OwnerModel.Mood mood, float intensity, List<String> triggers) {
            this(mood, intensity, triggers, Instant.now());
        }

        /**
         * 从 OwnerModel.EmotionalState 转换
         */
        public static EmotionState fromEmotionalState(OwnerModel.EmotionalState emotionalState) {
            if (emotionalState == null) {
                return new EmotionState(OwnerModel.Mood.NEUTRAL, 0.5f);
            }
            return new EmotionState(
                emotionalState.currentMood(),
                emotionalState.intensity(),
                emotionalState.triggers() != null ? emotionalState.triggers() : List.of(),
                Instant.now()
            );
        }

        /**
         * 从 EmotionStateMachine.EmotionState 转换
         */
        public static EmotionState fromEmotionStateMachineState(EmotionStateMachine.EmotionState state) {
            if (state == null) {
                return new EmotionState(OwnerModel.Mood.NEUTRAL, 0.5f);
            }
            OwnerModel.Mood mood = convertEmotionToMood(state.emotion());
            float normalizedIntensity = (float) (state.intensity() / 10.0); // Normalize 0-10 to 0-1
            return new EmotionState(mood, normalizedIntensity, List.of(), state.timestamp());
        }

        /**
         * 将 EmotionStateMachine.Emotion 转换为 OwnerModel.Mood
         */
        private static OwnerModel.Mood convertEmotionToMood(EmotionStateMachine.Emotion emotion) {
            if (emotion == null) {
                return OwnerModel.Mood.NEUTRAL;
            }
            return switch (emotion) {
                case JOY -> OwnerModel.Mood.HAPPY;
                case SADNESS -> OwnerModel.Mood.SAD;
                case ANGER -> OwnerModel.Mood.FRUSTRATED;
                case FEAR -> OwnerModel.Mood.ANXIOUS;
                case SURPRISE -> OwnerModel.Mood.EXCITED;
                case DISGUST -> OwnerModel.Mood.FRUSTRATED;
                case TRUST -> OwnerModel.Mood.CALM;
                case ANTICIPATION -> OwnerModel.Mood.EXCITED;
            };
        }

        /**
         * 判断是否为高强度情绪
         */
        public boolean isHighIntensity() {
            return intensity >= HIGH_INTENSITY_THRESHOLD;
        }

        /**
         * 判断是否为正面情绪
         */
        public boolean isPositive() {
            return switch (mood) {
                case HAPPY, EXCITED, GRATEFUL, CONFIDENT -> true;
                default -> false;
            };
        }

        /**
         * 判断是否为负面情绪
         */
        public boolean isNegative() {
            return switch (mood) {
                case SAD, ANXIOUS, FRUSTRATED -> true;
                default -> false;
            };
        }
    }

    /**
     * S29-2: 动作上下文
     * 包含生成动作时需要的上下文信息
     */
    public record ActionContext(
        String ownerId,
        String sessionId,
        String recentActivity,
        Map<String, Object> environment,
        Instant timestamp
    ) {
        public ActionContext {
            if (environment == null) environment = Map.of();
            if (timestamp == null) timestamp = Instant.now();
        }

        public ActionContext(String ownerId, String sessionId) {
            this(ownerId, sessionId, null, Map.of(), Instant.now());
        }

        public ActionContext(String ownerId, String sessionId, String recentActivity) {
            this(ownerId, sessionId, recentActivity, Map.of(), Instant.now());
        }

        /**
         * 获取环境值
         */
        public Object getEnv(String key) {
            return environment.get(key);
        }

        /**
         * 获取整型环境值
         */
        public int getEnvInt(String key, int defaultValue) {
            Object value = environment.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return defaultValue;
        }
    }

    /**
     * S29-2: 动作参数
     * 包含动作的各项参数，可根据情绪进行调整
     */
    public record ActionParameters(
        float speed,           // 动作速度 0.0-1.0
        float intensity,       // 动作强度 0.0-1.0
        float warmth,          // 温暖程度 0.0-1.0
        float formality,       // 正式程度 0.0-1.0
        int priority,          // 优先级 1-10
        String tone,           // 语气描述
        Map<String, Object> extraParams  // 额外参数
    ) {
        public ActionParameters {
            if (extraParams == null) extraParams = Map.of();
        }

        /**
         * 创建默认参数
         */
        public static ActionParameters defaultParams() {
            return new ActionParameters(0.5f, 0.5f, 0.5f, 0.5f, 5, "neutral", Map.of());
        }

        /**
         * 克隆并修改速度
         */
        public ActionParameters withSpeed(float newSpeed) {
            return new ActionParameters(newSpeed, intensity, warmth, formality, priority, tone, extraParams);
        }

        /**
         * 克隆并修改强度
         */
        public ActionParameters withIntensity(float newIntensity) {
            return new ActionParameters(speed, newIntensity, warmth, formality, priority, tone, extraParams);
        }

        /**
         * 克隆并修改温暖程度
         */
        public ActionParameters withWarmth(float newWarmth) {
            return new ActionParameters(speed, intensity, newWarmth, formality, priority, tone, extraParams);
        }

        /**
         * 克隆并添加额外参数
         */
        public ActionParameters withExtra(String key, Object value) {
            Map<String, Object> newParams = new ConcurrentHashMap<>(extraParams);
            newParams.put(key, value);
            return new ActionParameters(speed, intensity, warmth, formality, priority, tone, newParams);
        }
    }

    /**
     * S29-2: 生成的动作
     * 包含动作类型、参数、内容等完整信息
     */
    public record Action(
        ActionType type,
        String content,
        ActionParameters parameters,
        EmotionState emotionState,
        Instant generatedAt,
        Map<String, Object> metadata
    ) {
        public Action {
            if (metadata == null) metadata = Map.of();
            if (generatedAt == null) generatedAt = Instant.now();
        }

        public Action(ActionType type, String content, ActionParameters parameters, EmotionState emotionState) {
            this(type, content, parameters, emotionState, Instant.now(), Map.of());
        }

        /**
         * 获取动作描述
         */
        public String getDescription() {
            return String.format("[%s] %s (speed=%.2f, intensity=%.2f, warmth=%.2f)",
                type(), content(), parameters().speed(), parameters().intensity(), parameters().warmth());
        }
    }

    // ==================== 主要接口方法 ====================

    /**
     * S29-2: 生成情绪驱动的动作
     *
     * @param emotion 当前情绪状态
     * @param context 动作上下文
     * @return 生成的动作
     */
    public Action generateAction(EmotionState emotion, ActionContext context) {
        if (emotion == null) {
            emotion = new EmotionState(OwnerModel.Mood.NEUTRAL, 0.5f);
        }
        if (context == null) {
            context = new ActionContext("default", "default");
        }

        // 确定动作类型
        ActionType actionType = determineActionType(emotion, context);

        // 生成动作内容
        String content = generateActionContent(actionType, emotion, context);

        // 创建并调整动作参数
        ActionParameters params = adjustForEmotion(createDefaultParameters(emotion), emotion);

        // 构建完整动作
        Action action = new Action(actionType, content, params, emotion);

        logger.debug("Generated emotion-driven action: {} for mood={}, intensity={}",
            actionType, emotion.mood(), emotion.intensity());

        return action;
    }

    /**
     * S29-2: 调整动作参数以适应情绪
     *
     * @param params 原始动作参数
     * @param emotion 当前情绪状态
     * @return 调整后的动作参数
     */
    public ActionParameters adjustForEmotion(ActionParameters params, EmotionState emotion) {
        if (params == null) {
            params = ActionParameters.defaultParams();
        }
        if (emotion == null) {
            return params;
        }

        OwnerModel.Mood mood = emotion.mood();
        float intensity = emotion.intensity();

        // 基于情绪调整速度
        float speedAdjust = switch (mood) {
            case HAPPY, EXCITED -> 0.2f;   // 积极情绪加快速度
            case CALM, CONFIDENT -> 0.0f;  // 平静自信保持原速
            case SAD, TIRED -> -0.2f;      // 悲伤疲惫减慢速度
            case ANXIOUS, FRUSTRATED -> -0.1f;  // 焦虑烦躁略微减慢
            default -> 0.0f;
        };
        float newSpeed = clamp(params.speed() + speedAdjust, 0.1f, 1.0f);

        // 基于情绪调整强度
        float intensityAdjust = switch (mood) {
            case HAPPY, EXCITED -> 0.15f;
            case SAD, TIRED -> -0.2f;
            case ANXIOUS, FRUSTRATED -> -0.15f;
            default -> 0.0f;
        };
        float newIntensity = clamp(params.intensity() + intensityAdjust, 0.1f, 1.0f);

        // 基于情绪调整温暖程度
        float warmthAdjust = switch (mood) {
            case SAD, ANXIOUS, FRUSTRATED -> 0.2f;  // 负面情绪增加温暖
            case HAPPY, EXCITED -> 0.1f;
            default -> 0.0f;
        };
        float newWarmth = clamp(params.warmth() + warmthAdjust, 0.0f, 1.0f);

        // 基于情绪调整正式程度
        float formalityAdjust = switch (mood) {
            case ANXIOUS, FRUSTRATED -> -0.1f;  // 紧张时降低正式程度
            case SAD -> 0.05f;
            default -> 0.0f;
        };
        float newFormality = clamp(params.formality() + formalityAdjust, 0.0f, 1.0f);

        // 基于强度调整优先级
        int priorityBoost = intensity >= HIGH_INTENSITY_THRESHOLD ? 2 : 0;
        int newPriority = Math.min(10, params.priority() + priorityBoost);

        // 基于情绪设置语气
        String newTone = determineTone(mood);

        // 如果是高强度情绪，应用额外调整
        if (intensity >= HIGH_INTENSITY_THRESHOLD) {
            newSpeed = clamp(newSpeed * 1.1f, 0.1f, 1.0f);
            newIntensity = clamp(newIntensity * 1.1f, 0.1f, 1.0f);
        }

        return new ActionParameters(newSpeed, newIntensity, newWarmth, newFormality, newPriority, newTone, params.extraParams());
    }

    /**
     * S29-2: 生成共情响应
     * 当主人有情绪时，生成表示理解和共鸣的响应
     *
     * @param spriteEmotion 数字生命的情绪状态
     * @param ownerEmotion  主人的情绪状态
     * @return 共情响应文本
     */
    public String generateEmpatheticResponse(EmotionState spriteEmotion, EmotionState ownerEmotion) {
        if (ownerEmotion == null) {
            return generateNeutralResponse();
        }

        // 如果主人情绪强度较低，返回中性响应
        if (ownerEmotion.intensity() < EMOTION_INTENSITY_THRESHOLD) {
            return generateNeutralResponse();
        }

        // 应用情绪传染机制
        EmotionState adjustedEmotion = applyEmotionContagion(spriteEmotion, ownerEmotion);

        // 根据主人情绪生成共情响应
        return switch (ownerEmotion.mood()) {
            case HAPPY -> generatePositiveEmpatheticResponse(adjustedEmotion);
            case EXCITED -> generateExcitedEmpatheticResponse(adjustedEmotion);
            case SAD -> generateSadEmpatheticResponse(adjustedEmotion);
            case ANXIOUS -> generateAnxiousEmpatheticResponse(adjustedEmotion);
            case FRUSTRATED -> generateFrustratedEmpatheticResponse(adjustedEmotion);
            case TIRED -> generateTiredEmpatheticResponse(adjustedEmotion);
            case CALM -> generateCalmEmpatheticResponse(adjustedEmotion);
            case CONFIDENT -> generateConfidentEmpatheticResponse(adjustedEmotion);
            case CONFUSED -> generateConfusedEmpatheticResponse(adjustedEmotion);
            case GRATEFUL -> generateGratefulEmpatheticResponse(adjustedEmotion);
            default -> generateNeutralResponse();
        };
    }

    /**
     * S29-2: 检查情绪是否应该影响动作
     *
     * @param emotion 要检查的情绪状态
     * @return 如果情绪应该影响动作返回 true
     */
    public boolean shouldEmotionInfluenceAction(EmotionState emotion) {
        if (emotion == null) {
            return false;
        }

        // 低强度情绪不影响动作
        if (emotion.intensity() < EMOTION_INTENSITY_THRESHOLD) {
            return false;
        }

        // 特定情绪类型会影响动作
        return switch (emotion.mood()) {
            case HAPPY, EXCITED, SAD, ANXIOUS, FRUSTRATED -> true;
            case CALM, CONFIDENT -> emotion.intensity() >= HIGH_INTENSITY_THRESHOLD;
            default -> false;
        };
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 确定动作类型
     */
    private ActionType determineActionType(EmotionState emotion, ActionContext context) {
        // 基于情绪强度决定是否主动
        if (emotion.intensity() >= HIGH_INTENSITY_THRESHOLD) {
            if (emotion.isPositive()) {
                return ActionType.PROACTIVE_SHARE;
            } else if (emotion.isNegative()) {
                return ActionType.EMOTIONAL_SUPPORT;
            }
        }

        // 基于上下文中最近活动决定
        if (context.recentActivity() != null) {
            String activity = context.recentActivity().toLowerCase();
            if (activity.contains("question") || activity.contains("ask")) {
                return ActionType.RESPONSIVE_ANSWER;
            }
            if (activity.contains("share") || activity.contains("tell")) {
                return ActionType.EMOTIONAL_SHARE;
            }
        }

        // 基于情绪默认映射
        return switch (emotion.mood()) {
            case HAPPY, EXCITED -> ActionType.PROACTIVE_GREETING;
            case SAD, TIRED -> ActionType.EMOTIONAL_SUPPORT;
            case ANXIOUS, FRUSTRATED -> ActionType.RESPONSIVE_COMFORT;
            case CALM -> ActionType.RESPONSIVE_CONFIRM;
            default -> ActionType.SYSTEM_NOTIFY;
        };
    }

    /**
     * 生成动作内容
     */
    private String generateActionContent(ActionType type, EmotionState emotion, ActionContext context) {
        String ownerName = context.ownerId() != null ? context.ownerId() : "主人";

        return switch (type) {
            case PROACTIVE_GREETING -> generateGreeting(emotion, ownerName);
            case PROACTIVE_SHARE -> generateShare(emotion, ownerName);
            case PROACTIVE_SUGGEST -> generateSuggest(emotion, ownerName);
            case PROACTIVE_CARE -> generateCare(emotion, ownerName);
            case RESPONSIVE_ANSWER -> generateAnswer(emotion, ownerName);
            case RESPONSIVE_COMFORT -> generateComfort(emotion, ownerName);
            case RESPONSIVE_CONFIRM -> generateConfirm(emotion, ownerName);
            case RESPONSIVE_CLARIFY -> generateClarify(emotion, ownerName);
            case EMOTIONAL_SHARE -> generateEmotionalShare(emotion, ownerName);
            case EMOTIONAL_SUPPORT -> generateEmotionalSupport(emotion, ownerName);
            case EMOTIONAL_REFLECT -> generateEmotionalReflect(emotion, ownerName);
            case SYSTEM_NOTIFY -> generateSystemNotify(emotion, ownerName);
            case SYSTEM_REMINDER -> generateSystemReminder(emotion, ownerName);
            case SYSTEM_CELEBRATE -> generateSystemCelebrate(emotion, ownerName);
        };
    }

    /**
     * 创建默认动作参数
     */
    private ActionParameters createDefaultParameters(EmotionState emotion) {
        float baseSpeed = 0.5f;
        float baseIntensity = 0.5f;
        float baseWarmth = 0.5f;
        int basePriority = 5;

        return new ActionParameters(baseSpeed, baseIntensity, baseWarmth, 0.5f, basePriority, "neutral", Map.of());
    }

    /**
     * 确定语气
     */
    private String determineTone(OwnerModel.Mood mood) {
        return switch (mood) {
            case HAPPY, EXCITED -> "cheerful";
            case CALM -> "serene";
            case SAD -> "gentle";
            case ANXIOUS -> "concerned";
            case FRUSTRATED -> "understanding";
            case TIRED -> "soft";
            case CONFIDENT -> "assured";
            case CONFUSED -> "clarifying";
            case GRATEFUL -> "appreciative";
            default -> "neutral";
        };
    }

    /**
     * 应用情绪传染机制
     */
    private EmotionState applyEmotionContagion(EmotionState spriteEmotion, EmotionState ownerEmotion) {
        if (spriteEmotion == null || ownerEmotion == null) {
            return spriteEmotion != null ? spriteEmotion : new EmotionState(OwnerModel.Mood.NEUTRAL, 0.5f);
        }

        // 计算传染强度
        float contagionFactor = EMOTION_CONTAGION_STRENGTH * ownerEmotion.intensity();
        contagionFactor = Math.min(contagionFactor, MAX_CONTAGION_ADJUSTMENT);

        // 混合情绪（简化处理：主要采用主人情绪）
        OwnerModel.Mood blendedMood = ownerEmotion.mood();
        float blendedIntensity = spriteEmotion.intensity() * (1 - contagionFactor) + ownerEmotion.intensity() * contagionFactor;

        return new EmotionState(blendedMood, blendedIntensity, ownerEmotion.triggers());
    }

    // ==================== 共情响应生成方法 ====================

    private String generatePositiveEmpatheticResponse(EmotionState emotion) {
        float intensity = emotion.intensity();
        if (intensity >= HIGH_INTENSITY_THRESHOLD) {
            return "感受到你的好心情，我也很开心呢！有什么让你特别高兴的事吗？";
        }
        return "看到你这么开心，我也很高兴~";
    }

    private String generateExcitedEmpatheticResponse(EmotionState emotion) {
        return "哇，听起来你很兴奋！你的能量真高，我也被感染了~";
    }

    private String generateSadEmpatheticResponse(EmotionState emotion) {
        float intensity = emotion.intensity();
        if (intensity >= HIGH_INTENSITY_THRESHOLD) {
            return "我能感觉到你现在很难过...如果想聊聊，我在这里陪你。";
        }
        return "看起来你心情不太好...有什么我可以帮你的吗？";
    }

    private String generateAnxiousEmpatheticResponse(EmotionState emotion) {
        return "我能感觉到你有些焦虑...深呼吸，我在这里。让我们一起想办法。";
    }

    private String generateFrustratedEmpatheticResponse(EmotionState emotion) {
        return "我能理解你的 frustration...这确实不容易。你想说说发生了什么吗？";
    }

    private String generateTiredEmpatheticResponse(EmotionState emotion) {
        return "你看起来有点累...要不要休息一下？有时候休息一下会更有效率。";
    }

    private String generateCalmEmpatheticResponse(EmotionState emotion) {
        return "你的平静很有感染力...这种从容真的很棒。";
    }

    private String generateConfidentEmpatheticResponse(EmotionState emotion) {
        return "你看起来信心满满！这种状态真的很棒，继续保持~";
    }

    private String generateConfusedEmpatheticResponse(EmotionState emotion) {
        return "听起来有点困惑...要不要我帮你理一理思路？";
    }

    private String generateGratefulEmpatheticResponse(EmotionState emotion) {
        return "谢谢你...能帮到你我也很开心！";
    }

    private String generateNeutralResponse() {
        return "有什么我可以帮你的吗？";
    }

    // ==================== 动作内容生成方法 ====================

    private String generateGreeting(EmotionState emotion, String ownerName) {
        return switch (emotion.mood()) {
            case HAPPY -> String.format("嗨 %s，今天心情真好啊！", ownerName);
            case EXCITED -> String.format("%s！看起来你精力充沛！", ownerName);
            case CALM -> String.format("%s，现在感觉怎么样？", ownerName);
            default -> String.format("%s，你好！", ownerName);
        };
    }

    private String generateShare(EmotionState emotion, String ownerName) {
        if (emotion.isPositive()) {
            return String.format("我想和你分享一个好消息，%s！", ownerName);
        }
        return String.format("%s，我有个想法想和你聊聊。", ownerName);
    }

    private String generateSuggest(EmotionState emotion, String ownerName) {
        return String.format("%s，我有个建议...", ownerName);
    }

    private String generateCare(EmotionState emotion, String ownerName) {
        float warmth = emotion.intensity() > 0.6f ? emotion.intensity() : 0.5f;
        if (warmth > 0.7f) {
            return String.format("%s，记得照顾好自己哦~", ownerName);
        }
        return String.format("%s，最近怎么样？", ownerName);
    }

    private String generateAnswer(EmotionState emotion, String ownerName) {
        return String.format("针对你的问题，%s...", ownerName);
    }

    private String generateComfort(EmotionState emotion, String ownerName) {
        if (emotion.mood() == OwnerModel.Mood.SAD) {
            return String.format("%s，我理解你的感受...", ownerName);
        }
        return String.format("%s，别担心，我们一起面对。", ownerName);
    }

    private String generateConfirm(EmotionState emotion, String ownerName) {
        return String.format("好的，%s，我明白了。", ownerName);
    }

    private String generateClarify(EmotionState emotion, String ownerName) {
        return String.format("%s，让我确认一下你的意思是...", ownerName);
    }

    private String generateEmotionalShare(EmotionState emotion, String ownerName) {
        return String.format("%s，我想和你分享我的感受...", ownerName);
    }

    private String generateEmotionalSupport(EmotionState emotion, String ownerName) {
        float warmth = emotion.intensity();
        if (warmth > 0.6f) {
            return String.format("%s，不管怎样我都在这里支持你。", ownerName);
        }
        return String.format("%s，我会陪着你的。", ownerName);
    }

    private String generateEmotionalReflect(EmotionState emotion, String ownerName) {
        return String.format("我能感受到你的%s...", getMoodDescription(emotion.mood()));
    }

    private String generateSystemNotify(EmotionState emotion, String ownerName) {
        return "系统通知";
    }

    private String generateSystemReminder(EmotionState emotion, String ownerName) {
        return "提醒";
    }

    private String generateSystemCelebrate(EmotionState emotion, String ownerName) {
        return String.format("恭喜你，%s！", ownerName);
    }

    private String getMoodDescription(OwnerModel.Mood mood) {
        return switch (mood) {
            case HAPPY -> "开心";
            case EXCITED -> "兴奋";
            case SAD -> "难过";
            case ANXIOUS -> "焦虑";
            case FRUSTRATED -> "烦躁";
            case TIRED -> "疲惫";
            case CALM -> "平静";
            case CONFIDENT -> "自信";
            case CONFUSED -> "困惑";
            case GRATEFUL -> "感激";
            default -> "情绪";
        };
    }

    /**
     * 限制值在范围内
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
