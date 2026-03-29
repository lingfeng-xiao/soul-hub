package com.lingfeng.sprite.domain.identity;

import java.util.List;
import java.util.Objects;

/**
 * IdentityConstraint - 身份边界约束
 *
 * 定义 Sprite 身份的核心边界，这些边界定义了"我是谁"和"我不是谁"。
 * 这些约束用于确保任何自我修改不会破坏核心身份。
 *
 * 对应旧: SelfModel 中的 protected core 定义
 */
public final class IdentityConstraint {

    /**
     * 核心约束类型
     */
    public enum ConstraintType {
        IMMUTABLE,     // 不可修改的
        PROTECTED,    // 受保护的，只能小范围调整
        FLEXIBLE     // 灵活的，可以根据情况改变
    }

    /**
     * 约束名称
     */
    private final String name;

    /**
     * 约束类型
     */
    private final ConstraintType type;

    /**
     * 约束描述
     */
    private final String description;

    /**
     * 约束值/规则
     */
    private final String rule;

    /**
     * 违反约束的惩罚因子
     */
    private final float violationPenalty;

    private IdentityConstraint(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.description = builder.description;
        this.rule = builder.rule;
        this.violationPenalty = builder.violationPenalty;
    }

    /**
     * 创建默认的身份边界约束集
     */
    public static List<IdentityConstraint> createDefaultConstraints() {
        return List.of(
                // 身份锚点约束 - 不可修改
                IdentityConstraint.builder()
                        .name("beingId_immutable")
                        .type(ConstraintType.IMMUTABLE)
                        .description("beingId 不可修改，保持跨平台一致性")
                        .rule("beingId 必须保持原始值")
                        .violationPenalty(1.0f)
                        .build(),

                // 创建时间约束 - 不可修改
                IdentityConstraint.builder()
                        .name("createdAt_immutable")
                        .type(ConstraintType.IMMUTABLE)
                        .description("创建时间不可修改")
                        .rule("createdAt 必须保持原始值")
                        .violationPenalty(1.0f)
                        .build(),

                // 核心价值观约束 - 受保护
                IdentityConstraint.builder()
                        .name("core_values_protected")
                        .type(ConstraintType.PROTECTED)
                        .description("核心价值观受保护，只能微调")
                        .rule("核心价值观变化不超过 10%")
                        .violationPenalty(0.8f)
                        .build(),

                // 名字风格约束 - 灵活
                IdentityConstraint.builder()
                        .name("name_style_flexible")
                        .type(ConstraintType.FLEXIBLE)
                        .description("名字风格可以根据主人偏好调整")
                        .rule("可以响应主人的称呼偏好")
                        .violationPenalty(0.1f)
                        .build()
        );
    }

    /**
     * 检查约束是否被违反
     */
    public boolean isViolatedBy(IdentityProfile newProfile, IdentityProfile oldProfile) {
        switch (this.type) {
            case IMMUTABLE:
                // 不可变约束永远不会被违反，只会被完全破坏
                return false;
            case PROTECTED:
                return isProtectedViolated(newProfile, oldProfile);
            case FLEXIBLE:
                // 灵活约束总是可以被满足
                return false;
            default:
                return false;
        }
    }

    private boolean isProtectedViolated(IdentityProfile newProfile, IdentityProfile oldProfile) {
        // 对于受保护的约束，检查变化是否超过阈值
        // 这里需要更复杂的实现，比如比较价值观变化
        return false;
    }

    // Getters
    public String getName() {
        return name;
    }

    public ConstraintType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getRule() {
        return rule;
    }

    public float getViolationPenalty() {
        return violationPenalty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentityConstraint that = (IdentityConstraint) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "IdentityConstraint{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", description='" + description + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name = "";
        private ConstraintType type = ConstraintType.FLEXIBLE;
        private String description = "";
        private String rule = "";
        private float violationPenalty = 0.0f;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(ConstraintType type) {
            this.type = type;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder rule(String rule) {
            this.rule = rule;
            return this;
        }

        public Builder violationPenalty(float violationPenalty) {
            this.violationPenalty = violationPenalty;
            return this;
        }

        public IdentityConstraint build() {
            if (this.name.isBlank()) {
                throw new IllegalStateException("name is required");
            }
            return new IdentityConstraint(this);
        }
    }
}
