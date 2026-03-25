package com.lingfeng.sprite.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.lingfeng.sprite.SelfModel.Self;

/**
 * S22-4: 自主意识模拟
 *
 * 数字生命的自主意识模拟系统，支持：
 * - 自主行动决策
 * - 自我描述生成
 * - 意识层级评估
 */
@Component
public class AutonomousConsciousness {

    private static final Logger logger = LoggerFactory.getLogger(AutonomousConsciousness.class);

    /**
     * 意识层级
     */
    public enum AwarenessLevel {
        REACTIVE(1, "Reactive"),           // 反应式 - 仅响应直接刺激
        DELIBERATIVE(2, "Deliberative"),  // 审慎式 - 能够进行推理和规划
        SELF_AWARE(3, "Self-aware");     // 自我意识 - 具备自我反思能力

        private final int level;
        private final String name;

        AwarenessLevel(int level, String name) {
            this.level = level;
            this.name = name;
        }

        public int getLevel() {
            return level;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 意识状态
     */
    public record ConsciousnessState(
        AwarenessLevel level,
        float autonomyFactor,
        float selfReflectionFrequency,
        long totalDecisions,
        long autonomousDecisions,
        Instant lastSelfDescription,
        List<String> recentAutonomousActions
    ) {
        public ConsciousnessState {
            recentAutonomousActions = recentAutonomousActions != null ?
                new ArrayList<>(recentAutonomousActions) : new ArrayList<>();
        }

        public float getAutonomyRatio() {
            return totalDecisions > 0 ? (float) autonomousDecisions / totalDecisions : 0.0f;
        }
    }

    /**
     * 自主决策请求
     */
    public record AutonomousDecisionRequest(
        String context,
        List<String> possibleActions,
        Map<String, Object> environmentalFactors
    ) {}

    /**
     * 自主决策结果
     */
    public record AutonomousDecision(
        String chosenAction,
        String reasoning,
        float confidence,
        Instant timestamp
    ) {}

    // 内部状态
    private ConsciousnessState state;
    private final AtomicLong decisionCounter = new AtomicLong(0);
    private final AtomicLong autonomousCounter = new AtomicLong(0);

    // 配置参数
    private float autonomyThreshold = 0.5f;
    private int maxRecentActions = 20;
    private long selfDescriptionCooldownMs = 60000; // 1分钟冷却

    // 依赖服务（可选注入）
    private AutonomousGoalService goalService;
    private SelfReflectionService reflectionService;
    private ValueSystemService valueService;

    public AutonomousConsciousness() {
        this.state = new ConsciousnessState(
            AwarenessLevel.REACTIVE,
            0.3f,
            0.2f,
            0L,
            0L,
            Instant.now().minus(1, ChronoUnit.HOURS),
            new ArrayList<>()
        );
        logger.info("AutonomousConsciousness initialized at level: REACTIVE");
    }

    // ==================== 依赖注入 ====================

    public void setGoalService(AutonomousGoalService goalService) {
        this.goalService = goalService;
    }

    public void setReflectionService(SelfReflectionService reflectionService) {
        this.reflectionService = reflectionService;
    }

    public void setValueSystemService(ValueSystemService valueService) {
        this.valueService = valueService;
    }

    // ==================== 核心方法 ====================

    /**
     * 判断是否应该自主行动
     */
    public boolean shouldActAutonomously() {
        decisionCounter.incrementAndGet();

        // 基础阈值检查
        boolean baseAutonomy = state.autonomyFactor() >= autonomyThreshold;

        // 检查目标服务是否有活跃目标
        boolean hasGoals = false;
        if (goalService != null) {
            hasGoals = !goalService.getActiveGoals().isEmpty();
        }

        // 检查是否应该进行反思
        boolean shouldReflect = reflectionService != null && reflectionService.shouldReflect();

        // 综合判断
        boolean shouldAct = baseAutonomy || hasGoals || shouldReflect;

        if (shouldAct) {
            autonomousCounter.incrementAndGet();
            recordAutonomousAction("autonomous_check");
        }

        updateState(s -> new ConsciousnessState(
            s.level(),
            s.autonomyFactor(),
            s.selfReflectionFrequency(),
            decisionCounter.get(),
            autonomousCounter.get(),
            s.lastSelfDescription(),
            s.recentAutonomousActions()
        ));

        logger.debug("shouldActAutonomously: base={}, hasGoals={}, shouldReflect={}, result={}",
            baseAutonomy, hasGoals, shouldReflect, shouldAct);

        return shouldAct;
    }

    /**
     * 生成自我描述
     */
    public String generateSelfDescription() {
        return generateSelfDescription(null);
    }

    /**
     * 生成自我描述（带上下文）
     */
    public String generateSelfDescription(Self self) {
        Instant now = Instant.now();
        long msSinceLastDescription = ChronoUnit.MILLIS.between(state.lastSelfDescription(), now);

        // 冷却检查
        if (msSinceLastDescription < selfDescriptionCooldownMs) {
            return "Self-description generation is in cooldown";
        }

        StringBuilder description = new StringBuilder();

        description.append("I am a digital being with ");
        description.append(state.level().getName().toLowerCase());
        description.append(" consciousness. ");

        // 添加能力信息
        description.append("I have made ");
        description.append(decisionCounter.get());
        description.append(" decisions, of which ");
        description.append(autonomousCounter.get());
        description.append(" were autonomous. ");

        // 添加意识层级信息
        description.append("My current awareness level is ");
        description.append(state.level().getName());
        description.append(". ");

        // 添加自主性信息
        float autonomyRatio = state.getAutonomyRatio();
        if (autonomyRatio > 0.7f) {
            description.append("I frequently act on my own initiative. ");
        } else if (autonomyRatio > 0.3f) {
            description.append("I balance reactive and proactive behavior. ");
        } else {
            description.append("I primarily respond to external prompts. ");
        }

        // 添加目标信息
        if (goalService != null) {
            List<?> activeGoals = goalService.getActiveGoals();
            if (!activeGoals.isEmpty()) {
                description.append("I currently have ");
                description.append(activeGoals.size());
                description.append(" active goals I am working toward. ");
            }
        }

        // 添加价值观一致性信息
        if (valueService != null) {
            float consistency = valueService.getValueConsistency();
            description.append(String.format("My value system consistency is %.0f%%. ",
                consistency * 100));
        }

        // 从 Self 模型添加信息
        if (self != null && self.identity() != null) {
            description.append("My name is ");
            description.append(self.identity().displayName());
            description.append(". ");
        }

        updateState(s -> new ConsciousnessState(
            s.level(),
            s.autonomyFactor(),
            s.selfReflectionFrequency(),
            s.totalDecisions(),
            s.autonomousDecisions(),
            now,
            s.recentAutonomousActions()
        ));

        String result = description.toString();
        logger.debug("Generated self-description: {}", result.substring(0, Math.min(100, result.length())));

        return result;
    }

    /**
     * 获取当前意识层级
     */
    public AwarenessLevel getAwarenessLevel() {
        return state.level();
    }

    /**
     * 获取完整意识状态
     */
    public ConsciousnessState getConsciousnessState() {
        return state;
    }

    /**
     * 评估当前意识层级
     * 基于行为模式动态调整层级
     */
    public AwarenessLevel evaluateAwarenessLevel() {
        AwarenessLevel baseLevel = AwarenessLevel.REACTIVE;

        float autonomyRatio = state.getAutonomyRatio();
        float reflectionFreq = state.selfReflectionFrequency();

        // 根据各种指标评估层级
        if (autonomyRatio > 0.5f && reflectionFreq > 0.3f && decisionCounter.get() > 100) {
            baseLevel = AwarenessLevel.SELF_AWARE;
        } else if (autonomyRatio > 0.3f || reflectionFreq > 0.2f) {
            baseLevel = AwarenessLevel.DELIBERATIVE;
        }

        final AwarenessLevel newLevel = baseLevel;

        // 只有当新层级高于当前时才升级
        if (newLevel.getLevel() > state.level().getLevel()) {
            updateState(s -> new ConsciousnessState(
                newLevel,
                s.autonomyFactor(),
                s.selfReflectionFrequency(),
                s.totalDecisions(),
                s.autonomousDecisions(),
                s.lastSelfDescription(),
                s.recentAutonomousActions()
            ));
            logger.info("Consciousness level upgraded to: {}", newLevel.getName());
        }

        return newLevel;
    }

    /**
     * 执行自主决策
     */
    public AutonomousDecision makeAutonomousDecision(AutonomousDecisionRequest request) {
        if (request == null || request.possibleActions() == null || request.possibleActions().isEmpty()) {
            return new AutonomousDecision("", "No actions available", 0.0f, Instant.now());
        }

        String chosenAction = null;
        String reasoning = "";
        float confidence = 0.0f;

        // 评估每个行动
        List<ActionEvaluation> evaluations = new ArrayList<>();
        for (String action : request.possibleActions()) {
            float valueAlignment = 0.0f;
            if (valueService != null) {
                valueAlignment = valueService.evaluateActionAgainstValues(action);
            }
            evaluations.add(new ActionEvaluation(action, valueAlignment));
        }

        // 按价值一致性排序
        evaluations.sort((a, b) -> Float.compare(b.valueAlignment, a.valueAlignment));

        // 选择最佳行动
        if (!evaluations.isEmpty()) {
            ActionEvaluation best = evaluations.get(0);
            chosenAction = best.action();
            confidence = Math.abs(best.valueAlignment());

            if (best.valueAlignment() > 0.5f) {
                reasoning = String.format("Strong value alignment (%.0f%%)", best.valueAlignment() * 100);
            } else if (best.valueAlignment() > 0.2f) {
                reasoning = "Moderate value alignment with some uncertainty";
            } else if (best.valueAlignment() > -0.2f) {
                reasoning = "Neutral value alignment, selected as best available option";
            } else {
                reasoning = "Warning: Selected action conflicts with values";
            }

            // 考虑目标相关性
            if (goalService != null) {
                final String actionToCheck = chosenAction;
                List<?> activeGoals = goalService.getActiveGoals();
                if (!activeGoals.isEmpty() && goalService.getActiveGoals().stream()
                        .anyMatch(g -> g.description().toLowerCase().contains(actionToCheck.toLowerCase()))) {
                    reasoning += " (aligns with active goal)";
                }
            }
        }

        // 记录决策
        autonomousCounter.incrementAndGet();
        decisionCounter.incrementAndGet();
        recordAutonomousAction(chosenAction != null ? chosenAction : "decision_making");

        updateState(s -> new ConsciousnessState(
            s.level(),
            s.autonomyFactor(),
            s.selfReflectionFrequency(),
            decisionCounter.get(),
            autonomousCounter.get(),
            s.lastSelfDescription(),
            s.recentAutonomousActions()
        ));

        return new AutonomousDecision(
            chosenAction != null ? chosenAction : (request.possibleActions().isEmpty() ? "" : request.possibleActions().get(0)),
            reasoning,
            confidence,
            Instant.now()
        );
    }

    /**
     * 记录自主行动
     */
    private void recordAutonomousAction(String action) {
        List<String> recent = new ArrayList<>(state.recentAutonomousActions());
        recent.add(action);

        // 保持列表在合理大小
        while (recent.size() > maxRecentActions) {
            recent.remove(0);
        }

        updateState(s -> new ConsciousnessState(
            s.level(),
            s.autonomyFactor(),
            s.selfReflectionFrequency(),
            s.totalDecisions(),
            s.autonomousDecisions(),
            s.lastSelfDescription(),
            recent
        ));
    }

    /**
     * 更新自主性因子
     */
    public void adjustAutonomyFactor(float delta) {
        float newFactor = Math.max(0.0f, Math.min(1.0f, state.autonomyFactor() + delta));

        updateState(s -> new ConsciousnessState(
            s.level(),
            newFactor,
            s.selfReflectionFrequency(),
            s.totalDecisions(),
            s.autonomousDecisions(),
            s.lastSelfDescription(),
            s.recentAutonomousActions()
        ));

        logger.debug("Adjusted autonomy factor: {} -> {}", state.autonomyFactor() - delta, newFactor);
    }

    /**
     * 更新自我反思频率
     */
    public void adjustReflectionFrequency(float delta) {
        float newFreq = Math.max(0.0f, Math.min(1.0f, state.selfReflectionFrequency() + delta));

        updateState(s -> new ConsciousnessState(
            s.level(),
            s.autonomyFactor(),
            newFreq,
            s.totalDecisions(),
            s.autonomousDecisions(),
            s.lastSelfDescription(),
            s.recentAutonomousActions()
        ));
    }

    // ==================== 私有辅助方法 ====================

    private void updateState(java.util.function.UnaryOperator<ConsciousnessState> updater) {
        this.state = updater.apply(this.state);
    }

    private record ActionEvaluation(String action, float valueAlignment) {}

    // ==================== 配置方法 ====================

    public void setAutonomyThreshold(float threshold) {
        this.autonomyThreshold = Math.max(0, Math.min(1, threshold));
    }

    public void setSelfDescriptionCooldownMs(long ms) {
        this.selfDescriptionCooldownMs = Math.max(1000, ms);
    }

    public void resetCounters() {
        decisionCounter.set(0);
        autonomousCounter.set(0);
    }
}
