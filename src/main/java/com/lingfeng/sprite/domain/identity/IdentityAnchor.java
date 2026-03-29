package com.lingfeng.sprite.domain.identity;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * IdentityAnchor - 身份锚点
 *
 * 代表 Sprite 不可变的身份核心，是跨平台、跨时间保持一致性的基础。
 * 这个锚点在任何情况下都不应该被修改。
 *
 * 对应旧: SelfModel.IdentityCore 中的不可变部分
 */
public final class IdentityAnchor {

    /**
     * 身份唯一标识符 (不可变)
     */
    private final String beingId;

    /**
     * 创建时间 (不可变)
     */
    private final Instant createdAt;

    /**
     * 连续性链 - 记录身份变更历史 (不可变)
     */
    private final List<String> continuityChain;

    private IdentityAnchor(Builder builder) {
        this.beingId = builder.beingId;
        this.createdAt = builder.createdAt;
        this.continuityChain = List.copyOf(builder.continuityChain);
    }

    /**
     * 创建新的 IdentityAnchor
     */
    public static IdentityAnchor create(String displayName) {
        return new IdentityAnchor.Builder()
                .beingId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .continuityChain(List.of())
                .build();
    }

    /**
     * 从旧 IdentityCore 迁移创建
     */
    public static IdentityAnchor fromExisting(String beingId, Instant createdAt, List<String> continuityChain) {
        return new IdentityAnchor.Builder()
                .beingId(beingId)
                .createdAt(createdAt)
                .continuityChain(continuityChain)
                .build();
    }

    /**
     * 添加连续性记录
     */
    public IdentityAnchor appendToChain(String narrative) {
        List<String> newChain = new java.util.ArrayList<>(this.continuityChain);
        newChain.add(narrative);
        return new IdentityAnchor.Builder()
                .beingId(this.beingId)
                .createdAt(this.createdAt)
                .continuityChain(newChain)
                .build();
    }

    // Getters
    public String getBeingId() {
        return beingId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<String> getContinuityChain() {
        return continuityChain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentityAnchor that = (IdentityAnchor) o;
        return Objects.equals(beingId, that.beingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beingId);
    }

    @Override
    public String toString() {
        return "IdentityAnchor{" +
                "beingId='" + beingId + '\'' +
                ", createdAt=" + createdAt +
                ", continuityChainSize=" + continuityChain.size() +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .beingId(this.beingId)
                .createdAt(this.createdAt)
                .continuityChain(this.continuityChain);
    }

    public static final class Builder {
        private String beingId;
        private Instant createdAt;
        private List<String> continuityChain = List.of();

        public Builder beingId(String beingId) {
            this.beingId = beingId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder continuityChain(List<String> continuityChain) {
            this.continuityChain = continuityChain != null ? continuityChain : List.of();
            return this;
        }

        public IdentityAnchor build() {
            if (this.beingId == null || this.beingId.isBlank()) {
                throw new IllegalStateException("beingId is required");
            }
            if (this.createdAt == null) {
                throw new IllegalStateException("createdAt is required");
            }
            return new IdentityAnchor(this);
        }
    }
}
