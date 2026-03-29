package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * S22-1: 目标管理与自我驱动服务
 *
 * 数字生命的自主目标管理系统，支持：
 * - 创建和管理多层次目标
 * - 目标优先级排序
 * - 进度跟踪
 * - 自主行动选择
 */
@Service
public class AutonomousGoalService {

    private static final Logger logger = LoggerFactory.getLogger(AutonomousGoalService.class);

    /**
     * 目标优先级枚举
     */
    public enum GoalPriority {
        CRITICAL(1),    // 关键目标 - 必须立即处理
        HIGH(2),        // 高优先级 - 尽快完成
        MEDIUM(3),      // 中优先级 - 正常队列
        LOW(4);         // 低优先级 - 空闲时处理

        private final int order;

        GoalPriority(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    /**
     * 目标状态
     */
    public enum GoalStatus {
        ACTIVE,
        COMPLETED,
        ABANDONED,
        SUSPENDED
    }

    /**
     * 目标记录
     */
    public record Goal(
        String id,
        String description,
        GoalPriority priority,
        float progress,
        Instant created,
        Instant lastUpdated,
        List<String> subGoals,
        GoalStatus status,
        String category,
        Map<String, Object> metadata
    ) {
        public Goal(String id, String description, GoalPriority priority, float progress,
                    Instant created, List<String> subGoals) {
            this(id, description, priority, progress, created, Instant.now(),
                 subGoals, GoalStatus.ACTIVE, "general", new ConcurrentHashMap<>());
        }

        public Goal withProgress(float newProgress) {
            return new Goal(id, description, priority, newProgress, created,
                          Instant.now(), subGoals, status, category, metadata);
        }

        public Goal withStatus(GoalStatus newStatus) {
            return new Goal(id, description, priority, progress, created,
                          Instant.now(), subGoals, newStatus, category, metadata);
        }
    }

    /**
     * 目标选择结果
     */
    public record GoalSelection(
        List<Goal> selectedGoals,
        String reasoning
    ) {}

    // 内部状态
    private final Map<String, Goal> goals = new ConcurrentHashMap<>();
    private final Map<String, List<String>> goalDependencies = new ConcurrentHashMap<>();
    private Instant lastSelectionTime = Instant.now();

    // 配置参数
    private int maxActiveGoals = 10;
    private long selectionIntervalMs = 60000; // 1分钟

    public AutonomousGoalService() {
        logger.info("AutonomousGoalService initialized");
    }

    /**
     * S22-2: 定期生成自主目标
     *
     * 每5分钟评估当前状态并生成新的目标
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void generatePeriodicGoals() {
        logger.debug("Running periodic goal generation");

        // 检查是否已有活跃目标，避免生成过多目标
        List<Goal> activeGoals = getActiveGoals();
        if (activeGoals.size() >= maxActiveGoals) {
            logger.debug("Already have {} active goals, skipping goal generation", activeGoals.size());
            return;
        }

        // 生成一个通用的自主目标示例（实际实现可以根据上下文生成更有意义的目标）
        Goal newGoal = createGoal(
            "探索自我提升机会",
            GoalPriority.MEDIUM,
            List.of("评估当前技能", "识别学习机会", "制定提升计划"),
            "self_improvement"
        );

        logger.info("Generated periodic goal: id={}, description={}", newGoal.id(), newGoal.description());
    }

    /**
     * 创建新目标
     */
    public Goal createGoal(String description, GoalPriority priority) {
        return createGoal(description, priority, List.of(), "general");
    }

    /**
     * 创建新目标（带子目标）
     */
    public Goal createGoal(String description, GoalPriority priority, List<String> subGoals) {
        return createGoal(description, priority, subGoals, "general");
    }

    /**
     * 创建新目标（完整参数）
     */
    public Goal createGoal(String description, GoalPriority priority, List<String> subGoals, String category) {
        String id = UUID.randomUUID().toString();
        Goal goal = new Goal(id, description, priority, 0.0f, Instant.now(), Instant.now(), subGoals, GoalStatus.ACTIVE, category, new ConcurrentHashMap<>());
        goals.put(id, goal);

        // 建立依赖关系
        if (subGoals != null && !subGoals.isEmpty()) {
            goalDependencies.put(id, new ArrayList<>(subGoals));
        }

        logger.info("Created goal: id={}, description={}, priority={}", id, description, priority);
        return goal;
    }

    /**
     * 获取所有活动目标
     */
    public List<Goal> getActiveGoals() {
        return goals.values().stream()
            .filter(g -> g.status() == GoalStatus.ACTIVE)
            .sorted(Comparator
                .comparingInt((Goal g) -> g.priority().getOrder())
                .thenComparing(Goal::created))
            .collect(Collectors.toList());
    }

    /**
     * 获取指定目标
     */
    public Goal getGoal(String goalId) {
        return goals.get(goalId);
    }

    /**
     * 更新目标进度
     */
    public void updateProgress(String goalId, float progress) {
        Goal goal = goals.get(goalId);
        if (goal == null) {
            logger.warn("Attempted to update non-existent goal: {}", goalId);
            return;
        }

        float clampedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        Goal updatedGoal = goal.withProgress(clampedProgress);
        goals.put(goalId, updatedGoal);

        // 如果进度达到100%，自动标记为完成
        if (clampedProgress >= 1.0f) {
            completeGoal(goalId);
        } else {
            logger.debug("Updated goal progress: id={}, progress={}", goalId, clampedProgress);
        }
    }

    /**
     * 标记目标为完成
     */
    public void completeGoal(String goalId) {
        Goal goal = goals.get(goalId);
        if (goal == null) {
            logger.warn("Attempted to complete non-existent goal: {}", goalId);
            return;
        }

        Goal completedGoal = goal.withStatus(GoalStatus.COMPLETED).withProgress(1.0f);
        goals.put(goalId, completedGoal);
        logger.info("Goal completed: id={}, description={}", goalId, goal.description());
    }

    /**
     * 放弃目标
     */
    public void abandonGoal(String goalId) {
        Goal goal = goals.get(goalId);
        if (goal == null) {
            logger.warn("Attempted to abandon non-existent goal: {}", goalId);
            return;
        }

        Goal abandonedGoal = goal.withStatus(GoalStatus.ABANDONED);
        goals.put(goalId, abandonedGoal);
        logger.info("Goal abandoned: id={}, description={}", goalId, goal.description());
    }

    /**
     * 挂起目标
     */
    public void suspendGoal(String goalId) {
        Goal goal = goals.get(goalId);
        if (goal == null) {
            logger.warn("Attempted to suspend non-existent goal: {}", goalId);
            return;
        }

        Goal suspendedGoal = goal.withStatus(GoalStatus.SUSPENDED);
        goals.put(goalId, suspendedGoal);
        logger.debug("Goal suspended: id={}", goalId);
    }

    /**
     * 恢复挂起的目标
     */
    public void resumeGoal(String goalId) {
        Goal goal = goals.get(goalId);
        if (goal == null) {
            logger.warn("Attempted to resume non-existent goal: {}", goalId);
            return;
        }

        if (goal.status() == GoalStatus.SUSPENDED) {
            Goal resumedGoal = goal.withStatus(GoalStatus.ACTIVE);
            goals.put(goalId, resumedGoal);
            logger.debug("Goal resumed: id={}", goalId);
        }
    }

    /**
     * 选择下一个应该执行的目标
     * 基于优先级、进度和时间因素进行选择
     */
    public GoalSelection selectNextActions() {
        return selectNextActions(maxActiveGoals);
    }

    /**
     * 选择下一个应该执行的目标（指定数量）
     */
    public GoalSelection selectNextActions(int maxGoals) {
        Instant now = Instant.now();
        long msSinceLastSelection = java.time.temporal.ChronoUnit.MILLIS.between(lastSelectionTime, now);

        // 基于时间衰减调整优先级
        List<Goal> candidates = getActiveGoals();

        if (candidates.isEmpty()) {
            lastSelectionTime = now;
            return new GoalSelection(List.of(), "No active goals");
        }

        // 计算每个目标的动态优先级分数
        List<Goal> scoredGoals = candidates.stream()
            .map(this::calculateDynamicPriority)
            .sorted(Comparator.comparingDouble((GoalWithScore g) -> g.score).reversed())
            .limit(maxGoals)
            .map(GoalWithScore::goal)
            .collect(Collectors.toList());

        String reasoning = String.format("Selected %d goals from %d candidates (interval: %dms)",
            scoredGoals.size(), candidates.size(), msSinceLastSelection);

        lastSelectionTime = now;
        logger.debug("Goal selection: {}", reasoning);

        return new GoalSelection(scoredGoals, reasoning);
    }

    /**
     * 内部类：带分数的目标
     */
    private record GoalWithScore(Goal goal, double score) {}

    /**
     * 计算目标的动态优先级分数
     * 考虑因素：基础优先级、进度、时间衰减
     */
    private GoalWithScore calculateDynamicPriority(Goal goal) {
        // 基础分数：优先级越高分数越高
        double baseScore = (5 - goal.priority().getOrder()) * 20.0;

        // 进度因素：进度在30%-70%之间的目标给予额外分数（正在进行中）
        double progressBonus = 0.0;
        if (goal.progress() > 0.3 && goal.progress() < 0.7) {
            progressBonus = 10.0;
        } else if (goal.progress() >= 0.7 && goal.progress() < 1.0) {
            progressBonus = 15.0; // 快完成的目标优先
        }

        // 时间衰减：如果目标很久没更新，适当提升优先级
        long hoursSinceUpdate = java.time.temporal.ChronoUnit.HOURS.between(goal.lastUpdated(), Instant.now());
        double timeBonus = Math.min(hoursSinceUpdate * 2.0, 20.0);

        double totalScore = baseScore + progressBonus + timeBonus;

        return new GoalWithScore(goal, totalScore);
    }

    /**
     * 获取目标统计信息
     */
    public Map<String, Object> getGoalStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("total", goals.size());
        stats.put("active", goals.values().stream().filter(g -> g.status() == GoalStatus.ACTIVE).count());
        stats.put("completed", goals.values().stream().filter(g -> g.status() == GoalStatus.COMPLETED).count());
        stats.put("abandoned", goals.values().stream().filter(g -> g.status() == GoalStatus.ABANDONED).count());
        stats.put("suspended", goals.values().stream().filter(g -> g.status() == GoalStatus.SUSPENDED).count());
        return stats;
    }

    /**
     * 清理已删除的目标
     */
    public void pruneCompletedGoals() {
        List<String> toRemove = goals.entrySet().stream()
            .filter(e -> e.getValue().status() == GoalStatus.COMPLETED ||
                        e.getValue().status() == GoalStatus.ABANDONED)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        toRemove.forEach(goals::remove);
        toRemove.forEach(goalDependencies::remove);

        if (!toRemove.isEmpty()) {
            logger.info("Pruned {} completed/abandoned goals", toRemove.size());
        }
    }

    /**
     * 设置最大活动目标数
     */
    public void setMaxActiveGoals(int max) {
        this.maxActiveGoals = Math.max(1, max);
    }

    /**
     * 设置选择间隔
     */
    public void setSelectionIntervalMs(long ms) {
        this.selectionIntervalMs = Math.max(1000, ms);
    }
}
