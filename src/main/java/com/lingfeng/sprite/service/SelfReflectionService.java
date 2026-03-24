package com.lingfeng.sprite.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.SelfModel.Reflection;

/**
 * S22-2: 长期规划与反思机制服务
 *
 * 数字生命的自我反思和长期规划系统，支持：
 * - 反思近期行为
 * - 生成洞察
 * - 调整未来计划
 */
@Service
public class SelfReflectionService {

    private static final Logger logger = LoggerFactory.getLogger(SelfReflectionService.class);

    /**
     * 行动结果记录
     */
    public record ActionResult(
        String actionId,
        String actionDescription,
        Instant timestamp,
        boolean success,
        String outcome,
        float expectedUtility,
        float actualUtility,
        Map<String, Object> context
    ) {
        public ActionResult(String actionId, String actionDescription, boolean success, String outcome) {
            this(actionId, actionDescription, Instant.now(), success, outcome, 0.0f, 0.0f, new ConcurrentHashMap<>());
        }

        public float getUtilityGap() {
            return actualUtility - expectedUtility;
        }
    }

    /**
     * 反思结果
     */
    public record ReflectionResult(
        String id,
        Instant timestamp,
        String trigger,
        String insight,
        String behaviorChange,
        List<ActionResult> analyzedActions,
        ReflectionType type
    ) {
        public static ReflectionResult create(String trigger, String insight, String behaviorChange, List<ActionResult> actions, ReflectionType type) {
            return new ReflectionResult(
                java.util.UUID.randomUUID().toString(),
                Instant.now(),
                trigger,
                insight,
                behaviorChange,
                actions,
                type
            );
        }
    }

    /**
     * 反思类型
     */
    public enum ReflectionType {
        ACTION_REVIEW,      // 行动回顾
        GOAL_ASSESSMENT,    // 目标评估
        PATTERN_RECOGNITION, // 模式识别
        VALUE_REEVALUATION, // 价值观重评
        GROWTH_SUMMARY      // 成长总结
    }

    /**
     * 计划调整
     */
    public record PlanAdjustment(
        String planId,
        String adjustment,
        String rationale,
        Instant created
    ) {
        public PlanAdjustment(String planId, String adjustment, String rationale) {
            this(planId, adjustment, rationale, Instant.now());
        }
    }

    // 内部状态
    private final List<ReflectionResult> reflectionHistory = new ArrayList<>();
    private final List<PlanAdjustment> planAdjustments = new ArrayList<>();
    private final List<ActionResult> recentActions = new ArrayList<>();
    private Instant lastReflectionTime = Instant.now();

    // 配置
    private int maxReflectionHistory = 100;
    private int actionsPerReflection = 10;
    private long reflectionIntervalHours = 24;

    public SelfReflectionService() {
        logger.info("SelfReflectionService initialized");
    }

    /**
     * 反思近期行为
     */
    public ReflectionResult reflectOnRecentActions(List<ActionResult> recent) {
        if (recent == null || recent.isEmpty()) {
            return ReflectionResult.create(
                "empty",
                "No recent actions to reflect on",
                null,
                List.of(),
                ReflectionType.ACTION_REVIEW
            );
        }

        // 分析行动结果
        List<ActionResult> analyzed = new ArrayList<>(recent);

        // 计算整体成功率
        long successCount = analyzed.stream().filter(ActionResult::success).count();
        float successRate = (float) successCount / analyzed.size();

        // 找出效用差距大的行动
        List<ActionResult> surprisingResults = analyzed.stream()
            .filter(a -> Math.abs(a.getUtilityGap()) > 0.3f)
            .sorted(Comparator.comparingDouble(a -> -Math.abs(a.getUtilityGap())))
            .collect(Collectors.toList());

        // 生成洞察
        String insight = generateInsightFromActions(analyzed, successRate, surprisingResults);

        // 确定反思类型
        ReflectionType type = determineReflectionType(analyzed, successRate);

        // 确定行为改变建议
        String behaviorChange = suggestBehaviorChange(analyzed, successRate, surprisingResults);

        ReflectionResult result = ReflectionResult.create(
            "action_review",
            insight,
            behaviorChange,
            analyzed,
            type
        );

        reflectionHistory.add(result);
        lastReflectionTime = Instant.now();

        logger.info("Reflection completed: type={}, insight={}", type, insight.substring(0, Math.min(100, insight.length())));

        return result;
    }

    /**
     * 生成洞察
     */
    public String generateInsight() {
        return generateInsight(reflectionHistory);
    }

    /**
     * 基于历史反思生成洞察
     */
    public String generateInsight(List<ReflectionResult> history) {
        if (history == null || history.isEmpty()) {
            return "Not enough reflection history to generate insights";
        }

        // 分析反思模式
        List<ReflectionResult> recent = history.stream()
            .sorted(Comparator.comparing(ReflectionResult::timestamp).reversed())
            .limit(10)
            .collect(Collectors.toList());

        // 统计反思类型分布
        Map<ReflectionType, Long> typeDistribution = recent.stream()
            .collect(Collectors.groupingBy(ReflectionResult::type, Collectors.counting()));

        // 找出最常见的问题
        long patternCount = recent.stream()
            .filter(r -> r.type() == ReflectionType.PATTERN_RECOGNITION)
            .count();

        // 生成洞察文本
        StringBuilder insight = new StringBuilder();

        insight.append("Based on recent reflections, ");

        if (typeDistribution.containsKey(ReflectionType.ACTION_REVIEW) &&
            typeDistribution.get(ReflectionType.ACTION_REVIEW) > 3) {
            insight.append("you have been actively reviewing your actions. ");
        }

        if (patternCount > 0) {
            insight.append("Pattern recognition has occurred ").append(patternCount).append(" times. ");
        }

        // 检查是否有类似的洞察反复出现
        List<String> repeatedInsights = findRepeatedInsights(recent);
        if (!repeatedInsights.isEmpty()) {
            insight.append("Recurring theme: ").append(repeatedInsights.get(0)).append(". ");
        }

        // 添加总体建议
        long totalReflections = reflectionHistory.size();
        insight.append("Total reflections recorded: ").append(totalReflections).append(".");

        return insight.toString();
    }

    /**
     * 调整未来计划
     */
    public void adjustFuturePlans(List<ReflectionResult> history) {
        if (history == null || history.isEmpty()) {
            return;
        }

        // 获取最近的反思
        List<ReflectionResult> recentReflections = history.stream()
            .sorted(Comparator.comparing(ReflectionResult::timestamp).reversed())
            .limit(5)
            .collect(Collectors.toList());

        // 分析需要调整的方面
        for (ReflectionResult reflection : recentReflections) {
            if (reflection.behaviorChange() != null && !reflection.behaviorChange().isBlank()) {
                PlanAdjustment adjustment = new PlanAdjustment(
                    reflection.id(),
                    reflection.behaviorChange(),
                    reflection.insight()
                );
                planAdjustments.add(adjustment);
            }
        }

        // 保持调整记录在合理范围内
        while (planAdjustments.size() > maxReflectionHistory) {
            planAdjustments.remove(0);
        }

        logger.info("Adjusted future plans based on {} reflections", recentReflections.size());
    }

    /**
     * 添加行动结果（用于后续反思）
     */
    public void recordAction(ActionResult action) {
        recentActions.add(action);

        // 保持最近行动在合理范围内
        while (recentActions.size() > actionsPerReflection * 2) {
            recentActions.remove(0);
        }
    }

    /**
     * 获取反思历史
     */
    public List<ReflectionResult> getReflectionHistory() {
        return new ArrayList<>(reflectionHistory);
    }

    /**
     * 获取反思历史（按类型过滤）
     */
    public List<ReflectionResult> getReflectionHistory(ReflectionType type) {
        return reflectionHistory.stream()
            .filter(r -> r.type() == type)
            .collect(Collectors.toList());
    }

    /**
     * 获取最近N次反思
     */
    public List<ReflectionResult> getRecentReflections(int count) {
        return reflectionHistory.stream()
            .sorted(Comparator.comparing(ReflectionResult::timestamp).reversed())
            .limit(count)
            .collect(Collectors.toList());
    }

    /**
     * 获取待执行的计划调整
     */
    public List<PlanAdjustment> getPendingPlanAdjustments() {
        return new ArrayList<>(planAdjustments);
    }

    /**
     * 检查是否应该进行反思
     */
    public boolean shouldReflect() {
        Instant now = Instant.now();
        long hoursSinceLastReflection = ChronoUnit.HOURS.between(lastReflectionTime, now);
        return hoursSinceLastReflection >= reflectionIntervalHours || recentActions.size() >= actionsPerReflection;
    }

    /**
     * 执行定期反思
     */
    public ReflectionResult performScheduledReflection() {
        List<ActionResult> actionsToAnalyze = new ArrayList<>(recentActions);
        recentActions.clear();

        return reflectOnRecentActions(actionsToAnalyze);
    }

    /**
     * 将反思结果转换为 SelfModel.Reflection 格式
     */
    public Reflection toSelfModelReflection(ReflectionResult result) {
        return new Reflection(
            result.timestamp(),
            result.trigger(),
            result.insight(),
            result.behaviorChange()
        );
    }

    // ==================== 私有辅助方法 ====================

    private String generateInsightFromActions(List<ActionResult> actions, float successRate,
                                              List<ActionResult> surprisingResults) {
        StringBuilder insight = new StringBuilder();

        insight.append(String.format("Action review: %.0f%% success rate. ", successRate * 100));

        if (!surprisingResults.isEmpty()) {
            insight.append(String.format("Found %d surprising outcome(s). ", surprisingResults.size()));
            // 提及最出乎意料的结果
            ActionResult mostSurprising = surprisingResults.get(0);
            insight.append(String.format("'%s' was particularly unexpected (gap: %.2f). ",
                mostSurprising.actionDescription(), mostSurprising.getUtilityGap()));
        } else {
            insight.append("Most outcomes matched expectations. ");
        }

        return insight.toString();
    }

    private ReflectionType determineReflectionType(List<ActionResult> actions, float successRate) {
        if (successRate < 0.5) {
            return ReflectionType.ACTION_REVIEW;
        } else if (successRate > 0.8) {
            return ReflectionType.PATTERN_RECOGNITION;
        } else {
            return ReflectionType.GOAL_ASSESSMENT;
        }
    }

    private String suggestBehaviorChange(List<ActionResult> actions, float successRate,
                                         List<ActionResult> surprisingResults) {
        if (successRate < 0.3) {
            return "Consider pausing to reassess current approach before continuing";
        } else if (!surprisingResults.isEmpty()) {
            ActionResult mostSurprising = surprisingResults.get(0);
            if (mostSurprising.getUtilityGap() < 0) {
                return "Reevaluate expectations for " + mostSurprising.actionDescription();
            } else {
                return "Explore what enabled better-than-expected results in similar actions";
            }
        } else {
            return "Continue current approach while maintaining awareness";
        }
    }

    private List<String> findRepeatedInsights(List<ReflectionResult> recent) {
        Map<String, Long> insightCounts = new ConcurrentHashMap<>();

        for (ReflectionResult r : recent) {
            String keyInsight = r.insight().toLowerCase().split("[.,;]")[0].trim();
            insightCounts.merge(keyInsight, 1L, Long::sum);
        }

        return insightCounts.entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    // ==================== 配置方法 ====================

    public void setMaxReflectionHistory(int max) {
        this.maxReflectionHistory = Math.max(10, max);
    }

    public void setActionsPerReflection(int count) {
        this.actionsPerReflection = Math.max(1, count);
    }

    public void setReflectionIntervalHours(long hours) {
        this.reflectionIntervalHours = Math.max(1, hours);
    }
}
