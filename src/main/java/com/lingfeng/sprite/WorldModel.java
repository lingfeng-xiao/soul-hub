package com.lingfeng.sprite;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 世界模型 - 对主人和环境的深度理解
 *
 * ## 架构设计
 *
 * ```
 * WorldModel
 * ├── OwnerModel.Owner     // 主人（他者中最重要的创造者）
 * ├── PhysicalWorld         // 物理世界（位置、设备）
 * ├── SocialGraph          // 社会关系图谱
 * ├── KnowledgeGraph       // 知识图谱
 * └── Context              // 当前情境
 * ```
 *
 * ## 与 OwnerModel 的关系
 *
 * - 使用 OwnerModel.Owner 作为主人模型
 * - 主人是"他者"中最重要的创造者
 * - 数字生命认识主人的一切
 */
public final class WorldModel {

    private WorldModel() {}

    // ==================== 枚举类型 ====================

    public enum LocationType {
        HOME, WORK, TRAVEL, OTHER
    }

    public enum DeviceType {
        PHONE, PC, CLOUD, TABLET, OTHER
    }

    public enum Activity {
        WORK, LEISURE, SLEEP, COMMUTE, MEAL, UNKNOWN
    }

    // ==================== 数据类型 ====================

    /**
     * 物理位置
     */
    public record Location(
        String id,
        String name,
        String address,
        LocationType type
    ) {
        public Location {
            if (address == null) address = null;
        }

        public Location(String id, String name, LocationType type) {
            this(id, name, null, type);
        }
    }

    /**
     * 物理世界知识
     */
    public record PhysicalWorld(
        List<Location> locations,
        List<OwnerModel.Device> devices,
        List<OwnerModel.Schedule> schedules
    ) {
        public PhysicalWorld {
            locations = locations != null ? List.copyOf(locations) : List.of();
            devices = devices != null ? List.copyOf(devices) : List.of();
            schedules = schedules != null ? List.copyOf(schedules) : List.of();
        }

        public PhysicalWorld() {
            this(List.of(), List.of(), List.of());
        }
    }

    /**
     * 社会图谱
     */
    public record SocialGraph(
        List<OwnerModel.OwnerIdentity> people,
        List<OwnerModel.Relationship> relationships
    ) {
        public SocialGraph {
            people = people != null ? List.copyOf(people) : List.of();
            relationships = relationships != null ? List.copyOf(relationships) : List.of();
        }

        public SocialGraph() {
            this(List.of(), List.of());
        }
    }

    /**
     * 知识图谱
     */
    public record KnowledgeGraph(
        List<Fact> facts,
        List<OwnerModel.Belief> beliefs,
        List<Concept> concepts
    ) {
        public KnowledgeGraph {
            facts = facts != null ? List.copyOf(facts) : List.of();
            beliefs = beliefs != null ? List.copyOf(beliefs) : List.of();
            concepts = concepts != null ? List.copyOf(concepts) : List.of();
        }

        public KnowledgeGraph() {
            this(List.of(), List.of(), List.of());
        }
    }

    public record Fact(
        String id,
        String statement,
        String source,
        float confidence,
        Instant timestamp
    ) {
        public Fact {
            Objects.requireNonNull(id);
            Objects.requireNonNull(statement);
            Objects.requireNonNull(source);
            Objects.requireNonNull(timestamp);
        }
    }

    public record Concept(
        String id,
        String name,
        String description,
        List<String> examples,
        List<String> relatedConcepts
    ) {
        public Concept {
            examples = examples != null ? List.copyOf(examples) : List.of();
            relatedConcepts = relatedConcepts != null ? List.copyOf(relatedConcepts) : List.of();
        }
    }

    /**
     * 当前情境
     */
    public record Context(
        String location,
        Instant time,
        Activity activity,
        OwnerModel.EmotionalState emotionalState,
        List<String> recentEvents,
        float attention,
        float urgency
    ) {
        public Context {
            if (location == null) location = null;
            if (time == null) time = Instant.now();
            if (activity == null) activity = Activity.UNKNOWN;
            if (emotionalState == null) emotionalState = null;
            recentEvents = recentEvents != null ? List.copyOf(recentEvents) : List.of();
        }

        public Context(Instant time, Activity activity) {
            this(null, time, activity, null, List.of(), 1.0f, 0.0f);
        }
    }

    /**
     * 世界模型完整视图
     */
    public record World(
        OwnerModel.Owner owner,
        PhysicalWorld physicalWorld,
        SocialGraph socialGraph,
        KnowledgeGraph knowledgeGraph,
        Context currentContext
    ) {
        public World {
            if (physicalWorld == null) physicalWorld = new PhysicalWorld();
            if (socialGraph == null) socialGraph = new SocialGraph();
            if (knowledgeGraph == null) knowledgeGraph = new KnowledgeGraph();
            if (currentContext == null) currentContext = new Context(Instant.now(), Activity.UNKNOWN);
        }

        public World(OwnerModel.Owner owner) {
            this(owner, new PhysicalWorld(), new SocialGraph(), new KnowledgeGraph(), new Context(Instant.now(), Activity.UNKNOWN));
        }

        /**
         * 创建默认世界模型（使用 OwnerModel 默认主人）
         */
        public static World createDefault() {
            return new World(OwnerModel.Owner.createDefault());
        }

        /**
         * 添加主人明确表达的偏好
         */
        public World addExplicitPreference(String key, String value) {
            OwnerModel.Preference.Explicit pref = new OwnerModel.Preference.Explicit(key, value, Instant.now());
            OwnerModel.Owner newOwner = new OwnerModel.Owner(
                owner.identity(),
                owner.lifeContext(),
                owner.goals(),
                owner.beliefs(),
                owner.habits(),
                owner.emotionalState(),
                concat(owner.explicitPreferences(), List.of(pref)),
                owner.inferredPreferences(),
                owner.trustLevel(),
                owner.workStyle(),
                owner.communicationStyle(),
                owner.digitalFootprint(),
                owner.interactionHistory(),
                Instant.now()
            );
            return new World(newOwner, physicalWorld, socialGraph, knowledgeGraph, currentContext);
        }

        /**
         * 推断隐式偏好
         */
        public World inferPreference(String key, String value, List<String> evidence, float confidence) {
            OwnerModel.Preference.Inferred pref = new OwnerModel.Preference.Inferred(key, value, confidence, evidence, Instant.now());
            OwnerModel.Owner newOwner = new OwnerModel.Owner(
                owner.identity(),
                owner.lifeContext(),
                owner.goals(),
                owner.beliefs(),
                owner.habits(),
                owner.emotionalState(),
                owner.explicitPreferences(),
                concat(owner.inferredPreferences(), List.of(pref)),
                owner.trustLevel(),
                owner.workStyle(),
                owner.communicationStyle(),
                owner.digitalFootprint(),
                owner.interactionHistory(),
                Instant.now()
            );
            return new World(newOwner, physicalWorld, socialGraph, knowledgeGraph, currentContext);
        }

        /**
         * 记录交互
         */
        public World recordInteraction(OwnerModel.InteractionType type, String content, float sentiment, String topic) {
            OwnerModel.Interaction interaction = new OwnerModel.Interaction(Instant.now(), type, content, sentiment, topic);
            List<OwnerModel.Interaction> newHistory = new ArrayList<>(owner.interactionHistory());
            newHistory.add(interaction);
            OwnerModel.Owner newOwner = new OwnerModel.Owner(
                owner.identity(),
                owner.lifeContext(),
                owner.goals(),
                owner.beliefs(),
                owner.habits(),
                owner.emotionalState(),
                owner.explicitPreferences(),
                owner.inferredPreferences(),
                owner.trustLevel(),
                owner.workStyle(),
                owner.communicationStyle(),
                owner.digitalFootprint(),
                List.copyOf(newHistory),
                Instant.now()
            );
            return new World(newOwner, physicalWorld, socialGraph, knowledgeGraph, currentContext);
        }

        /**
         * 更新情感状态
         */
        public World updateEmotionalState(OwnerModel.Mood mood, float intensity, List<String> triggers) {
            OwnerModel.MoodEntry moodEntry = new OwnerModel.MoodEntry(Instant.now(), mood, intensity, triggers.isEmpty() ? null : triggers.get(0));
            OwnerModel.EmotionalState currentState = owner.emotionalState();
            if (currentState == null) {
                currentState = new OwnerModel.EmotionalState(mood, intensity, triggers);
            }
            List<OwnerModel.MoodEntry> recentMoods = new ArrayList<>(currentState.recentMoods());
            recentMoods.add(moodEntry);
            while (recentMoods.size() > 10) {
                recentMoods.remove(0);
            }
            OwnerModel.EmotionalState newState = new OwnerModel.EmotionalState(mood, intensity, triggers, List.copyOf(recentMoods), currentState.regulationStrategy());
            OwnerModel.Owner newOwner = new OwnerModel.Owner(
                owner.identity(),
                owner.lifeContext(),
                owner.goals(),
                owner.beliefs(),
                owner.habits(),
                newState,
                owner.explicitPreferences(),
                owner.inferredPreferences(),
                owner.trustLevel(),
                owner.workStyle(),
                owner.communicationStyle(),
                owner.digitalFootprint(),
                owner.interactionHistory(),
                Instant.now()
            );
            return new World(newOwner, physicalWorld, socialGraph, knowledgeGraph, currentContext);
        }

        /**
         * 添加信念
         */
        public World addBelief(String statement, float confidence, OwnerModel.BeliefSource source) {
            OwnerModel.Belief belief = new OwnerModel.Belief(java.util.UUID.randomUUID().toString(), statement, confidence, source, Instant.now());
            List<OwnerModel.Belief> newBeliefs = new ArrayList<>(owner.beliefs());
            newBeliefs.add(belief);
            OwnerModel.Owner newOwner = new OwnerModel.Owner(
                owner.identity(),
                owner.lifeContext(),
                owner.goals(),
                List.copyOf(newBeliefs),
                owner.habits(),
                owner.emotionalState(),
                owner.explicitPreferences(),
                owner.inferredPreferences(),
                owner.trustLevel(),
                owner.workStyle(),
                owner.communicationStyle(),
                owner.digitalFootprint(),
                owner.interactionHistory(),
                Instant.now()
            );
            return new World(newOwner, physicalWorld, socialGraph, knowledgeGraph, currentContext);
        }

        /**
         * 获取主人总结（用于提示词）
         */
        public String getOwnerSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 主人画像 ===\n");
            sb.append("名字: ").append(owner.identity().name()).append("\n\n");
            if (owner.identity() != null && owner.identity().occupation() != null) {
                sb.append("职业: ").append(owner.identity().occupation()).append("\n\n");
            }
            if (!owner.explicitPreferences().isEmpty()) {
                sb.append("明确偏好:\n");
                for (OwnerModel.Preference.Explicit p : owner.explicitPreferences()) {
                    sb.append("  - ").append(p.key()).append(": ").append(p.value()).append("\n");
                }
                sb.append("\n");
            }
            if (!owner.beliefs().isEmpty()) {
                sb.append("信念:\n");
                for (OwnerModel.Belief b : owner.beliefs().subList(0, Math.min(5, owner.beliefs().size()))) {
                    sb.append("  - ").append(b.statement()).append(" (置信度: ").append(b.confidence()).append(")\n");
                }
                sb.append("\n");
            }
            if (owner.communicationStyle() != null) {
                sb.append("沟通风格: ").append(owner.communicationStyle().tone())
                   .append(", ").append(owner.communicationStyle().verbosity().name().toLowerCase())
                   .append(", 格式: ").append(owner.communicationStyle().preferredFormat()).append("\n");
            }
            if (owner.workStyle() != null) {
                sb.append("工作风格: 高效时段 ").append(owner.workStyle().peakHours())
                   .append(", 方式: ").append(owner.workStyle().approach()).append("\n");
            }
            return sb.toString();
        }

        private static <T> List<T> concat(List<T> a, List<T> b) {
            List<T> result = new ArrayList<>(a);
            result.addAll(b);
            return List.copyOf(result);
        }
    }
}
