package com.lingfeng.sprite;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 主人模型 - 数字生命认识的"他者"中最重要的创造者
 *
 * 数字生命认识主人的一切，包括：
 * - 身份信息（姓名、职业、社会关系）
 * - 生活全貌（工作、家庭、作息）
 * - 心理模型（信念、目标、习惯、情感）
 * - 行为模式（工作风格、沟通偏好）
 * - 数字足迹（设备、应用、在线时段）
 */
public final class OwnerModel {

    private OwnerModel() {}

    // ==================== 主人身份 ====================

    /**
     * 主人身份
     */
    public record OwnerIdentity(
        String name,
        String occupation,
        List<Relationship> relationships
    ) {
        public OwnerIdentity {
            relationships = relationships != null ? List.copyOf(relationships) : List.of();
        }

        public OwnerIdentity(String name) {
            this(name, null, List.of());
        }
    }

    /**
     * 社会关系
     */
    public record Relationship(
        String personId,
        String name,
        RelationshipType type,
        float strength,
        String description,
        Instant lastInteraction
    ) {
        public Relationship {
            if (description == null) description = null;
            if (lastInteraction == null) lastInteraction = null;
        }

        public Relationship(String personId, String name, RelationshipType type, float strength) {
            this(personId, name, type, strength, null, null);
        }
    }

    public enum RelationshipType {
        FAMILY, FRIEND, COLLEAGUE, CLIENT, OTHER
    }

    // ==================== 生活上下文 ====================

    /**
     * 生活上下文
     */
    public record LifeContext(
        String workplace,
        String home,
        Family family,
        List<Schedule> schedules
    ) {
        public LifeContext {
            if (workplace == null) workplace = null;
            if (home == null) home = null;
            family = family != null ? family : new Family(List.of());
            schedules = schedules != null ? List.copyOf(schedules) : List.of();
        }
    }

    /**
     * 家庭成员
     */
    public record Family(
        List<FamilyMember> members
    ) {
        public Family {
            members = members != null ? List.copyOf(members) : List.of();
        }
    }

    public record FamilyMember(
        String name,
        String relation,
        String note
    ) {
        public FamilyMember {
            if (note == null) note = null;
        }
    }

    /**
     * 日程安排
     */
    public record Schedule(
        String id,
        String title,
        String time,
        int duration,
        String recurrence,
        String context
    ) {
        public Schedule {
            Objects.requireNonNull(id);
            Objects.requireNonNull(title);
            if (time == null) time = null;
            if (recurrence == null) recurrence = null;
            if (context == null) context = null;
        }
    }

    // ==================== 心理模型 ====================

    /**
     * 目标
     */
    public record Goal(
        String id,
        String title,
        String description,
        Priority priority,
        Instant deadline,
        float progress,
        GoalStatus status
    ) {
        public Goal {
            if (deadline == null) deadline = null;
        }

        public Goal(String id, String title, String description, Priority priority, GoalStatus status) {
            this(id, title, description, priority, null, 0f, status);
        }
    }

    public enum Priority { CRITICAL, HIGH, MEDIUM, LOW }
    public enum GoalStatus { ACTIVE, PAUSED, COMPLETED, ABANDONED }

    /**
     * 信念
     */
    public record Belief(
        String id,
        String statement,
        float confidence,
        BeliefSource source,
        Instant inferredAt
    ) {
        public Belief {
            Objects.requireNonNull(source);
            Objects.requireNonNull(inferredAt);
        }
    }

    public enum BeliefSource {
        EXPLICIT_STATED,
        OBSERVED_BEHAVIOR,
        DEDUCED,
        UNCERTAIN
    }

    /**
     * 习惯
     */
    public record Habit(
        String id,
        String trigger,
        String action,
        Frequency frequency,
        Instant lastOccurrence,
        int timesPerformed
    ) {
        public Habit {
            if (lastOccurrence == null) lastOccurrence = null;
        }

        public Habit(String id, String trigger, String action, Frequency frequency) {
            this(id, trigger, action, frequency, null, 0);
        }
    }

    public enum Frequency { ALWAYS, USUALLY, SOMETIMES, RARELY, UNKNOWN }

    // ==================== 情感状态 ====================

    public enum Mood {
        HAPPY, SAD, ANXIOUS, CALM, EXCITED, FRUSTRATED,
        GRATEFUL, CONFUSED, CONFIDENT, TIRED, NEUTRAL
    }

    public record EmotionalState(
        Mood currentMood,
        float intensity,
        List<String> triggers,
        List<MoodEntry> recentMoods,
        String regulationStrategy
    ) {
        public EmotionalState {
            triggers = triggers != null ? List.copyOf(triggers) : List.of();
            recentMoods = recentMoods != null ? List.copyOf(recentMoods) : List.of();
            if (regulationStrategy == null) regulationStrategy = null;
        }

        public EmotionalState(Mood currentMood, float intensity, List<String> triggers) {
            this(currentMood, intensity, triggers, List.of(), null);
        }
    }

    public record MoodEntry(
        Instant timestamp,
        Mood mood,
        float intensity,
        String trigger
    ) {
        public MoodEntry {
            if (trigger == null) trigger = null;
        }

        public MoodEntry(Instant timestamp, Mood mood, float intensity) {
            this(timestamp, mood, intensity, null);
        }
    }

    // ==================== 偏好 ====================

    /**
     * 偏好（密封接口）
     */
    public sealed interface Preference permits Preference.Explicit, Preference.Inferred {

        record Explicit(
            String key,
            String value,
            Instant statedAt,
            String source
        ) implements Preference {
            public Explicit {
                Objects.requireNonNull(key);
                Objects.requireNonNull(value);
                Objects.requireNonNull(statedAt);
                if (source == null) source = "owner";
            }

            public Explicit(String key, String value, Instant statedAt) {
                this(key, value, statedAt, "owner");
            }
        }

        record Inferred(
            String key,
            String value,
            float confidence,
            List<String> inferredFrom,
            Instant inferredAt
        ) implements Preference {
            public Inferred {
                Objects.requireNonNull(key);
                Objects.requireNonNull(value);
                Objects.requireNonNull(inferredFrom);
                inferredFrom = List.copyOf(inferredFrom);
                Objects.requireNonNull(inferredAt);
            }
        }
    }

    // ==================== 信任 ====================

    public record TrustLevel(
        float overall,
        java.util.Map<String, Float> aspects,
        List<TrustEvent> history
    ) {
        public TrustLevel {
            aspects = aspects != null ? java.util.Collections.unmodifiableMap(aspects) : java.util.Collections.emptyMap();
            history = history != null ? List.copyOf(history) : List.of();
        }

        public TrustLevel(float overall) {
            this(overall, java.util.Collections.emptyMap(), List.of());
        }
    }

    public record TrustEvent(
        Instant timestamp,
        String aspect,
        float delta,
        String reason
    ) {
        public TrustEvent {
            Objects.requireNonNull(reason);
        }
    }

    // ==================== 工作风格 ====================

    public record WorkStyle(
        List<Integer> peakHours,
        String approach,
        String breakPattern,
        String environment
    ) {
        public WorkStyle {
            peakHours = peakHours != null ? List.copyOf(peakHours) : List.of();
        }
    }

    // ==================== 沟通风格 ====================

    public enum Verbosity { BRIEF, MODERATE, DETAILED }

    public record CommunicationStyle(
        String tone,
        Verbosity verbosity,
        String preferredFormat,
        String language
    ) {
        public CommunicationStyle {
            if (language == null) language = "中文";
        }

        public CommunicationStyle(String tone, Verbosity verbosity, String preferredFormat) {
            this(tone, verbosity, preferredFormat, "中文");
        }
    }

    // ==================== 数字足迹 ====================

    /**
     * 数字足迹
     */
    public record DigitalFootprint(
        List<Device> devices,
        List<String> frequentApps,
        List<ActiveHour> activeHours
    ) {
        public DigitalFootprint {
            devices = devices != null ? List.copyOf(devices) : List.of();
            frequentApps = frequentApps != null ? List.copyOf(frequentApps) : List.of();
            activeHours = activeHours != null ? List.copyOf(activeHours) : List.of();
        }
    }

    public record Device(
        String deviceId,
        String name,
        DeviceType type,
        List<String> capabilities,
        Instant lastSeen
    ) {
        public Device {
            capabilities = capabilities != null ? List.copyOf(capabilities) : List.of();
        }
    }

    public enum DeviceType {
        PHONE, PC, CLOUD, TABLET, OTHER
    }

    public record ActiveHour(
        int hour,
        int dayOfWeek,
        float activityLevel
    ) {}

    // ==================== 主人完整模型 ====================

    /**
     * 主人完整模型
     */
    public record Owner(
        // 身份
        OwnerIdentity identity,

        // 生活上下文
        LifeContext lifeContext,

        // 心理模型
        List<Goal> goals,
        List<Belief> beliefs,
        List<Habit> habits,

        // 情感
        EmotionalState emotionalState,

        // 偏好
        List<Preference.Explicit> explicitPreferences,
        List<Preference.Inferred> inferredPreferences,

        // 信任
        TrustLevel trustLevel,

        // 风格
        WorkStyle workStyle,
        CommunicationStyle communicationStyle,

        // 数字足迹
        DigitalFootprint digitalFootprint,

        // 交互历史
        List<Interaction> interactionHistory,

        // 元数据
        Instant lastUpdated
    ) {
        public Owner {
            goals = goals != null ? List.copyOf(goals) : List.of();
            beliefs = beliefs != null ? List.copyOf(beliefs) : List.of();
            habits = habits != null ? List.copyOf(habits) : List.of();
            explicitPreferences = explicitPreferences != null ? List.copyOf(explicitPreferences) : List.of();
            inferredPreferences = inferredPreferences != null ? List.copyOf(inferredPreferences) : List.of();
            if (workStyle == null) workStyle = null;
            if (communicationStyle == null) communicationStyle = null;
            if (digitalFootprint == null) digitalFootprint = new DigitalFootprint(List.of(), List.of(), List.of());
            interactionHistory = interactionHistory != null ? List.copyOf(interactionHistory) : List.of();
            if (lastUpdated == null) lastUpdated = Instant.now();
        }

        public Owner(OwnerIdentity identity) {
            this(
                identity,
                new LifeContext(null, null, new Family(List.of()), List.of()),
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                new TrustLevel(0.5f),
                null,
                null,
                new DigitalFootprint(List.of(), List.of(), List.of()),
                List.of(),
                Instant.now()
            );
        }

        /**
         * 创建默认主人（名字为灵锋）
         */
        public static Owner createDefault() {
            return new Owner(new OwnerIdentity("灵锋"));
        }
    }

    // ==================== 交互记录 ====================

    public enum InteractionType {
        REQUEST, FEEDBACK, CASUAL, QUESTION, COMPLAINT, PRAISE, QUESTION_REJECT, PROACTIVE_REPLY, PROACTIVE_IGNORE, PROACTIVE_REJECT
    }

    public record Interaction(
        Instant timestamp,
        InteractionType type,
        String content,
        float sentiment,
        String topic,
        String outcome,
        String digitalBeingReaction
    ) {
        public Interaction {
            if (topic == null) topic = null;
            if (outcome == null) outcome = null;
            if (digitalBeingReaction == null) digitalBeingReaction = null;
        }

        public Interaction(Instant timestamp, InteractionType type, String content, float sentiment) {
            this(timestamp, type, content, sentiment, null, null, null);
        }

        public Interaction(Instant timestamp, InteractionType type, String content, float sentiment, String topic) {
            this(timestamp, type, content, sentiment, topic, null, null);
        }
    }

    // ==================== 主动消息反馈追踪 ====================

    /**
     * 主动消息反馈追踪
     */
    public record ProactiveFeedback(
        String messageId,
        Instant sentTime,
        String triggerType,
        String content,
        ResponseType response,
        Instant responseTime,
        String responseContent,
        float sentiment
    ) {
        public enum ResponseType {
            REPLY,       // 主人回复了消息
            IGNORE,      // 主人无响应（超时）
            REJECT,     // 主人明确拒绝/负面反馈
            POSITIVE,   // 主人积极响应
            NEUTRAL     // 中性响应
        }

        public ProactiveFeedback {
            if (response == null) response = ResponseType.IGNORE;
            if (sentiment == 0f && response != ResponseType.IGNORE) sentiment = 0.5f;
        }
    }
}
