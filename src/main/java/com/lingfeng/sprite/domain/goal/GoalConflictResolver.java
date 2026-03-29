package com.lingfeng.sprite.domain.goal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * GoalConflictResolver - 目标冲突解决器
 *
 * 负责检测和解决目标之间的冲突，支持优先级排序和资源分配。
 *
 * 对应旧: SelfModel.SelfGoal 和 AutonomousGoalService 的冲突处理逻辑
 */
public final class GoalConflictResolver {

    private static final Logger logger = LoggerFactory.getLogger(GoalConflictResolver.class);

    /**
     * 冲突类型
     */
    public enum ConflictType {
        RESOURCE,      // 资源冲突
        DIRECTION,     // 方向冲突
        TIME,          // 时间冲突
        PRIORITY,      // 优先级冲突
        DEPENDENCY     // 依赖冲突
    }

    /**
     * 冲突记录
     */
    public record Conflict(
            String conflictId,
            ConflictType type,
            String goalId1,
            String goalId2,
            String description,
            Instant detectedAt,
            ResolutionStrategy resolution
    ) {}

    /**
     * 解决策略
     */
    public enum ResolutionStrategy {
        PRIORITY_BASED,      // 基于优先级
        FIRST_COME_FIRST,    // 先到先得
        SPLIT_RESOURCE,      // 分割资源
        DEFER_LESS_IMPORTANT, // 延迟次要目标
        CANCEL_CONFLICTING   // 取消冲突目标
    }

    /**
     * 冲突检测结果
     */
    public record ConflictDetectionResult(
            boolean hasConflict,
            List<Conflict> conflicts,
            ResolutionStrategy recommendedStrategy
    ) {}

    /**
     * 冲突解决结果
     */
    public record ResolutionResult(
            boolean resolved,
            String winningGoalId,
            String losingGoalId,
            String reasoning
    ) {}

    private final List<Conflict> detectedConflicts;

    public GoalConflictResolver() {
        this.detectedConflicts = new ArrayList<>();
    }

    /**
     * 检测目标间的冲突
     */
    public ConflictDetectionResult detectConflicts(List<LongTermGoal> goals, List<ActiveIntention> intentions) {
        List<Conflict> conflicts = new ArrayList<>();

        // 检测目标间的优先级冲突
        for (int i = 0; i < goals.size(); i++) {
            for (int j = i + 1; j < goals.size(); j++) {
                LongTermGoal goal1 = goals.get(i);
                LongTermGoal goal2 = goals.get(j);

                if (goal1.getStatus() != LongTermGoal.GoalStatus.ACTIVE ||
                    goal2.getStatus() != LongTermGoal.GoalStatus.ACTIVE) {
                    continue;
                }

                // 检测方向冲突（不同类别但可能冲突）
                if (hasDirectionConflict(goal1, goal2)) {
                    conflicts.add(new Conflict(
                            "conflict-" + System.currentTimeMillis(),
                            ConflictType.DIRECTION,
                            goal1.getGoalId(),
                            goal2.getGoalId(),
                            "目标方向冲突: " + goal1.getTitle() + " vs " + goal2.getTitle(),
                            Instant.now(),
                            ResolutionStrategy.PRIORITY_BASED
                    ));
                }

                // 检测时间冲突
                if (hasTimeConflict(goal1, goal2)) {
                    conflicts.add(new Conflict(
                            "conflict-" + System.currentTimeMillis(),
                            ConflictType.TIME,
                            goal1.getGoalId(),
                            goal2.getGoalId(),
                            "目标时间冲突: " + goal1.getTitle() + " vs " + goal2.getTitle(),
                            Instant.now(),
                            ResolutionStrategy.FIRST_COME_FIRST
                    ));
                }
            }
        }

        // 检测意向与目标间的冲突
        for (ActiveIntention intention : intentions) {
            if (intention.getStatus() != ActiveIntention.IntentionStatus.ACTIVE) {
                continue;
            }

            for (LongTermGoal goal : goals) {
                if (goal.getStatus() != LongTermGoal.GoalStatus.ACTIVE) {
                    continue;
                }

                if (hasPriorityConflict(intention, goal)) {
                    conflicts.add(new Conflict(
                            "conflict-" + System.currentTimeMillis(),
                            ConflictType.PRIORITY,
                            intention.getIntentionId(),
                            goal.getGoalId(),
                            "意向与目标优先级冲突: " + intention.getDescription() + " vs " + goal.getTitle(),
                            Instant.now(),
                            ResolutionStrategy.PRIORITY_BASED
                    ));
                }
            }
        }

        // 记录检测到的冲突
        detectedConflicts.addAll(conflicts);

        ResolutionStrategy recommendedStrategy = conflicts.isEmpty() ?
                null :
                determineOverallStrategy(conflicts);

        logger.debug("Conflict detection: found {} conflicts", conflicts.size());

        return new ConflictDetectionResult(!conflicts.isEmpty(), conflicts, recommendedStrategy);
    }

    /**
     * 解决冲突
     */
    public ResolutionResult resolve(Conflict conflict, List<LongTermGoal> goals, List<ActiveIntention> intentions) {
        logger.info("Resolving conflict: {} between {} and {}",
                conflict.type(), conflict.goalId1(), conflict.goalId2());

        return switch (conflict.resolution()) {
            case PRIORITY_BASED -> resolveByPriority(conflict, goals, intentions);
            case FIRST_COME_FIRST -> resolveByTime(conflict, goals, intentions);
            case SPLIT_RESOURCE -> resolveBySplit(conflict);
            case DEFER_LESS_IMPORTANT -> resolveByDefer(conflict, goals);
            case CANCEL_CONFLICTING -> resolveByCancel(conflict, goals);
        };
    }

    private boolean hasDirectionConflict(LongTermGoal goal1, LongTermGoal goal2) {
        // 简单检测：如果两个目标的类别不同但进度同时很高，可能有方向冲突
        return goal1.getCategory() != goal2.getCategory() &&
                goal1.getProgress() > 0.5f &&
                goal2.getProgress() > 0.5f;
    }

    private boolean hasTimeConflict(LongTermGoal goal1, LongTermGoal goal2) {
        // 检测截止时间是否重叠且接近
        if (goal1.getExpectedCompletionAt() == null || goal2.getExpectedCompletionAt() == null) {
            return false;
        }
        long diff = Math.abs(goal1.getExpectedCompletionAt().toEpochMilli() -
                           goal2.getExpectedCompletionAt().toEpochMilli());
        return diff < 7 * 24 * 60 * 60 * 1000L; // 7天内
    }

    private boolean hasPriorityConflict(ActiveIntention intention, LongTermGoal goal) {
        // 检测意向的紧迫度是否与目标的优先级冲突
        if (intention.getUrgency() == ActiveIntention.Urgency.CRITICAL &&
            goal.getCategory() == LongTermGoal.GoalCategory.PERSONAL_GROWTH) {
            return true;
        }
        return false;
    }

    private ResolutionStrategy determineOverallStrategy(List<Conflict> conflicts) {
        // 统计最常见的冲突类型
        long resourceConflicts = conflicts.stream()
                .filter(c -> c.type() == ConflictType.RESOURCE).count();
        long priorityConflicts = conflicts.stream()
                .filter(c -> c.type() == ConflictType.PRIORITY).count();

        if (resourceConflicts > priorityConflicts) {
            return ResolutionStrategy.SPLIT_RESOURCE;
        }
        return ResolutionStrategy.PRIORITY_BASED;
    }

    private ResolutionResult resolveByPriority(Conflict conflict, List<LongTermGoal> goals, List<ActiveIntention> intentions) {
        // 基于紧迫度和重要性决定
        Optional<LongTermGoal> goal1 = goals.stream()
                .filter(g -> g.getGoalId().equals(conflict.goalId1())).findFirst();
        Optional<LongTermGoal> goal2 = goals.stream()
                .filter(g -> g.getGoalId().equals(conflict.goalId2())).findFirst();

        String winner, loser;
        if (goal1.isPresent() && goal2.isPresent()) {
            // 比较进度：进度低的可能需要被延迟
            if (goal1.get().getProgress() > goal2.get().getProgress()) {
                winner = conflict.goalId1();
                loser = conflict.goalId2();
            } else {
                winner = conflict.goalId2();
                loser = conflict.goalId1();
            }
        } else {
            winner = conflict.goalId1();
            loser = conflict.goalId2();
        }

        return new ResolutionResult(true, winner, loser,
                "基于优先级解决: " + winner + " 优先于 " + loser);
    }

    private ResolutionResult resolveByTime(Conflict conflict, List<LongTermGoal> goals, List<ActiveIntention> intentions) {
        // 先到先得
        Optional<LongTermGoal> goal1 = goals.stream()
                .filter(g -> g.getGoalId().equals(conflict.goalId1())).findFirst();
        Optional<LongTermGoal> goal2 = goals.stream()
                .filter(g -> g.getGoalId().equals(conflict.goalId2())).findFirst();

        String winner, loser;
        if (goal1.isPresent() && goal2.isPresent()) {
            if (goal1.get().getCreatedAt().isBefore(goal2.get().getCreatedAt())) {
                winner = conflict.goalId1();
                loser = conflict.goalId2();
            } else {
                winner = conflict.goalId2();
                loser = conflict.goalId1();
            }
        } else {
            winner = conflict.goalId1();
            loser = conflict.goalId2();
        }

        return new ResolutionResult(true, winner, loser,
                "基于时间解决: " + winner + " 先创建所以优先");
    }

    private ResolutionResult resolveBySplit(Conflict conflict) {
        return new ResolutionResult(true, conflict.goalId1(), conflict.goalId2(),
                "资源分割解决: 两个目标各分一半资源");
    }

    private ResolutionResult resolveByDefer(Conflict conflict, List<LongTermGoal> goals) {
        // 延迟进度较低的目标
        Optional<LongTermGoal> goal1 = goals.stream()
                .filter(g -> g.getGoalId().equals(conflict.goalId1())).findFirst();
        Optional<LongTermGoal> goal2 = goals.stream()
                .filter(g -> g.getGoalId().equals(conflict.goalId2())).findFirst();

        String deferred;
        if (goal1.isPresent() && goal2.isPresent()) {
            deferred = goal1.get().getProgress() < goal2.get().getProgress() ?
                    conflict.goalId1() : conflict.goalId2();
        } else {
            deferred = conflict.goalId2();
        }

        return new ResolutionResult(true, conflict.goalId1(), deferred,
                "延迟解决: " + deferred + " 被暂时延迟");
    }

    private ResolutionResult resolveByCancel(Conflict conflict, List<LongTermGoal> goals) {
        return new ResolutionResult(true, conflict.goalId1(), conflict.goalId2(),
                "取消解决: " + conflict.goalId2() + " 被取消");
    }

    /**
     * 获取检测到的冲突列表
     */
    public List<Conflict> getDetectedConflicts() {
        return new ArrayList<>(detectedConflicts);
    }

    /**
     * 清除冲突历史
     */
    public void clearHistory() {
        detectedConflicts.clear();
    }
}
