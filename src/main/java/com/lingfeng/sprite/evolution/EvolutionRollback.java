package com.lingfeng.sprite.evolution;

import java.time.Instant;
import java.util.Objects;

/**
 * EvolutionRollback - 进化回滚记录
 */
public final class EvolutionRollback {

    public enum RollbackStatus {
        INITIATED,   // 已发起
        IN_PROGRESS, // 执行中
        COMPLETED,   // 已完成
        FAILED       // 失败
    }

    private final String rollbackId;
    private final String releaseId;
    private final String reason;
    private final RollbackStatus status;
    private final Instant initiatedAt;
    private final Instant completedAt;
    private final String performedBy;
    private final String snapshotBefore;
    private final String snapshotAfter;

    private EvolutionRollback(Builder builder) {
        this.rollbackId = builder.rollbackId;
        this.releaseId = builder.releaseId;
        this.reason = builder.reason;
        this.status = builder.status;
        this.initiatedAt = builder.initiatedAt;
        this.completedAt = builder.completedAt;
        this.performedBy = builder.performedBy;
        this.snapshotBefore = builder.snapshotBefore;
        this.snapshotAfter = builder.snapshotAfter;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRollbackId() {
        return rollbackId;
    }

    public String getReleaseId() {
        return releaseId;
    }

    public String getReason() {
        return reason;
    }

    public RollbackStatus getStatus() {
        return status;
    }

    public Instant getInitiatedAt() {
        return initiatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public String getSnapshotBefore() {
        return snapshotBefore;
    }

    public String getSnapshotAfter() {
        return snapshotAfter;
    }

    public Builder with() {
        return new Builder()
                .rollbackId(this.rollbackId)
                .releaseId(this.releaseId)
                .reason(this.reason)
                .status(this.status)
                .initiatedAt(this.initiatedAt)
                .completedAt(this.completedAt)
                .performedBy(this.performedBy)
                .snapshotBefore(this.snapshotBefore)
                .snapshotAfter(this.snapshotAfter);
    }

    /**
     * Builder for EvolutionRollback
     */
    public static final class Builder {
        private String rollbackId;
        private String releaseId;
        private String reason;
        private RollbackStatus status;
        private Instant initiatedAt;
        private Instant completedAt;
        private String performedBy;
        private String snapshotBefore;
        private String snapshotAfter;

        private Builder() {}

        public Builder rollbackId(String rollbackId) {
            this.rollbackId = rollbackId;
            return this;
        }

        public Builder releaseId(String releaseId) {
            this.releaseId = releaseId;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder status(RollbackStatus status) {
            this.status = status;
            return this;
        }

        public Builder initiatedAt(Instant initiatedAt) {
            this.initiatedAt = initiatedAt;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder performedBy(String performedBy) {
            this.performedBy = performedBy;
            return this;
        }

        public Builder snapshotBefore(String snapshotBefore) {
            this.snapshotBefore = snapshotBefore;
            return this;
        }

        public Builder snapshotAfter(String snapshotAfter) {
            this.snapshotAfter = snapshotAfter;
            return this;
        }

        public EvolutionRollback build() {
            return new EvolutionRollback(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvolutionRollback that = (EvolutionRollback) o;
        return Objects.equals(rollbackId, that.rollbackId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rollbackId);
    }

    @Override
    public String toString() {
        return "EvolutionRollback{" +
                "rollbackId='" + rollbackId + '\'' +
                ", releaseId='" + releaseId + '\'' +
                ", status=" + status +
                ", initiatedAt=" + initiatedAt +
                '}';
    }
}
