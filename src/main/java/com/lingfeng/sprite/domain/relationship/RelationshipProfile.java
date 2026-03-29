package com.lingfeng.sprite.domain.relationship;

import java.time.Instant;
import java.util.Objects;

/**
 * RelationshipProfile - 关系配置
 *
 * 代表 Sprite 与主人之间关系的配置信息。
 *
 * 对应旧: OwnerModel.OwnerIdentity 和 Relationship
 */
public final class RelationshipProfile {

    /**
     * 关系类型
     */
    public enum RelationshipType {
        FAMILY,      // 家人
        FRIEND,      // 朋友
        COLLEAGUE,   // 同事
        CLIENT,      // 客户
        PARTNER,     // 伙伴
        OTHER        // 其他
    }

    /**
     * 关系 ID
     */
    private final String relationshipId;

    /**
     * 关系类型
     */
    private final RelationshipType type;

    /**
     * 关系强度 (0-1)
     */
    private final float strength;

    /**
     * 关系描述
     */
    private final String description;

    /**
     * 建立时间
     */
    private final Instant establishedAt;

    /**
     * 最后交互时间
     */
    private final Instant lastInteractionAt;

    /**
     * 交互次数
     */
    private final int interactionCount;

    private RelationshipProfile(Builder builder) {
        this.relationshipId = builder.relationshipId;
        this.type = builder.type;
        this.strength = builder.strength;
        this.description = builder.description;
        this.establishedAt = builder.establishedAt;
        this.lastInteractionAt = builder.lastInteractionAt;
        this.interactionCount = builder.interactionCount;
    }

    /**
     * 创建默认关系
     */
    public static RelationshipProfile createDefault(String ownerId) {
        return new RelationshipProfile.Builder()
                .relationshipId(ownerId)
                .type(RelationshipType.FRIEND)
                .strength(0.5f)
                .description("初始关系")
                .establishedAt(Instant.now())
                .lastInteractionAt(Instant.now())
                .interactionCount(0)
                .build();
    }

    /**
     * 更新关系强度
     */
    public RelationshipProfile withStrength(float newStrength) {
        return new RelationshipProfile.Builder()
                .relationshipId(this.relationshipId)
                .type(this.type)
                .strength(Math.max(0f, Math.min(1f, newStrength)))
                .description(this.description)
                .establishedAt(this.establishedAt)
                .lastInteractionAt(Instant.now())
                .interactionCount(this.interactionCount + 1)
                .build();
    }

    /**
     * 记录交互
     */
    public RelationshipProfile recordInteraction() {
        return new RelationshipProfile.Builder()
                .relationshipId(this.relationshipId)
                .type(this.type)
                .strength(Math.min(1f, this.strength + 0.01f)) // 每次交互增加 1%
                .description(this.description)
                .establishedAt(this.establishedAt)
                .lastInteractionAt(Instant.now())
                .interactionCount(this.interactionCount + 1)
                .build();
    }

    // Getters
    public String getRelationshipId() {
        return relationshipId;
    }

    public RelationshipType getType() {
        return type;
    }

    public float getStrength() {
        return strength;
    }

    public String getDescription() {
        return description;
    }

    public Instant getEstablishedAt() {
        return establishedAt;
    }

    public Instant getLastInteractionAt() {
        return lastInteractionAt;
    }

    public int getInteractionCount() {
        return interactionCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationshipProfile that = (RelationshipProfile) o;
        return Objects.equals(relationshipId, that.relationshipId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationshipId);
    }

    @Override
    public String toString() {
        return "RelationshipProfile{" +
                "relationshipId='" + relationshipId + '\'' +
                ", type=" + type +
                ", strength=" + strength +
                ", interactionCount=" + interactionCount +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .relationshipId(this.relationshipId)
                .type(this.type)
                .strength(this.strength)
                .description(this.description)
                .establishedAt(this.establishedAt)
                .lastInteractionAt(this.lastInteractionAt)
                .interactionCount(this.interactionCount);
    }

    public static final class Builder {
        private String relationshipId = "";
        private RelationshipType type = RelationshipType.FRIEND;
        private float strength = 0.5f;
        private String description = "";
        private Instant establishedAt = Instant.now();
        private Instant lastInteractionAt = Instant.now();
        private int interactionCount = 0;

        public Builder relationshipId(String relationshipId) {
            this.relationshipId = relationshipId;
            return this;
        }

        public Builder type(RelationshipType type) {
            this.type = type;
            return this;
        }

        public Builder strength(float strength) {
            this.strength = Math.max(0f, Math.min(1f, strength));
            return this;
        }

        public Builder description(String description) {
            this.description = description != null ? description : "";
            return this;
        }

        public Builder establishedAt(Instant establishedAt) {
            this.establishedAt = establishedAt;
            return this;
        }

        public Builder lastInteractionAt(Instant lastInteractionAt) {
            this.lastInteractionAt = lastInteractionAt;
            return this;
        }

        public Builder interactionCount(int interactionCount) {
            this.interactionCount = interactionCount;
            return this;
        }

        public RelationshipProfile build() {
            if (this.relationshipId.isBlank()) {
                throw new IllegalStateException("relationshipId is required");
            }
            return new RelationshipProfile(this);
        }
    }
}
