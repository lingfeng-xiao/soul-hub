package com.lingfeng.sprite.domain.goal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * LongTermGoal - 长期目标
 *
 * 代表 Sprite 的长期愿景和目标，通常是跨越数周或数月的目标。
 *
 * 对应旧: SelfModel.SelfGoal 中的长期目标概念
 */
public final class LongTermGoal {

    /**
     * 目标状态
     */
    public enum GoalStatus {
        ACTIVE,      // 进行中
        ACHIEVED,    // 已达成
        ABANDONED,   // 已放弃
        SUSPENDED,   // 已暂停
        REJECTED     // 已拒绝
    }

    /**
     * 目标类别
     */
    public enum GoalCategory {
        PERSONAL_GROWTH,     // 个人成长
        RELATIONSHIP,        // 关系建设
        CAPABILITY,          // 能力提升
        CREATION,            // 创作产出
        LEARNING,            // 学习探索
        CONTRIBUTION         // 贡献社会
    }

    /**
     * 目标 ID
     */
    private final String goalId;

    /**
     * 目标标题
     */
    private final String title;

    /**
     * 目标描述
     */
    private final String description;

    /**
     * 目标类别
     */
    private final GoalCategory category;

    /**
     * 目标状态
     */
    private final GoalStatus status;

    /**
     * 达成进度 (0-1)
     */
    private final float progress;

    /**
     * 创建时间
     */
    private final Instant createdAt;

    /**
     * 预期完成时间
     */
    private final Instant expectedCompletionAt;

    /**
     * 完成时间
     */
    private final Instant completedAt;

    /**
     * 里程碑列表
     */
    private final List<Milestone> milestones;

    /**
     * 放弃原因
     */
    private final String abandonmentReason;

    private LongTermGoal(Builder builder) {
        this.goalId = builder.goalId;
        this.title = builder.title;
        this.description = builder.description;
        this.category = builder.category;
        this.status = builder.status;
        this.progress = builder.progress;
        this.createdAt = builder.createdAt;
        this.expectedCompletionAt = builder.expectedCompletionAt;
        this.completedAt = builder.completedAt;
        this.milestones = List.copyOf(builder.milestones);
        this.abandonmentReason = builder.abandonmentReason;
    }

    /**
     * 创建长期目标
     */
    public static LongTermGoal create(String goalId, String title, String description, GoalCategory category) {
        return new LongTermGoal.Builder()
                .goalId(goalId)
                .title(title)
                .description(description)
                .category(category)
                .status(GoalStatus.ACTIVE)
                .progress(0f)
                .createdAt(Instant.now())
                .expectedCompletionAt(Instant.now().plusSeconds(30 * 24 * 60 * 60)) // 默认30天后
                .milestones(new ArrayList<>())
                .build();
    }

    /**
     * 更新进度
     */
    public LongTermGoal withProgress(float newProgress) {
        float clampedProgress = Math.max(0f, Math.min(1f, newProgress));
        return new LongTermGoal.Builder()
                .goalId(this.goalId)
                .title(this.title)
                .description(this.description)
                .category(this.category)
                .status(clampedProgress >= 1f ? GoalStatus.ACHIEVED : this.status)
                .progress(clampedProgress)
                .createdAt(this.createdAt)
                .expectedCompletionAt(this.expectedCompletionAt)
                .completedAt(clampedProgress >= 1f ? Instant.now() : this.completedAt)
                .milestones(this.milestones)
                .abandonmentReason(this.abandonmentReason)
                .build();
    }

    /**
     * 添加里程碑
     */
    public LongTermGoal withMilestoneAdded(String milestoneTitle) {
        List<Milestone> newMilestones = new ArrayList<>(this.milestones);
        newMilestones.add(new Milestone(
                "ms-" + System.currentTimeMillis(),
                milestoneTitle,
                false,
                null
        ));
        return new LongTermGoal.Builder()
                .goalId(this.goalId)
                .title(this.title)
                .description(this.description)
                .category(this.category)
                .status(this.status)
                .progress(this.progress)
                .createdAt(this.createdAt)
                .expectedCompletionAt(this.expectedCompletionAt)
                .completedAt(this.completedAt)
                .milestones(newMilestones)
                .abandonmentReason(this.abandonmentReason)
                .build();
    }

    /**
     * 标记里程碑完成
     */
    public LongTermGoal withMilestoneCompleted(String milestoneId) {
        List<Milestone> newMilestones = new ArrayList<>();
        for (Milestone ms : this.milestones) {
            if (ms.id().equals(milestoneId)) {
                newMilestones.add(new Milestone(ms.id(), ms.title(), true, Instant.now()));
            } else {
                newMilestones.add(ms);
            }
        }
        // 更新进度
        long completed = newMilestones.stream().filter(Milestone::completed).count();
        float newProgress = (float) completed / newMilestones.size();

        return new LongTermGoal.Builder()
                .goalId(this.goalId)
                .title(this.title)
                .description(this.description)
                .category(this.category)
                .status(newProgress >= 1f ? GoalStatus.ACHIEVED : this.status)
                .progress(newProgress)
                .createdAt(this.createdAt)
                .expectedCompletionAt(this.expectedCompletionAt)
                .completedAt(newProgress >= 1f ? Instant.now() : this.completedAt)
                .milestones(newMilestones)
                .abandonmentReason(this.abandonmentReason)
                .build();
    }

    /**
     * 暂停目标
     */
    public LongTermGoal suspend() {
        return new LongTermGoal.Builder()
                .goalId(this.goalId)
                .title(this.title)
                .description(this.description)
                .category(this.category)
                .status(GoalStatus.SUSPENDED)
                .progress(this.progress)
                .createdAt(this.createdAt)
                .expectedCompletionAt(this.expectedCompletionAt)
                .completedAt(this.completedAt)
                .milestones(this.milestones)
                .abandonmentReason(this.abandonmentReason)
                .build();
    }

    /**
     * 放弃目标
     */
    public LongTermGoal abandon(String reason) {
        return new LongTermGoal.Builder()
                .goalId(this.goalId)
                .title(this.title)
                .description(this.description)
                .category(this.category)
                .status(GoalStatus.ABANDONED)
                .progress(this.progress)
                .createdAt(this.createdAt)
                .expectedCompletionAt(this.expectedCompletionAt)
                .completedAt(this.completedAt)
                .milestones(this.milestones)
                .abandonmentReason(reason)
                .build();
    }

    // Getters
    public String getGoalId() {
        return goalId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public GoalCategory getCategory() {
        return category;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public float getProgress() {
        return progress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpectedCompletionAt() {
        return expectedCompletionAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public List<Milestone> getMilestones() {
        return milestones;
    }

    public String getAbandonmentReason() {
        return abandonmentReason;
    }

    /**
     * 里程碑记录
     */
    public record Milestone(
            String id,
            String title,
            boolean completed,
            Instant completedAt
    ) {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LongTermGoal that = (LongTermGoal) o;
        return Objects.equals(goalId, that.goalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(goalId);
    }

    @Override
    public String toString() {
        return "LongTermGoal{" +
                "goalId='" + goalId + '\'' +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", progress=" + (int)(progress * 100) + "%" +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .goalId(this.goalId)
                .title(this.title)
                .description(this.description)
                .category(this.category)
                .status(this.status)
                .progress(this.progress)
                .createdAt(this.createdAt)
                .expectedCompletionAt(this.expectedCompletionAt)
                .completedAt(this.completedAt)
                .milestones(this.milestones)
                .abandonmentReason(this.abandonmentReason);
    }

    public static final class Builder {
        private String goalId = "";
        private String title = "";
        private String description = "";
        private GoalCategory category = GoalCategory.PERSONAL_GROWTH;
        private GoalStatus status = GoalStatus.ACTIVE;
        private float progress = 0f;
        private Instant createdAt = Instant.now();
        private Instant expectedCompletionAt = Instant.now().plusSeconds(30 * 24 * 60 * 60);
        private Instant completedAt = null;
        private List<Milestone> milestones = new ArrayList<>();
        private String abandonmentReason = null;

        public Builder goalId(String goalId) {
            this.goalId = goalId != null ? goalId : "";
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

        public Builder category(GoalCategory category) {
            this.category = category != null ? category : GoalCategory.PERSONAL_GROWTH;
            return this;
        }

        public Builder status(GoalStatus status) {
            this.status = status != null ? status : GoalStatus.ACTIVE;
            return this;
        }

        public Builder progress(float progress) {
            this.progress = Math.max(0f, Math.min(1f, progress));
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt != null ? createdAt : Instant.now();
            return this;
        }

        public Builder expectedCompletionAt(Instant expectedCompletionAt) {
            this.expectedCompletionAt = expectedCompletionAt;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder milestones(List<Milestone> milestones) {
            this.milestones = milestones != null ? new ArrayList<>(milestones) : new ArrayList<>();
            return this;
        }

        public Builder abandonmentReason(String abandonmentReason) {
            this.abandonmentReason = abandonmentReason;
            return this;
        }

        public LongTermGoal build() {
            if (this.goalId.isBlank()) {
                throw new IllegalStateException("goalId is required");
            }
            return new LongTermGoal(this);
        }
    }
}
