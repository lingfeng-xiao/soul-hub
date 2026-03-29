package com.lingfeng.sprite.domain.goal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * GoalService - 目标领域服务
 *
 * 提供目标的统一管理，包括长期目标、中期追踪、当前意向的管理。
 *
 * 对应旧: SelfModel.SelfGoal 和 AutonomousGoalService
 */
@Service
public final class GoalService {

    private static final Logger logger = LoggerFactory.getLogger(GoalService.class);

    private Map<String, LongTermGoal> longTermGoals;
    private Map<String, MidTermTrack> midTermTracks;
    private Map<String, ActiveIntention> activeIntentions;
    private GoalConflictResolver conflictResolver;

    public GoalService() {
        this.longTermGoals = new ConcurrentHashMap<>();
        this.midTermTracks = new ConcurrentHashMap<>();
        this.activeIntentions = new ConcurrentHashMap<>();
        this.conflictResolver = new GoalConflictResolver();
    }

    // ==================== LongTermGoal Operations ====================

    /**
     * 添加长期目标
     */
    public void addLongTermGoal(String goalId, String title, String description, LongTermGoal.GoalCategory category) {
        LongTermGoal goal = LongTermGoal.create(goalId, title, description, category);
        this.longTermGoals.put(goalId, goal);
        logger.info("Long-term goal added: {} - {}", goalId, title);
    }

    /**
     * 获取长期目标
     */
    public LongTermGoal getLongTermGoal(String goalId) {
        return longTermGoals.get(goalId);
    }

    /**
     * 获取所有活跃的长期目标
     */
    public List<LongTermGoal> getActiveLongTermGoals() {
        return longTermGoals.values().stream()
                .filter(g -> g.getStatus() == LongTermGoal.GoalStatus.ACTIVE)
                .sorted(Comparator.comparing(LongTermGoal::getCreatedAt))
                .collect(Collectors.toList());
    }

    /**
     * 更新目标进度
     */
    public void updateGoalProgress(String goalId, float newProgress) {
        LongTermGoal goal = longTermGoals.get(goalId);
        if (goal != null) {
            LongTermGoal updated = goal.withProgress(newProgress);
            this.longTermGoals.put(goalId, updated);
            logger.debug("Goal progress updated: {} -> {:.0f}%", goalId, newProgress * 100);

            // 如果目标完成，同步更新相关的中期追踪
            if (updated.getStatus() == LongTermGoal.GoalStatus.ACHIEVED) {
                syncTracksOnGoalCompletion(goalId);
            }
        }
    }

    /**
     * 添加里程碑
     */
    public void addMilestone(String goalId, String milestoneTitle) {
        LongTermGoal goal = longTermGoals.get(goalId);
        if (goal != null) {
            LongTermGoal updated = goal.withMilestoneAdded(milestoneTitle);
            this.longTermGoals.put(goalId, updated);
            logger.info("Milestone added to goal {}: {}", goalId, milestoneTitle);
        }
    }

    /**
     * 完成里程碑
     */
    public void completeMilestone(String goalId, String milestoneId) {
        LongTermGoal goal = longTermGoals.get(goalId);
        if (goal != null) {
            LongTermGoal updated = goal.withMilestoneCompleted(milestoneId);
            this.longTermGoals.put(goalId, updated);
            logger.info("Milestone completed: {} on goal {}", milestoneId, goalId);
        }
    }

    /**
     * 放弃目标
     */
    public void abandonGoal(String goalId, String reason) {
        LongTermGoal goal = longTermGoals.get(goalId);
        if (goal != null) {
            LongTermGoal updated = goal.abandon(reason);
            this.longTermGoals.put(goalId, updated);
            logger.warn("Goal abandoned: {} - reason: {}", goalId, reason);
        }
    }

    // ==================== MidTermTrack Operations ====================

    /**
     * 创建中期追踪
     */
    public void createMidTermTrack(String trackId, String title, String description, String relatedGoalId) {
        MidTermTrack track = MidTermTrack.create(trackId, title, description, relatedGoalId);
        this.midTermTracks.put(trackId, track);
        logger.info("Mid-term track created: {} for goal {}", trackId, relatedGoalId);
    }

    /**
     * 获取中期追踪
     */
    public MidTermTrack getMidTermTrack(String trackId) {
        return midTermTracks.get(trackId);
    }

    /**
     * 获取目标关联的中期追踪
     */
    public List<MidTermTrack> getTracksForGoal(String goalId) {
        return midTermTracks.values().stream()
                .filter(t -> t.getRelatedGoalId().equals(goalId))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有活跃追踪
     */
    public List<MidTermTrack> getActiveTracks() {
        return midTermTracks.values().stream()
                .filter(t -> t.getStatus() != MidTermTrack.TrackStatus.COMPLETED)
                .sorted(Comparator.comparing(MidTermTrack::getDeadline))
                .collect(Collectors.toList());
    }

    /**
     * 更新追踪进度
     */
    public void updateTrackProgress(String trackId, float newProgress) {
        MidTermTrack track = midTermTracks.get(trackId);
        if (track != null) {
            MidTermTrack updated = track.withProgress(newProgress);
            this.midTermTracks.put(trackId, updated);
            logger.debug("Track progress updated: {} -> {:.0f}%", trackId, newProgress * 100);

            // 如果追踪完成，同步更新关联的长期目标
            if (updated.getStatus() == MidTermTrack.TrackStatus.COMPLETED) {
                syncGoalOnTrackCompletion(updated);
            }
        }
    }

    /**
     * 阻塞追踪
     */
    public void blockTrack(String trackId, String reason) {
        MidTermTrack track = midTermTracks.get(trackId);
        if (track != null) {
            MidTermTrack updated = track.blocked(reason);
            this.midTermTracks.put(trackId, updated);
            logger.warn("Track blocked: {} - reason: {}", trackId, reason);
        }
    }

    private void syncTracksOnGoalCompletion(String goalId) {
        midTermTracks.values().stream()
                .filter(t -> t.getRelatedGoalId().equals(goalId))
                .forEach(t -> {
                    MidTermTrack completed = t.markCompleted();
                    midTermTracks.put(t.getTrackId(), completed);
                });
    }

    private void syncGoalOnTrackCompletion(MidTermTrack track) {
        LongTermGoal goal = longTermGoals.get(track.getRelatedGoalId());
        if (goal != null) {
            // 计算新的总体进度
            List<MidTermTrack> allTracks = getTracksForGoal(goal.getGoalId());
            float totalProgress = allTracks.stream()
                    .map(MidTermTrack::getActualProgress)
                    .reduce(0f, Float::sum) / allTracks.size();

            LongTermGoal updated = goal.withProgress(totalProgress);
            longTermGoals.put(goal.getGoalId(), updated);
        }
    }

    // ==================== ActiveIntention Operations ====================

    /**
     * 创建当前意向
     */
    public void createIntention(String intentionId, String description, ActiveIntention.Urgency urgency) {
        ActiveIntention intention = ActiveIntention.create(intentionId, description, urgency);
        this.activeIntentions.put(intentionId, intention);
        logger.info("Active intention created: {} - {}", intentionId, description);
    }

    /**
     * 获取当前意向
     */
    public ActiveIntention getIntention(String intentionId) {
        return activeIntentions.get(intentionId);
    }

    /**
     * 获取所有活跃意向
     */
    public List<ActiveIntention> getActiveIntentions() {
        return activeIntentions.values().stream()
                .filter(i -> i.getStatus() == ActiveIntention.IntentionStatus.ACTIVE)
                .sorted(Comparator.comparing(ActiveIntention::getUrgency)
                        .thenComparing(ActiveIntention::getDeadline))
                .collect(Collectors.toList());
    }

    /**
     * 获取最高优先级意向
     */
    public ActiveIntention getTopPriorityIntention() {
        return getActiveIntentions().stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * 完成意向
     */
    public void completeIntention(String intentionId) {
        ActiveIntention intention = activeIntentions.get(intentionId);
        if (intention != null) {
            ActiveIntention updated = intention.markCompleted();
            this.activeIntentions.put(intentionId, updated);
            logger.info("Intention completed: {}", intentionId);
        }
    }

    /**
     * 失败意向
     */
    public void failIntention(String intentionId, String reason) {
        ActiveIntention intention = activeIntentions.get(intentionId);
        if (intention != null) {
            ActiveIntention updated = intention.markFailed(reason);
            this.activeIntentions.put(intentionId, updated);
            logger.warn("Intention failed: {} - reason: {}", intentionId, reason);
        }
    }

    /**
     * 取消意向
     */
    public void cancelIntention(String intentionId) {
        ActiveIntention intention = activeIntentions.get(intentionId);
        if (intention != null) {
            ActiveIntention updated = intention.cancel();
            this.activeIntentions.put(intentionId, updated);
            logger.info("Intention cancelled: {}", intentionId);
        }
    }

    // ==================== Conflict Resolution ====================

    /**
     * 检测冲突
     */
    public GoalConflictResolver.ConflictDetectionResult detectConflicts() {
        return conflictResolver.detectConflicts(
                new ArrayList<>(longTermGoals.values()),
                new ArrayList<>(activeIntentions.values())
        );
    }

    /**
     * 解决冲突
     */
    public GoalConflictResolver.ResolutionResult resolveConflict(GoalConflictResolver.Conflict conflict) {
        return conflictResolver.resolve(conflict,
                new ArrayList<>(longTermGoals.values()),
                new ArrayList<>(activeIntentions.values()));
    }

    /**
     * 获取检测到的冲突
     */
    public List<GoalConflictResolver.Conflict> getDetectedConflicts() {
        return conflictResolver.getDetectedConflicts();
    }

    // ==================== Summary ====================

    /**
     * 获取目标摘要
     */
    public String getGoalSummary() {
        long activeGoals = longTermGoals.values().stream()
                .filter(g -> g.getStatus() == LongTermGoal.GoalStatus.ACTIVE).count();
        long activeTracks = midTermTracks.values().stream()
                .filter(t -> t.getStatus() != MidTermTrack.TrackStatus.COMPLETED).count();
        long activeIntentions = this.activeIntentions.values().stream()
                .filter(i -> i.getStatus() == ActiveIntention.IntentionStatus.ACTIVE).count();

        return String.format(
                "长期目标: %d个活跃 | 中期追踪: %d个进行中 | 当前意向: %d个活跃",
                activeGoals, activeTracks, activeIntentions
        );
    }

    /**
     * 完整性检查
     */
    public boolean isComplete() {
        return !longTermGoals.isEmpty() || !midTermTracks.isEmpty() || !activeIntentions.isEmpty();
    }

    /**
     * Replace the goal state from persistence.
     */
    public void replaceState(
            List<LongTermGoal> longTermGoals,
            List<MidTermTrack> midTermTracks,
            List<ActiveIntention> activeIntentions
    ) {
        this.longTermGoals = new ConcurrentHashMap<>();
        this.midTermTracks = new ConcurrentHashMap<>();
        this.activeIntentions = new ConcurrentHashMap<>();
        if (longTermGoals != null) {
            longTermGoals.forEach(goal -> this.longTermGoals.put(goal.getGoalId(), goal));
        }
        if (midTermTracks != null) {
            midTermTracks.forEach(track -> this.midTermTracks.put(track.getTrackId(), track));
        }
        if (activeIntentions != null) {
            activeIntentions.forEach(intention -> this.activeIntentions.put(intention.getIntentionId(), intention));
        }
    }

    /**
     * Reset all goal data.
     */
    public void reset() {
        this.longTermGoals = new ConcurrentHashMap<>();
        this.midTermTracks = new ConcurrentHashMap<>();
        this.activeIntentions = new ConcurrentHashMap<>();
        this.conflictResolver = new GoalConflictResolver();
    }

    // ==================== Builder ====================

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<LongTermGoal> longTermGoals = new ArrayList<>();
        private List<MidTermTrack> midTermTracks = new ArrayList<>();
        private List<ActiveIntention> activeIntentions = new ArrayList<>();

        public Builder longTermGoals(List<LongTermGoal> longTermGoals) {
            this.longTermGoals = longTermGoals != null ? longTermGoals : new ArrayList<>();
            return this;
        }

        public Builder midTermTracks(List<MidTermTrack> midTermTracks) {
            this.midTermTracks = midTermTracks != null ? midTermTracks : new ArrayList<>();
            return this;
        }

        public Builder activeIntentions(List<ActiveIntention> activeIntentions) {
            this.activeIntentions = activeIntentions != null ? activeIntentions : new ArrayList<>();
            return this;
        }

        public GoalService build() {
            GoalService service = new GoalService();
            this.longTermGoals.forEach(g -> service.longTermGoals.put(g.getGoalId(), g));
            this.midTermTracks.forEach(t -> service.midTermTracks.put(t.getTrackId(), t));
            this.activeIntentions.forEach(i -> service.activeIntentions.put(i.getIntentionId(), i));
            return service;
        }
    }
}
