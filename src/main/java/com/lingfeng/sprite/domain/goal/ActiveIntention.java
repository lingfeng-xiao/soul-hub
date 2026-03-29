package com.lingfeng.sprite.domain.goal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ActiveIntention - 当前意向
 *
 * 代表 Sprite 当前正在追求的具体意向或意图，是最小的行动驱动力单位。
 *
 * 对应旧: SelfModel.SelfGoal 中的当前意向概念
 */
public final class ActiveIntention {

    /**
     * 意向状态
     */
    public enum IntentionStatus {
        ACTIVE,       // 活跃中
        COMPLETED,    // 已完成
        CANCELLED,    // 已取消
        SUPERSEDED,   // 被取代
        FAILED        // 失败
    }

    /**
     * 意向紧迫度
     */
    public enum Urgency {
        CRITICAL,  // 紧急
        HIGH,      // 高
        NORMAL,    // 普通
        LOW        // 低
    }

    /**
     * 意向 ID
     */
    private final String intentionId;

    /**
     * 意向描述
     */
    private final String description;

    /**
     * 关联的中期追踪 ID
     */
    private final String relatedTrackId;

    /**
     * 当前状态
     */
    private final IntentionStatus status;

    /**
     * 紧迫度
     */
    private final Urgency urgency;

    /**
     * 意向强度 (0-1)
     */
    private final float intensity;

    /**
     * 创建时间
     */
    private final Instant createdAt;

    /**
     * 激活时间
     */
    private final Instant activatedAt;

    /**
     * 完成时间
     */
    private final Instant completedAt;

    /**
     * 截止时间
     */
    private final Instant deadline;

    /**
     * 失败原因
     */
    private final String failureReason;

    /**
     * 依赖的其他意向 ID
     */
    private final List<String> dependsOn;

    private ActiveIntention(Builder builder) {
        this.intentionId = builder.intentionId;
        this.description = builder.description;
        this.relatedTrackId = builder.relatedTrackId;
        this.status = builder.status;
        this.urgency = builder.urgency;
        this.intensity = builder.intensity;
        this.createdAt = builder.createdAt;
        this.activatedAt = builder.activatedAt;
        this.completedAt = builder.completedAt;
        this.deadline = builder.deadline;
        this.failureReason = builder.failureReason;
        this.dependsOn = List.copyOf(builder.dependsOn);
    }

    /**
     * 创建当前意向
     */
    public static ActiveIntention create(String intentionId, String description, Urgency urgency) {
        return new ActiveIntention.Builder()
                .intentionId(intentionId)
                .description(description)
                .relatedTrackId(null)
                .status(IntentionStatus.ACTIVE)
                .urgency(urgency)
                .intensity(1.0f)
                .createdAt(Instant.now())
                .activatedAt(Instant.now())
                .completedAt(null)
                .deadline(Instant.now().plusSeconds(60 * 60)) // 默认1小时
                .dependsOn(new ArrayList<>())
                .build();
    }

    /**
     * 创建关键意向
     */
    public static ActiveIntention critical(String intentionId, String description) {
        return create(intentionId, description, Urgency.CRITICAL)
                .withDeadline(Instant.now().plusSeconds(5 * 60)); // 5分钟截止
    }

    /**
     * 更新强度
     */
    public ActiveIntention withIntensity(float newIntensity) {
        return new ActiveIntention.Builder()
                .intentionId(this.intentionId)
                .description(this.description)
                .relatedTrackId(this.relatedTrackId)
                .status(this.status)
                .urgency(this.urgency)
                .intensity(Math.max(0f, Math.min(1f, newIntensity)))
                .createdAt(this.createdAt)
                .activatedAt(this.activatedAt)
                .completedAt(this.completedAt)
                .deadline(this.deadline)
                .failureReason(this.failureReason)
                .dependsOn(this.dependsOn)
                .build();
    }

    /**
     * 更新紧迫度
     */
    public ActiveIntention withUrgency(Urgency newUrgency) {
        return new ActiveIntention.Builder()
                .intentionId(this.intentionId)
                .description(this.description)
                .relatedTrackId(this.relatedTrackId)
                .status(this.status)
                .urgency(newUrgency)
                .intensity(this.intensity)
                .createdAt(this.createdAt)
                .activatedAt(this.activatedAt)
                .completedAt(this.completedAt)
                .deadline(this.deadline)
                .failureReason(this.failureReason)
                .dependsOn(this.dependsOn)
                .build();
    }

    /**
     * 设置截止时间
     */
    public ActiveIntention withDeadline(Instant newDeadline) {
        return new ActiveIntention.Builder()
                .intentionId(this.intentionId)
                .description(this.description)
                .relatedTrackId(this.relatedTrackId)
                .status(this.status)
                .urgency(this.urgency)
                .intensity(this.intensity)
                .createdAt(this.createdAt)
                .activatedAt(this.activatedAt)
                .completedAt(this.completedAt)
                .deadline(newDeadline)
                .failureReason(this.failureReason)
                .dependsOn(this.dependsOn)
                .build();
    }

    /**
     * 标记完成
     */
    public ActiveIntention markCompleted() {
        return new ActiveIntention.Builder()
                .intentionId(this.intentionId)
                .description(this.description)
                .relatedTrackId(this.relatedTrackId)
                .status(IntentionStatus.COMPLETED)
                .urgency(this.urgency)
                .intensity(0f)
                .createdAt(this.createdAt)
                .activatedAt(this.activatedAt)
                .completedAt(Instant.now())
                .deadline(this.deadline)
                .failureReason(null)
                .dependsOn(this.dependsOn)
                .build();
    }

    /**
     * 标记失败
     */
    public ActiveIntention markFailed(String reason) {
        return new ActiveIntention.Builder()
                .intentionId(this.intentionId)
                .description(this.description)
                .relatedTrackId(this.relatedTrackId)
                .status(IntentionStatus.FAILED)
                .urgency(this.urgency)
                .intensity(0f)
                .createdAt(this.createdAt)
                .activatedAt(this.activatedAt)
                .completedAt(Instant.now())
                .deadline(this.deadline)
                .failureReason(reason)
                .dependsOn(this.dependsOn)
                .build();
    }

    /**
     * 取消
     */
    public ActiveIntention cancel() {
        return new ActiveIntention.Builder()
                .intentionId(this.intentionId)
                .description(this.description)
                .relatedTrackId(this.relatedTrackId)
                .status(IntentionStatus.CANCELLED)
                .urgency(this.urgency)
                .intensity(0f)
                .createdAt(this.createdAt)
                .activatedAt(this.activatedAt)
                .completedAt(Instant.now())
                .deadline(this.deadline)
                .failureReason(null)
                .dependsOn(this.dependsOn)
                .build();
    }

    /**
     * 检查是否超时
     */
    public boolean isTimedOut() {
        return deadline != null &&
                Instant.now().isAfter(deadline) &&
                status == IntentionStatus.ACTIVE;
    }

    // Getters
    public String getIntentionId() {
        return intentionId;
    }

    public String getDescription() {
        return description;
    }

    public String getRelatedTrackId() {
        return relatedTrackId;
    }

    public IntentionStatus getStatus() {
        return status;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public float getIntensity() {
        return intensity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveIntention that = (ActiveIntention) o;
        return Objects.equals(intentionId, that.intentionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(intentionId);
    }

    @Override
    public String toString() {
        return "ActiveIntention{" +
                "intentionId='" + intentionId + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", urgency=" + urgency +
                ", intensity=" + intensity +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .intentionId(this.intentionId)
                .description(this.description)
                .relatedTrackId(this.relatedTrackId)
                .status(this.status)
                .urgency(this.urgency)
                .intensity(this.intensity)
                .createdAt(this.createdAt)
                .activatedAt(this.activatedAt)
                .completedAt(this.completedAt)
                .deadline(this.deadline)
                .failureReason(this.failureReason)
                .dependsOn(this.dependsOn);
    }

    public static final class Builder {
        private String intentionId = "";
        private String description = "";
        private String relatedTrackId = null;
        private IntentionStatus status = IntentionStatus.ACTIVE;
        private Urgency urgency = Urgency.NORMAL;
        private float intensity = 1.0f;
        private Instant createdAt = Instant.now();
        private Instant activatedAt = Instant.now();
        private Instant completedAt = null;
        private Instant deadline = Instant.now().plusSeconds(60 * 60);
        private String failureReason = null;
        private List<String> dependsOn = new ArrayList<>();

        public Builder intentionId(String intentionId) {
            this.intentionId = intentionId != null ? intentionId : "";
            return this;
        }

        public Builder description(String description) {
            this.description = description != null ? description : "";
            return this;
        }

        public Builder relatedTrackId(String relatedTrackId) {
            this.relatedTrackId = relatedTrackId;
            return this;
        }

        public Builder status(IntentionStatus status) {
            this.status = status != null ? status : IntentionStatus.ACTIVE;
            return this;
        }

        public Builder urgency(Urgency urgency) {
            this.urgency = urgency != null ? urgency : Urgency.NORMAL;
            return this;
        }

        public Builder intensity(float intensity) {
            this.intensity = Math.max(0f, Math.min(1f, intensity));
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt != null ? createdAt : Instant.now();
            return this;
        }

        public Builder activatedAt(Instant activatedAt) {
            this.activatedAt = activatedAt != null ? activatedAt : Instant.now();
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder deadline(Instant deadline) {
            this.deadline = deadline;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder dependsOn(List<String> dependsOn) {
            this.dependsOn = dependsOn != null ? new ArrayList<>(dependsOn) : new ArrayList<>();
            return this;
        }

        public ActiveIntention build() {
            if (this.intentionId.isBlank()) {
                throw new IllegalStateException("intentionId is required");
            }
            return new ActiveIntention(this);
        }
    }
}
