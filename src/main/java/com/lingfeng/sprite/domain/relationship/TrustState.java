package com.lingfeng.sprite.domain.relationship;

import java.time.Instant;
import java.util.Objects;

/**
 * TrustState - 信任状态
 *
 * 代表 Sprite 对主人的信任状态。
 *
 * 对应旧: WorldModel.SocialGraph 中的信任相关概念
 */
public final class TrustState {

    /**
     * 信任等级
     */
    public enum TrustLevel {
        LOW,      // 低信任
        MEDIUM,   // 中信任
        HIGH,     // 高信任
        FULL      // 完全信任
    }

    /**
     * 信任等级
     */
    private final TrustLevel level;

    /**
     * 信任分数 (0-1)
     */
    private final float score;

    /**
     * 信任建立时间
     */
    private final Instant establishedAt;

    /**
     * 最后验证时间
     */
    private final Instant lastVerifiedAt;

    /**
     * 信任验证次数
     */
    private final int verificationCount;

    /**
     * 信任破裂次数
     */
    private final int betrayalCount;

    private TrustState(Builder builder) {
        this.level = builder.level;
        this.score = builder.score;
        this.establishedAt = builder.establishedAt;
        this.lastVerifiedAt = builder.lastVerifiedAt;
        this.verificationCount = builder.verificationCount;
        this.betrayalCount = builder.betrayalCount;
    }

    /**
     * 创建默认信任
     */
    public static TrustState createDefault() {
        return new TrustState.Builder()
                .level(TrustLevel.MEDIUM)
                .score(0.5f)
                .establishedAt(Instant.now())
                .lastVerifiedAt(Instant.now())
                .verificationCount(0)
                .betrayalCount(0)
                .build();
    }

    /**
     * 增加信任
     */
    public TrustState increaseTrust(float amount) {
        float newScore = Math.min(1f, this.score + amount);
        TrustLevel newLevel = calculateLevel(newScore);

        return new TrustState.Builder()
                .level(newLevel)
                .score(newScore)
                .establishedAt(this.establishedAt)
                .lastVerifiedAt(Instant.now())
                .verificationCount(this.verificationCount + 1)
                .betrayalCount(this.betrayalCount)
                .build();
    }

    /**
     * 减少信任
     */
    public TrustState decreaseTrust(float amount) {
        float newScore = Math.max(0f, this.score - amount);
        TrustLevel newLevel = calculateLevel(newScore);

        return new TrustState.Builder()
                .level(newLevel)
                .score(newScore)
                .establishedAt(this.establishedAt)
                .lastVerifiedAt(Instant.now())
                .verificationCount(this.verificationCount)
                .betrayalCount(amount > 0.1f ? this.betrayalCount + 1 : this.betrayalCount)
                .build();
    }

    private TrustLevel calculateLevel(float score) {
        if (score >= 0.9f) return TrustLevel.FULL;
        if (score >= 0.7f) return TrustLevel.HIGH;
        if (score >= 0.4f) return TrustLevel.MEDIUM;
        return TrustLevel.LOW;
    }

    // Getters
    public TrustLevel getLevel() {
        return level;
    }

    public float getScore() {
        return score;
    }

    public Instant getEstablishedAt() {
        return establishedAt;
    }

    public Instant getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public int getVerificationCount() {
        return verificationCount;
    }

    public int getBetrayalCount() {
        return betrayalCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrustState that = (TrustState) o;
        return Float.compare(that.score, score) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(score);
    }

    @Override
    public String toString() {
        return "TrustState{" +
                "level=" + level +
                ", score=" + score +
                ", verificationCount=" + verificationCount +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .level(this.level)
                .score(this.score)
                .establishedAt(this.establishedAt)
                .lastVerifiedAt(this.lastVerifiedAt)
                .verificationCount(this.verificationCount)
                .betrayalCount(this.betrayalCount);
    }

    public static final class Builder {
        private TrustLevel level = TrustLevel.MEDIUM;
        private float score = 0.5f;
        private Instant establishedAt = Instant.now();
        private Instant lastVerifiedAt = Instant.now();
        private int verificationCount = 0;
        private int betrayalCount = 0;

        public Builder level(TrustLevel level) {
            this.level = level;
            return this;
        }

        public Builder score(float score) {
            this.score = Math.max(0f, Math.min(1f, score));
            return this;
        }

        public Builder establishedAt(Instant establishedAt) {
            this.establishedAt = establishedAt;
            return this;
        }

        public Builder lastVerifiedAt(Instant lastVerifiedAt) {
            this.lastVerifiedAt = lastVerifiedAt;
            return this;
        }

        public Builder verificationCount(int verificationCount) {
            this.verificationCount = verificationCount;
            return this;
        }

        public Builder betrayalCount(int betrayalCount) {
            this.betrayalCount = betrayalCount;
            return this;
        }

        public TrustState build() {
            return new TrustState(this);
        }
    }
}
