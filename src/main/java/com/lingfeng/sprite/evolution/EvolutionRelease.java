package com.lingfeng.sprite.evolution;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * EvolutionRelease - 进化发布记录
 */
public final class EvolutionRelease {

    public enum ReleaseType {
        GRAY,   // 灰度发布
        FULL,   // 全量发布
        HOTFIX  // 热修复
    }

    public enum ReleaseStatus {
        PENDING,    // 待发布
        RELEASING,  // 发布中
        MONITORING, // 监控中
        PROMOTED,   // 已升级
        ROLLED_BACK // 已回滚
    }

    private final String releaseId;
    private final String proposalId;
    private final String version;
    private final ReleaseType type;
    private final String patchContent;
    private final Map<String, Object> metrics;
    private final Instant releasedAt;
    private final String releasedBy;
    private final ReleaseStatus status;
    private final String rollbackPoint;

    private EvolutionRelease(Builder builder) {
        this.releaseId = builder.releaseId;
        this.proposalId = builder.proposalId;
        this.version = builder.version;
        this.type = builder.type;
        this.patchContent = builder.patchContent;
        this.metrics = builder.metrics;
        this.releasedAt = builder.releasedAt;
        this.releasedBy = builder.releasedBy;
        this.status = builder.status;
        this.rollbackPoint = builder.rollbackPoint;
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

    public String getVersion() {
        return version;
    }

    public ReleaseType getType() {
        return type;
    }

    public String getPatchContent() {
        return patchContent;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public String getReleasedBy() {
        return releasedBy;
    }

    public ReleaseStatus getStatus() {
        return status;
    }

    public String getRollbackPoint() {
        return rollbackPoint;
    }

    public Builder with() {
        return new Builder()
                .releaseId(this.releaseId)
                .proposalId(this.proposalId)
                .version(this.version)
                .type(this.type)
                .patchContent(this.patchContent)
                .metrics(this.metrics)
                .releasedAt(this.releasedAt)
                .releasedBy(this.releasedBy)
                .status(this.status)
                .rollbackPoint(this.rollbackPoint);
    }

    /**
     * Builder for EvolutionRelease
     */
    public static final class Builder {
        private String releaseId;
        private String proposalId;
        private String version;
        private ReleaseType type;
        private String patchContent;
        private Map<String, Object> metrics;
        private Instant releasedAt;
        private String releasedBy;
        private ReleaseStatus status;
        private String rollbackPoint;

        private Builder() {}

        public Builder releaseId(String releaseId) {
            this.releaseId = releaseId;
            return this;
        }

        public Builder proposalId(String proposalId) {
            this.proposalId = proposalId;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder type(ReleaseType type) {
            this.type = type;
            return this;
        }

        public Builder patchContent(String patchContent) {
            this.patchContent = patchContent;
            return this;
        }

        public Builder metrics(Map<String, Object> metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder releasedAt(Instant releasedAt) {
            this.releasedAt = releasedAt;
            return this;
        }

        public Builder releasedBy(String releasedBy) {
            this.releasedBy = releasedBy;
            return this;
        }

        public Builder status(ReleaseStatus status) {
            this.status = status;
            return this;
        }

        public Builder rollbackPoint(String rollbackPoint) {
            this.rollbackPoint = rollbackPoint;
            return this;
        }

        public EvolutionRelease build() {
            return new EvolutionRelease(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvolutionRelease that = (EvolutionRelease) o;
        return Objects.equals(releaseId, that.releaseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(releaseId);
    }

    @Override
    public String toString() {
        return "EvolutionRelease{" +
                "releaseId='" + releaseId + '\'' +
                ", proposalId='" + proposalId + '\'' +
                ", version='" + version + '\'' +
                ", type=" + type +
                ", status=" + status +
                '}';
    }
}
