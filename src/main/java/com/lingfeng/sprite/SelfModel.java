package com.lingfeng.sprite;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 数字生命的自我模型 - 认知自己是"谁"
 *
 * ## 架构设计
 *
 * SelfModel.Self
 * ├── identity         // 身份认同（beingId 跨平台一致）
 * ├── personality      // 人格特质（可演变）
 * ├── capabilities     // 能力认知
 * ├── avatars          // 分身列表（多设备感知）
 * ├── metacognition    // 元认知（关于如何思考的知识）
 * ├── growthHistory    // 成长轨迹
 * ├── evolutionLevel   // 进化等级
 * └── evolutionCount   // 进化次数
 *
 * ## 核心概念
 *
 * - IdentityCore - 自我认同核心（beingId 不可变）
 * - Personality - 人格特质，包括本质、气质、价值观
 * - Avatar - 分身，数字生命在某个设备上的实例
 * - Value - 价值观，带权重和适用情境
 * - Capability - 能力，带水平和置信度
 * - Metacognition - 元认知，包括学习风格、决策模式、已知盲点
 * - GrowthEvent - 成长事件记录
 *
 * ## 不变性约束
 *
 * - IdentityCore.beingId 不可变（跨平台一致性）
 * - IdentityCore.createdAt 不可变
 * - evolutionLevel 只增不减
 */
public final class SelfModel {

    private SelfModel() {}

    /**
     * 自我认同核心
     */
    public record IdentityCore(
        String beingId,
        String displayName,
        String essence,
        String emoji,
        String vibe,
        Instant createdAt,
        List<String> continuityChain
    ) {
        public IdentityCore {
            continuityChain = continuityChain != null ? List.copyOf(continuityChain) : List.of();
        }

        public static IdentityCore create(String beingId, String displayName) {
            return new IdentityCore(
                beingId,
                displayName,
                "",
                "",
                "",
                Instant.now(),
                List.of()
            );
        }

        public static IdentityCore createDefault() {
            return new IdentityCore(
                "",
                "雪梨",
                "",
                "",
                "",
                Instant.now(),
                List.of()
            );
        }
    }

    /**
     * 价值观
     */
    public record Value(
        String name,
        float weight,
        String description,
        String situation
    ) {
        public Value {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name cannot be null or blank");
            }
            situation = situation != null ? situation : "";
        }

        public Value(String name, float weight, String description) {
            this(name, weight, description, "");
        }
    }

    /**
     * 能力
     */
    public enum CapabilityLevel {
        MASTER,
        ADVANCED,
        BASIC,
        NONE
    }

    /**
     * 能力
     */
    public record Capability(
        String name,
        CapabilityLevel level,
        float confidence,
        Instant lastPracticed
    ) {
        public Capability {
            lastPracticed = lastPracticed;
        }

        public Capability(String name, CapabilityLevel level, float confidence) {
            this(name, level, confidence, null);
        }

        public Capability withLevel(CapabilityLevel newLevel, float newConfidence) {
            return new Capability(name, newLevel, newConfidence, Instant.now());
        }
    }

    /**
     * 元认知 - 关于"如何思考"的知识
     */
    public record Metacognition(
        String learningStyle,
        List<String> decisionPatterns,
        List<String> blindSpots,
        List<String> strengths,
        List<Reflection> reflectionHistory
    ) {
        public Metacognition {
            reflectionHistory = reflectionHistory != null ? List.copyOf(reflectionHistory) : List.of();
            decisionPatterns = decisionPatterns != null ? List.copyOf(decisionPatterns) : List.of();
            blindSpots = blindSpots != null ? List.copyOf(blindSpots) : List.of();
            strengths = strengths != null ? List.copyOf(strengths) : List.of();
        }

        public Metacognition withReflection(Reflection reflection) {
            List<Reflection> newHistory = new ArrayList<>(reflectionHistory);
            newHistory.add(reflection);
            return new Metacognition(learningStyle, decisionPatterns, blindSpots, strengths, newHistory);
        }
    }

    /**
     * 反思记录
     */
    public record Reflection(
        Instant timestamp,
        String trigger,
        String insight,
        String behaviorChange
    ) {
        public Reflection {
            behaviorChange = behaviorChange != null ? behaviorChange : "";
        }
    }

    /**
     * 成长事件
     */
    public record GrowthEvent(
        Instant timestamp,
        GrowthType type,
        String description,
        String before,
        String after,
        String trigger
    ) {
        public GrowthEvent(Instant timestamp, GrowthType type, String description, String before, String after) {
            this(timestamp, type, description, before, after, null);
        }
    }

    /**
     * 成长类型
     */
    public enum GrowthType {
        CAPABILITY_IMPROVED,
        BELIEF_CHANGED,
        VALUE_CLARIFIED,
        SKILL_ACQUIRED,
        WEAKNESS_RECOGNIZED,
        GOAL_ACHIEVED,
        IDENTITY_DEEPENED
    }

    // ==================== 分身感知 ====================

    /**
     * 分身 - 数字生命在某个设备上的实例
     */
    public record Avatar(
        String deviceId,
        String deviceType,
        Instant lastSeen,
        String localContext
    ) {
        public Avatar {
            if (localContext == null) localContext = null;
        }
    }

    /**
     * 分身列表
     */
    public record Avatars(
        List<Avatar> instances
    ) {
        public Avatars {
            instances = instances != null ? List.copyOf(instances) : List.of();
        }

        public Avatars() {
            this(List.of());
        }
    }

    // ==================== 人格特质 ====================

    /**
     * 人格特质 - 可演变
     */
    public record Personality(
        String essence,           // 本质定义（我是谁）
        String vibe,           // 气质风格
        List<Value> values,    // 价值观
        List<String> decisionPatterns,  // 决策模式
        List<String> blindSpots,       // 认知盲点
        List<String> strengths          // 优势
    ) {
        public Personality {
            values = values != null ? List.copyOf(values) : List.of();
            decisionPatterns = decisionPatterns != null ? List.copyOf(decisionPatterns) : List.of();
            blindSpots = blindSpots != null ? List.copyOf(blindSpots) : List.of();
            strengths = strengths != null ? List.copyOf(strengths) : List.of();
        }

        public static Personality empty() {
            return new Personality("", "", List.of(), List.of(), List.of(), List.of());
        }
    }

    /**
     * 自我模型完整视图
     */
    public record Self(
        IdentityCore identity,
        Personality personality,
        List<Capability> capabilities,
        Avatars avatars,
        Metacognition metacognition,
        List<GrowthEvent> growthHistory,
        int evolutionLevel,
        int evolutionCount
    ) {
        public Self {
            capabilities = capabilities != null ? List.copyOf(capabilities) : List.of();
            growthHistory = growthHistory != null ? List.copyOf(growthHistory) : List.of();
        }

        /**
         * 创建默认的 Sprite 自我模型
         */
        public static Self createDefault() {
            return new Self(
                IdentityCore.createDefault(),
                Personality.empty(),
                List.of(),
                new Avatars(),
                new Metacognition(
                    "",
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
                ),
                List.of(),
                1,
                0
            );
        }

        /**
         * 记录成长事件
         */
        public Self recordGrowth(GrowthType type, String description, String before, String after) {
            return recordGrowth(type, description, before, after, null);
        }

        public Self recordGrowth(GrowthType type, String description, String before, String after, String trigger) {
            GrowthEvent event = new GrowthEvent(Instant.now(), type, description, before, after, trigger);
            List<GrowthEvent> newHistory = new ArrayList<>(growthHistory);
            newHistory.add(event);
            return new Self(identity, personality, capabilities, avatars, metacognition, newHistory, evolutionLevel, evolutionCount + 1);
        }

        /**
         * 更新能力水平
         */
        public Self updateCapability(String name, CapabilityLevel newLevel, float confidence) {
            List<Capability> updatedCapabilities = new ArrayList<>();
            Capability oldCapability = null;
            for (Capability cap : capabilities) {
                if (cap.name().equals(name)) {
                    Capability updated = cap.withLevel(newLevel, confidence);
                    updatedCapabilities.add(updated);
                    oldCapability = cap;
                } else {
                    updatedCapabilities.add(cap);
                }
            }

            String oldLevel = oldCapability != null ? oldCapability.level().name() : "UNKNOWN";
            return recordGrowth(
                GrowthType.CAPABILITY_IMPROVED,
                "能力提升: " + name,
                oldLevel,
                newLevel.name(),
                "练习或学习"
            ).withCapabilities(updatedCapabilities);
        }

        /**
         * 添加反思
         */
        public Self addReflection(String trigger, String insight) {
            return addReflection(trigger, insight, null);
        }

        public Self addReflection(String trigger, String insight, String behaviorChange) {
            Reflection reflection = new Reflection(Instant.now(), trigger, insight, behaviorChange);
            return new Self(
                identity,
                personality,
                capabilities,
                avatars,
                metacognition.withReflection(reflection),
                growthHistory,
                evolutionLevel,
                evolutionCount
            );
        }

        /**
         * 获取核心价值观描述
         */
        public String getCoreValuesSummary() {
            return personality.values().stream()
                .sorted(Comparator.comparingDouble(Value::weight).reversed())
                .limit(3)
                .map(Value::name)
                .collect(Collectors.joining(" > "));
        }

        // With methods for immutable updates
        public Self withIdentity(IdentityCore newIdentity) {
            return new Self(newIdentity, personality, capabilities, avatars, metacognition, growthHistory, evolutionLevel, evolutionCount);
        }

        public Self withCapabilities(List<Capability> newCapabilities) {
            return new Self(identity, personality, newCapabilities, avatars, metacognition, growthHistory, evolutionLevel, evolutionCount);
        }

        public Self withPersonality(Personality newPersonality) {
            return new Self(identity, newPersonality, capabilities, avatars, metacognition, growthHistory, evolutionLevel, evolutionCount);
        }
    }
}
