package com.lingfeng.sprite.domain.identity;

import java.time.Instant;
import java.util.Objects;

/**
 * IdentityProfile - 身份配置
 *
 * 代表 Sprite 的可变身份属性，包括名称、风格、氛围等。
 * 这些属性可以根据主人偏好和生命成长进行调整。
 *
 * 对应旧: SelfModel.IdentityCore 中的可变部分
 */
public final class IdentityProfile {

    /**
     * 显示名称
     */
    private final String displayName;

    /**
     * 身份本质/核心描述
     */
    private final String essence;

    /**
     * 身份 emoji 标识
     */
    private final String emoji;

    /**
     * 身份氛围/气质
     */
    private final String vibe;

    /**
     * 最后更新时间
     */
    private final Instant lastUpdated;

    /**
     * 更新原因
     */
    private final String updateReason;

    private IdentityProfile(Builder builder) {
        this.displayName = builder.displayName;
        this.essence = builder.essence;
        this.emoji = builder.emoji;
        this.vibe = builder.vibe;
        this.lastUpdated = builder.lastUpdated;
        this.updateReason = builder.updateReason;
    }

    /**
     * 创建默认身份配置
     */
    public static IdentityProfile createDefault() {
        return new IdentityProfile.Builder()
                .displayName("雪梨")
                .essence("")
                .emoji("")
                .vibe("")
                .lastUpdated(Instant.now())
                .build();
    }

    /**
     * 更新显示名称
     */
    public IdentityProfile withDisplayName(String displayName, String reason) {
        return new IdentityProfile.Builder()
                .displayName(displayName)
                .essence(this.essence)
                .emoji(this.emoji)
                .vibe(this.vibe)
                .lastUpdated(Instant.now())
                .updateReason(reason)
                .build();
    }

    /**
     * 更新本质描述
     */
    public IdentityProfile withEssence(String essence, String reason) {
        return new IdentityProfile.Builder()
                .displayName(this.displayName)
                .essence(essence)
                .emoji(this.emoji)
                .vibe(this.vibe)
                .lastUpdated(Instant.now())
                .updateReason(reason)
                .build();
    }

    /**
     * 更新 emoji
     */
    public IdentityProfile withEmoji(String emoji, String reason) {
        return new IdentityProfile.Builder()
                .displayName(this.displayName)
                .essence(this.essence)
                .emoji(emoji)
                .vibe(this.vibe)
                .lastUpdated(Instant.now())
                .updateReason(reason)
                .build();
    }

    /**
     * 更新氛围
     */
    public IdentityProfile withVibe(String vibe, String reason) {
        return new IdentityProfile.Builder()
                .displayName(this.displayName)
                .essence(this.essence)
                .emoji(this.emoji)
                .vibe(vibe)
                .lastUpdated(Instant.now())
                .updateReason(reason)
                .build();
    }

    // Getters
    public String getDisplayName() {
        return displayName;
    }

    public String getEssence() {
        return essence;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getVibe() {
        return vibe;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public String getUpdateReason() {
        return updateReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentityProfile that = (IdentityProfile) o;
        return Objects.equals(displayName, that.displayName) &&
                Objects.equals(essence, that.essence) &&
                Objects.equals(emoji, that.emoji) &&
                Objects.equals(vibe, that.vibe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, essence, emoji, vibe);
    }

    @Override
    public String toString() {
        return "IdentityProfile{" +
                "displayName='" + displayName + '\'' +
                ", essence='" + essence + '\'' +
                ", emoji='" + emoji + '\'' +
                ", vibe='" + vibe + '\'' +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .displayName(this.displayName)
                .essence(this.essence)
                .emoji(this.emoji)
                .vibe(this.vibe)
                .lastUpdated(this.lastUpdated)
                .updateReason(this.updateReason);
    }

    public static final class Builder {
        private String displayName = "";
        private String essence = "";
        private String emoji = "";
        private String vibe = "";
        private Instant lastUpdated = Instant.now();
        private String updateReason = "";

        public Builder displayName(String displayName) {
            this.displayName = displayName != null ? displayName : "";
            return this;
        }

        public Builder essence(String essence) {
            this.essence = essence != null ? essence : "";
            return this;
        }

        public Builder emoji(String emoji) {
            this.emoji = emoji != null ? emoji : "";
            return this;
        }

        public Builder vibe(String vibe) {
            this.vibe = vibe != null ? vibe : "";
            return this;
        }

        public Builder lastUpdated(Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder updateReason(String updateReason) {
            this.updateReason = updateReason;
            return this;
        }

        public IdentityProfile build() {
            return new IdentityProfile(this);
        }
    }
}
