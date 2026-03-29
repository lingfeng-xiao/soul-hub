package com.lingfeng.sprite.evolution;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * GrayRelease - 灰度发布
 */
public final class GrayRelease {

    public enum GrayStrategy {
        PERCENTAGE,   // 百分比灰度
        USER_GROUP,   // 用户组灰度
        TRIGGER_TYPE, // 按触发类型灰度
        MEMORY_TYPE,  // 按记忆类型灰度
        TEST_MODE     // 测试模式
    }

    private final String releaseId;
    private final String proposalId;
    private final GrayStrategy strategy;
    private final Map<String, Object> config;
    private final EvolutionRelease.ReleaseStatus status;
    private final Instant startedAt;
    private final Instant endedAt;

    private GrayRelease(Builder builder) {
        this.releaseId = builder.releaseId;
        this.proposalId = builder.proposalId;
        this.strategy = builder.strategy;
        this.config = builder.config;
        this.status = builder.status;
        this.startedAt = builder.startedAt;
        this.endedAt = builder.endedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getReleaseId() {
        return releaseId;
    }

    public String getProposalId() {
        return proposalId;
    }

    public GrayStrategy getStrategy() {
        return strategy;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public EvolutionRelease.ReleaseStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Builder with() {
        return new Builder()
                .releaseId(this.releaseId)
                .proposalId(this.proposalId)
                .strategy(this.strategy)
                .config(this.config)
                .status(this.status)
                .startedAt(this.startedAt)
                .endedAt(this.endedAt);
    }

    /**
     * Builder for GrayRelease
     */
    public static final class Builder {
        private String releaseId;
        private String proposalId;
        private GrayStrategy strategy;
        private Map<String, Object> config;
        private EvolutionRelease.ReleaseStatus status;
        private Instant startedAt;
        private Instant endedAt;

        private Builder() {}

        public Builder releaseId(String releaseId) {
            this.releaseId = releaseId;
            return this;
        }

        public Builder proposalId(String proposalId) {
            this.proposalId = proposalId;
            return this;
        }

        public Builder strategy(GrayStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder config(Map<String, Object> config) {
            this.config = config;
            return this;
        }

        public Builder status(EvolutionRelease.ReleaseStatus status) {
            this.status = status;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder endedAt(Instant endedAt) {
            this.endedAt = endedAt;
            return this;
        }

        public GrayRelease build() {
            return new GrayRelease(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GrayRelease that = (GrayRelease) o;
        return Objects.equals(releaseId, that.releaseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(releaseId);
    }

    @Override
    public String toString() {
        return "GrayRelease{" +
                "releaseId='" + releaseId + '\'' +
                ", proposalId='" + proposalId + '\'' +
                ", strategy=" + strategy +
                ", status=" + status +
                ", startedAt=" + startedAt +
                '}';
    }
}
