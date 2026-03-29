package com.lingfeng.sprite.domain.relationship;

import java.time.Instant;
import java.util.Objects;

/**
 * SharedProjectLink - 共同项目链接
 *
 * 代表 Sprite 与主人之间共同参与的项目或任务。
 *
 * 对应旧: WorldModel.SocialGraph 中的共同活动概念
 */
public final class SharedProjectLink {

    /**
     * 项目状态
     */
    public enum ProjectStatus {
        ACTIVE,     // 进行中
        COMPLETED,  // 已完成
        ABANDONED,  // 已放弃
        SUSPENDED   // 已暂停
    }

    /**
     * 项目 ID
     */
    private final String projectId;

    /**
     * 项目名称
     */
    private final String name;

    /**
     * 项目描述
     */
    private final String description;

    /**
     * 项目状态
     */
    private final ProjectStatus status;

    /**
     * 参与度 (0-1)
     */
    private final float engagement;

    /**
     * 创建时间
     */
    private final Instant createdAt;

    /**
     * 最后更新时间
     */
    private final Instant lastUpdatedAt;

    /**
     * 完成时间
     */
    private final Instant completedAt;

    private SharedProjectLink(Builder builder) {
        this.projectId = builder.projectId;
        this.name = builder.name;
        this.description = builder.description;
        this.status = builder.status;
        this.engagement = builder.engagement;
        this.createdAt = builder.createdAt;
        this.lastUpdatedAt = builder.lastUpdatedAt;
        this.completedAt = builder.completedAt;
    }

    /**
     * 创建共同项目
     */
    public static SharedProjectLink create(String projectId, String name, String description) {
        return new SharedProjectLink.Builder()
                .projectId(projectId)
                .name(name)
                .description(description)
                .status(ProjectStatus.ACTIVE)
                .engagement(0.5f)
                .createdAt(Instant.now())
                .lastUpdatedAt(Instant.now())
                .completedAt(null)
                .build();
    }

    /**
     * 更新参与度
     */
    public SharedProjectLink withEngagement(float newEngagement) {
        return new SharedProjectLink.Builder()
                .projectId(this.projectId)
                .name(this.name)
                .description(this.description)
                .status(this.status)
                .engagement(Math.max(0f, Math.min(1f, newEngagement)))
                .createdAt(this.createdAt)
                .lastUpdatedAt(Instant.now())
                .completedAt(this.completedAt)
                .build();
    }

    /**
     * 标记为完成
     */
    public SharedProjectLink markCompleted() {
        return new SharedProjectLink.Builder()
                .projectId(this.projectId)
                .name(this.name)
                .description(this.description)
                .status(ProjectStatus.COMPLETED)
                .engagement(this.engagement)
                .createdAt(this.createdAt)
                .lastUpdatedAt(Instant.now())
                .completedAt(Instant.now())
                .build();
    }

    /**
     * 标记为暂停
     */
    public SharedProjectLink markSuspended() {
        return new SharedProjectLink.Builder()
                .projectId(this.projectId)
                .name(this.name)
                .description(this.description)
                .status(ProjectStatus.SUSPENDED)
                .engagement(this.engagement)
                .createdAt(this.createdAt)
                .lastUpdatedAt(Instant.now())
                .completedAt(this.completedAt)
                .build();
    }

    // Getters
    public String getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public float getEngagement() {
        return engagement;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedProjectLink that = (SharedProjectLink) o;
        return Objects.equals(projectId, that.projectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId);
    }

    @Override
    public String toString() {
        return "SharedProjectLink{" +
                "projectId='" + projectId + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", engagement=" + engagement +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .projectId(this.projectId)
                .name(this.name)
                .description(this.description)
                .status(this.status)
                .engagement(this.engagement)
                .createdAt(this.createdAt)
                .lastUpdatedAt(this.lastUpdatedAt)
                .completedAt(this.completedAt);
    }

    public static final class Builder {
        private String projectId = "";
        private String name = "";
        private String description = "";
        private ProjectStatus status = ProjectStatus.ACTIVE;
        private float engagement = 0.5f;
        private Instant createdAt = Instant.now();
        private Instant lastUpdatedAt = Instant.now();
        private Instant completedAt = null;

        public Builder projectId(String projectId) {
            this.projectId = projectId != null ? projectId : "";
            return this;
        }

        public Builder name(String name) {
            this.name = name != null ? name : "";
            return this;
        }

        public Builder description(String description) {
            this.description = description != null ? description : "";
            return this;
        }

        public Builder status(ProjectStatus status) {
            this.status = status != null ? status : ProjectStatus.ACTIVE;
            return this;
        }

        public Builder engagement(float engagement) {
            this.engagement = Math.max(0f, Math.min(1f, engagement));
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt != null ? createdAt : Instant.now();
            return this;
        }

        public Builder lastUpdatedAt(Instant lastUpdatedAt) {
            this.lastUpdatedAt = lastUpdatedAt != null ? lastUpdatedAt : Instant.now();
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public SharedProjectLink build() {
            if (this.projectId.isBlank()) {
                throw new IllegalStateException("projectId is required");
            }
            return new SharedProjectLink(this);
        }
    }
}
