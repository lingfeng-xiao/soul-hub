package com.lingfeng.sprite.domain.goal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * MidTermTrack - 中期追踪
 *
 * 代表连接长期目标与日常行动的中间层，通常是数天到数周的阶段性目标。
 *
 * 对应旧: SelfModel.SelfGoal 中的中期追踪概念
 */
public final class MidTermTrack {

    /**
     * 追踪状态
     */
    public enum TrackStatus {
        ON_TRACK,      // 正常进行
        AHEAD,         // 超前进度
        BEHIND,        // 落后进度
        BLOCKED,       // 被阻塞
        COMPLETED      // 已完成
    }

    /**
     * 关联的长期目标 ID
     */
    private final String relatedGoalId;

    /**
     * 追踪 ID
     */
    private final String trackId;

    /**
     * 追踪标题
     */
    private final String title;

    /**
     * 追踪描述
     */
    private final String description;

    /**
     * 当前状态
     */
    private final TrackStatus status;

    /**
     * 目标完成度 (0-1)
     */
    private final float targetProgress;

    /**
     * 实际完成度 (0-1)
     */
    private final float actualProgress;

    /**
     * 开始时间
     */
    private final Instant startedAt;

    /**
     * 截止时间
     */
    private final Instant deadline;

    /**
     * 最后检查时间
     */
    private final Instant lastCheckedAt;

    /**
     * 关联的行动 ID 列表
     */
    private final List<String> relatedActionIds;

    /**
     * 阻塞原因
     */
    private final String blocker;

    private MidTermTrack(Builder builder) {
        this.relatedGoalId = builder.relatedGoalId;
        this.trackId = builder.trackId;
        this.title = builder.title;
        this.description = builder.description;
        this.status = builder.status;
        this.targetProgress = builder.targetProgress;
        this.actualProgress = builder.actualProgress;
        this.startedAt = builder.startedAt;
        this.deadline = builder.deadline;
        this.lastCheckedAt = builder.lastCheckedAt;
        this.relatedActionIds = List.copyOf(builder.relatedActionIds);
        this.blocker = builder.blocker;
    }

    /**
     * 创建中期追踪
     */
    public static MidTermTrack create(String trackId, String title, String description, String relatedGoalId) {
        return new MidTermTrack.Builder()
                .trackId(trackId)
                .title(title)
                .description(description)
                .relatedGoalId(relatedGoalId)
                .status(TrackStatus.ON_TRACK)
                .targetProgress(0f)
                .actualProgress(0f)
                .startedAt(Instant.now())
                .deadline(Instant.now().plusSeconds(7 * 24 * 60 * 60)) // 默认7天
                .lastCheckedAt(Instant.now())
                .relatedActionIds(new ArrayList<>())
                .build();
    }

    /**
     * 更新进度
     */
    public MidTermTrack withProgress(float newActualProgress) {
        float clampedProgress = Math.max(0f, Math.min(1f, newActualProgress));
        TrackStatus newStatus = calculateStatus(clampedProgress, this.targetProgress);

        return new MidTermTrack.Builder()
                .trackId(this.trackId)
                .title(this.title)
                .description(this.description)
                .relatedGoalId(this.relatedGoalId)
                .status(newStatus)
                .targetProgress(this.targetProgress)
                .actualProgress(clampedProgress)
                .startedAt(this.startedAt)
                .deadline(this.deadline)
                .lastCheckedAt(Instant.now())
                .relatedActionIds(this.relatedActionIds)
                .blocker(this.blocker)
                .build();
    }

    /**
     * 添加关联行动
     */
    public MidTermTrack withActionAdded(String actionId) {
        List<String> newActions = new ArrayList<>(this.relatedActionIds);
        newActions.add(actionId);
        return new MidTermTrack.Builder()
                .trackId(this.trackId)
                .title(this.title)
                .description(this.description)
                .relatedGoalId(this.relatedGoalId)
                .status(this.status)
                .targetProgress(this.targetProgress)
                .actualProgress(this.actualProgress)
                .startedAt(this.startedAt)
                .deadline(this.deadline)
                .lastCheckedAt(Instant.now())
                .relatedActionIds(newActions)
                .blocker(this.blocker)
                .build();
    }

    /**
     * 设置阻塞
     */
    public MidTermTrack blocked(String reason) {
        return new MidTermTrack.Builder()
                .trackId(this.trackId)
                .title(this.title)
                .description(this.description)
                .relatedGoalId(this.relatedGoalId)
                .status(TrackStatus.BLOCKED)
                .targetProgress(this.targetProgress)
                .actualProgress(this.actualProgress)
                .startedAt(this.startedAt)
                .deadline(this.deadline)
                .lastCheckedAt(Instant.now())
                .relatedActionIds(this.relatedActionIds)
                .blocker(reason)
                .build();
    }

    /**
     * 解除阻塞
     */
    public MidTermTrack unblock() {
        return new MidTermTrack.Builder()
                .trackId(this.trackId)
                .title(this.title)
                .description(this.description)
                .relatedGoalId(this.relatedGoalId)
                .status(calculateStatus(this.actualProgress, this.targetProgress))
                .targetProgress(this.targetProgress)
                .actualProgress(this.actualProgress)
                .startedAt(this.startedAt)
                .deadline(this.deadline)
                .lastCheckedAt(Instant.now())
                .relatedActionIds(this.relatedActionIds)
                .blocker(null)
                .build();
    }

    /**
     * 标记完成
     */
    public MidTermTrack markCompleted() {
        return new MidTermTrack.Builder()
                .trackId(this.trackId)
                .title(this.title)
                .description(this.description)
                .relatedGoalId(this.relatedGoalId)
                .status(TrackStatus.COMPLETED)
                .targetProgress(1f)
                .actualProgress(1f)
                .startedAt(this.startedAt)
                .deadline(this.deadline)
                .lastCheckedAt(Instant.now())
                .relatedActionIds(this.relatedActionIds)
                .blocker(null)
                .build();
    }

    private TrackStatus calculateStatus(float actual, float target) {
        if (actual >= 1f) return TrackStatus.COMPLETED;
        if (actual >= target + 0.1f) return TrackStatus.AHEAD;
        if (actual < target - 0.1f) return TrackStatus.BEHIND;
        return TrackStatus.ON_TRACK;
    }

    // Getters
    public String getRelatedGoalId() {
        return relatedGoalId;
    }

    public String getTrackId() {
        return trackId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TrackStatus getStatus() {
        return status;
    }

    public float getTargetProgress() {
        return targetProgress;
    }

    public float getActualProgress() {
        return actualProgress;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public List<String> getRelatedActionIds() {
        return relatedActionIds;
    }

    public String getBlocker() {
        return blocker;
    }

    /**
     * 检查是否超时
     */
    public boolean isOverdue() {
        return Instant.now().isAfter(deadline) && status != TrackStatus.COMPLETED;
    }

    /**
     * 获取进度差距
     */
    public float getProgressGap() {
        return targetProgress - actualProgress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MidTermTrack that = (MidTermTrack) o;
        return Objects.equals(trackId, that.trackId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackId);
    }

    @Override
    public String toString() {
        return "MidTermTrack{" +
                "trackId='" + trackId + '\'' +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", progress=" + (int)(actualProgress * 100) + "%" +
                ", deadline=" + deadline +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .trackId(this.trackId)
                .title(this.title)
                .description(this.description)
                .relatedGoalId(this.relatedGoalId)
                .status(this.status)
                .targetProgress(this.targetProgress)
                .actualProgress(this.actualProgress)
                .startedAt(this.startedAt)
                .deadline(this.deadline)
                .lastCheckedAt(this.lastCheckedAt)
                .relatedActionIds(this.relatedActionIds)
                .blocker(this.blocker);
    }

    public static final class Builder {
        private String relatedGoalId = "";
        private String trackId = "";
        private String title = "";
        private String description = "";
        private TrackStatus status = TrackStatus.ON_TRACK;
        private float targetProgress = 0f;
        private float actualProgress = 0f;
        private Instant startedAt = Instant.now();
        private Instant deadline = Instant.now().plusSeconds(7 * 24 * 60 * 60);
        private Instant lastCheckedAt = Instant.now();
        private List<String> relatedActionIds = new ArrayList<>();
        private String blocker = null;

        public Builder relatedGoalId(String relatedGoalId) {
            this.relatedGoalId = relatedGoalId != null ? relatedGoalId : "";
            return this;
        }

        public Builder trackId(String trackId) {
            this.trackId = trackId != null ? trackId : "";
            return this;
        }

        public Builder title(String title) {
            this.title = title != null ? title : "";
            return this;
        }

        public Builder description(String description) {
            this.description = description != null ? description : "";
            return this;
        }

        public Builder status(TrackStatus status) {
            this.status = status != null ? status : TrackStatus.ON_TRACK;
            return this;
        }

        public Builder targetProgress(float targetProgress) {
            this.targetProgress = Math.max(0f, Math.min(1f, targetProgress));
            return this;
        }

        public Builder actualProgress(float actualProgress) {
            this.actualProgress = Math.max(0f, Math.min(1f, actualProgress));
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt != null ? startedAt : Instant.now();
            return this;
        }

        public Builder deadline(Instant deadline) {
            this.deadline = deadline;
            return this;
        }

        public Builder lastCheckedAt(Instant lastCheckedAt) {
            this.lastCheckedAt = lastCheckedAt != null ? lastCheckedAt : Instant.now();
            return this;
        }

        public Builder relatedActionIds(List<String> relatedActionIds) {
            this.relatedActionIds = relatedActionIds != null ? new ArrayList<>(relatedActionIds) : new ArrayList<>();
            return this;
        }

        public Builder blocker(String blocker) {
            this.blocker = blocker;
            return this;
        }

        public MidTermTrack build() {
            if (this.trackId.isBlank()) {
                throw new IllegalStateException("trackId is required");
            }
            return new MidTermTrack(this);
        }
    }
}
