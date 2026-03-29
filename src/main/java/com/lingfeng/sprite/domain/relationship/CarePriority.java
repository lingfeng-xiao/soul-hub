package com.lingfeng.sprite.domain.relationship;

import java.time.Instant;
import java.util.Objects;

/**
 * CarePriority - 关怀优先级
 *
 * 代表 Sprite 对主人的关怀和优先级配置。
 *
 * 对应旧: OwnerModel.OwnerIdentity 中的关怀相关概念
 */
public final class CarePriority {

    /**
     * 优先级级别
     */
    public enum PriorityLevel {
        CRITICAL,   // 紧急重要
        HIGH,       // 高优先级
        NORMAL,     // 普通优先级
        LOW         // 低优先级
    }

    /**
     * 关怀类型
     */
    public enum CareType {
        EMOTIONAL,      // 情感关怀
        PHYSICAL,       // 身体关怀
        PRODUCTIVITY,   // 效率关怀
        SAFETY,         // 安全关怀
        GROWTH          // 成长关怀
    }

    /**
     * 关怀类型
     */
    private final CareType careType;

    /**
     * 优先级级别
     */
    private final PriorityLevel level;

    /**
     * 优先级分数 (0-1, 越高越优先)
     */
    private final float score;

    /**
     * 是否启用
     */
    private final boolean enabled;

    /**
     * 最后检查时间
     */
    private final Instant lastCheckedAt;

    /**
     * 最后触发时间
     */
    private final Instant lastTriggeredAt;

    private CarePriority(Builder builder) {
        this.careType = builder.careType;
        this.level = builder.level;
        this.score = builder.score;
        this.enabled = builder.enabled;
        this.lastCheckedAt = builder.lastCheckedAt;
        this.lastTriggeredAt = builder.lastTriggeredAt;
    }

    /**
     * 创建默认情感关怀
     */
    public static CarePriority createEmotional() {
        return new CarePriority.Builder()
                .careType(CareType.EMOTIONAL)
                .level(PriorityLevel.HIGH)
                .score(0.8f)
                .enabled(true)
                .lastCheckedAt(Instant.now())
                .lastTriggeredAt(null)
                .build();
    }

    /**
     * 创建默认安全关怀
     */
    public static CarePriority createSafety() {
        return new CarePriority.Builder()
                .careType(CareType.SAFETY)
                .level(PriorityLevel.CRITICAL)
                .score(1.0f)
                .enabled(true)
                .lastCheckedAt(Instant.now())
                .lastTriggeredAt(null)
                .build();
    }

    /**
     * 创建默认效率关怀
     */
    public static CarePriority createProductivity() {
        return new CarePriority.Builder()
                .careType(CareType.PRODUCTIVITY)
                .level(PriorityLevel.NORMAL)
                .score(0.6f)
                .enabled(true)
                .lastCheckedAt(Instant.now())
                .lastTriggeredAt(null)
                .build();
    }

    /**
     * 创建默认成长关怀
     */
    public static CarePriority createGrowth() {
        return new CarePriority.Builder()
                .careType(CareType.GROWTH)
                .level(PriorityLevel.NORMAL)
                .score(0.5f)
                .enabled(true)
                .lastCheckedAt(Instant.now())
                .lastTriggeredAt(null)
                .build();
    }

    /**
     * 更新优先级
     */
    public CarePriority withLevel(PriorityLevel newLevel) {
        float newScore = calculateScore(newLevel, this.careType);
        return new CarePriority.Builder()
                .careType(this.careType)
                .level(newLevel)
                .score(newScore)
                .enabled(this.enabled)
                .lastCheckedAt(Instant.now())
                .lastTriggeredAt(this.lastTriggeredAt)
                .build();
    }

    /**
     * 触发关怀
     */
    public CarePriority trigger() {
        return new CarePriority.Builder()
                .careType(this.careType)
                .level(this.level)
                .score(this.score)
                .enabled(this.enabled)
                .lastCheckedAt(Instant.now())
                .lastTriggeredAt(Instant.now())
                .build();
    }

    /**
     * 禁用
     */
    public CarePriority disable() {
        return new CarePriority.Builder()
                .careType(this.careType)
                .level(this.level)
                .score(0f)
                .enabled(false)
                .lastCheckedAt(this.lastCheckedAt)
                .lastTriggeredAt(this.lastTriggeredAt)
                .build();
    }

    private float calculateScore(PriorityLevel level, CareType careType) {
        float baseScore = switch (level) {
            case CRITICAL -> 1.0f;
            case HIGH -> 0.8f;
            case NORMAL -> 0.5f;
            case LOW -> 0.2f;
        };

        // 根据关怀类型调整
        float typeMultiplier = switch (careType) {
            case SAFETY -> 1.2f;
            case EMOTIONAL -> 1.1f;
            case PHYSICAL, PRODUCTIVITY, GROWTH -> 1.0f;
        };

        return Math.min(1f, baseScore * typeMultiplier);
    }

    // Getters
    public CareType getCareType() {
        return careType;
    }

    public PriorityLevel getLevel() {
        return level;
    }

    public float getScore() {
        return score;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public Instant getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CarePriority that = (CarePriority) o;
        return careType == that.careType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(careType);
    }

    @Override
    public String toString() {
        return "CarePriority{" +
                "careType=" + careType +
                ", level=" + level +
                ", score=" + score +
                ", enabled=" + enabled +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .careType(this.careType)
                .level(this.level)
                .score(this.score)
                .enabled(this.enabled)
                .lastCheckedAt(this.lastCheckedAt)
                .lastTriggeredAt(this.lastTriggeredAt);
    }

    public static final class Builder {
        private CareType careType = CareType.EMOTIONAL;
        private PriorityLevel level = PriorityLevel.NORMAL;
        private float score = 0.5f;
        private boolean enabled = true;
        private Instant lastCheckedAt = Instant.now();
        private Instant lastTriggeredAt = null;

        public Builder careType(CareType careType) {
            this.careType = careType != null ? careType : CareType.EMOTIONAL;
            return this;
        }

        public Builder level(PriorityLevel level) {
            this.level = level != null ? level : PriorityLevel.NORMAL;
            return this;
        }

        public Builder score(float score) {
            this.score = Math.max(0f, Math.min(1f, score));
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder lastCheckedAt(Instant lastCheckedAt) {
            this.lastCheckedAt = lastCheckedAt != null ? lastCheckedAt : Instant.now();
            return this;
        }

        public Builder lastTriggeredAt(Instant lastTriggeredAt) {
            this.lastTriggeredAt = lastTriggeredAt;
            return this;
        }

        public CarePriority build() {
            return new CarePriority(this);
        }
    }
}
