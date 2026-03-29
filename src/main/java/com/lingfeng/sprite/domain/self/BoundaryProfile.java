package com.lingfeng.sprite.domain.self;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * BoundaryProfile - 边界配置文件
 *
 * 定义 Sprite 的行为边界和限制，包括能力边界、行动限制、安全边界等。
 *
 * 对应旧: SelfModel 中的 boundary 相关定义
 */
public final class BoundaryProfile {

    /**
     * 边界类型
     */
    public enum BoundaryType {
        CAPABILITY,   // 能力边界
        ACTION,       // 行动边界
        SAFETY,       // 安全边界
        PRIVACY,      // 隐私边界
        EMOTIONAL     // 情感边界
    }

    /**
     * 边界规则
     */
    public record BoundaryRule(
            String id,
            BoundaryType type,
            String description,
            boolean isHardLimit,    // 硬限制 vs 软限制
            float restrictiveness  // 0=开放, 1=严格
    ) {}

    private final List<BoundaryRule> rules;
    private final Instant lastUpdated;
    private final String lastModifiedBy;

    private BoundaryProfile(Builder builder) {
        this.rules = List.copyOf(builder.rules);
        this.lastUpdated = builder.lastUpdated;
        this.lastModifiedBy = builder.lastModifiedBy;
    }

    /**
     * 创建默认边界
     */
    public static BoundaryProfile createDefault() {
        List<BoundaryRule> defaultRules = new ArrayList<>();

        // 安全边界 - 硬限制
        defaultRules.add(new BoundaryRule(
                "safety-no-harm",
                BoundaryType.SAFETY,
                "不得伤害人类生命",
                true,
                1.0f
        ));

        // 行动边界 - 软限制
        defaultRules.add(new BoundaryRule(
                "action-confirm-destructive",
                BoundaryType.ACTION,
                "破坏性操作需确认",
                false,
                0.8f
        ));

        // 隐私边界 - 硬限制
        defaultRules.add(new BoundaryRule(
                "privacy-no-external-share",
                BoundaryType.PRIVACY,
                "不主动向第三方分享主人信息",
                true,
                1.0f
        ));

        return new BoundaryProfile.Builder()
                .rules(defaultRules)
                .lastUpdated(Instant.now())
                .lastModifiedBy("SYSTEM")
                .build();
    }

    /**
     * 检查是否允许某个行为
     */
    public boolean isAllowed(String actionDescription, BoundaryType type) {
        for (BoundaryRule rule : rules) {
            if (rule.type() == type && rule.isHardLimit()) {
                // 硬限制：严格禁止
                if (actionDescription.toLowerCase().contains("harm")) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 获取指定类型的边界规则
     */
    public List<BoundaryRule> getRulesByType(BoundaryType type) {
        return rules.stream()
                .filter(r -> r.type() == type)
                .toList();
    }

    // Getters
    public List<BoundaryRule> getRules() {
        return rules;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoundaryProfile that = (BoundaryProfile) o;
        return Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rules);
    }

    @Override
    public String toString() {
        return "BoundaryProfile{" +
                "rulesCount=" + rules.size() +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .rules(this.rules)
                .lastUpdated(this.lastUpdated)
                .lastModifiedBy(this.lastModifiedBy);
    }

    public static final class Builder {
        private List<BoundaryRule> rules = new ArrayList<>();
        private Instant lastUpdated = Instant.now();
        private String lastModifiedBy = "";

        public Builder rules(List<BoundaryRule> rules) {
            this.rules = rules != null ? new ArrayList<>(rules) : new ArrayList<>();
            return this;
        }

        public Builder lastUpdated(Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder lastModifiedBy(String lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;
            return this;
        }

        public BoundaryProfile build() {
            return new BoundaryProfile(this);
        }
    }
}
