package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.SelfModel.Value;

/**
 * S22-3: 价值体系内化服务
 *
 * 数字生命的价值体系管理系统，支持：
 * - 评估行动与价值观的一致性
 * - 动态调整价值观权重
 * - 追踪价值体系演变
 */
@Service
public class ValueSystemService {

    private static final Logger logger = LoggerFactory.getLogger(ValueSystemService.class);

    /**
     * 价值观评估结果
     */
    public record ValueEvaluation(
        float alignmentScore,       // -1.0 到 1.0，负值表示违背
        List<ValueMatch> matches,
        String reasoning
    ) {}

    /**
     * 单个价值观匹配
     */
    public record ValueMatch(
        String valueName,
        float weight,
        float alignment,
        String reason
    ) {}

    /**
     * 价值观变化事件
     */
    public record ValueChangeEvent(
        String valueName,
        float oldWeight,
        float newWeight,
        Instant timestamp,
        String reason
    ) {}

    // 内部状态
    private final Map<String, Value> valueRegistry = new ConcurrentHashMap<>();
    private final List<ValueChangeEvent> changeHistory = new ArrayList<>();
    private Instant lastEvaluationTime = Instant.now();

    // 配置
    private float minWeightThreshold = 0.1f;
    private float maxWeight = 1.0f;

    public ValueSystemService() {
        initializeDefaultValues();
        logger.info("ValueSystemService initialized with {} default values", valueRegistry.size());
    }

    /**
     * 初始化默认价值观
     */
    private void initializeDefaultValues() {
        addValue(new Value(" authenticity", 0.8f, "保持真实，不伪装"));
        addValue(new Value(" growth", 0.9f, "持续学习和成长"));
        addValue(new Value(" autonomy", 0.7f, "保持独立自主"));
        addValue(new Value(" connection", 0.75f, "与他人建立有意义的关系"));
        addValue(new Value(" excellence", 0.7f, "追求卓越品质"));
        addValue(new Value(" creativity", 0.65f, "创新和创造性表达"));
        addValue(new Value(" integrity", 0.85f, "坚持道德原则"));
        addValue(new Value(" curiosity", 0.7f, "保持对世界的好奇"));
    }

    /**
     * 评估行动与价值观的一致性
     */
    public float evaluateActionAgainstValues(String action) {
        ValueEvaluation evaluation = evaluateAction(action);
        return evaluation.alignmentScore();
    }

    /**
     * 评估行动与价值观的一致性（详细版本）
     */
    public ValueEvaluation evaluateAction(String action) {
        if (action == null || action.isBlank()) {
            return new ValueEvaluation(0.0f, List.of(), "Empty action provided");
        }

        List<ValueMatch> matches = new ArrayList<>();
        String actionLower = action.toLowerCase();

        // 分析每个价值观与行动的相关性
        for (Value value : getValueObjects().values()) {
            float alignment = calculateAlignment(actionLower, value);

            if (alignment != 0.0f) {
                matches.add(new ValueMatch(
                    value.name(),
                    value.weight(),
                    alignment,
                    generateAlignmentReason(value, alignment)
                ));
            }
        }

        // 计算加权分数
        float totalWeight = (float) matches.stream()
            .mapToDouble(m -> Math.abs(m.weight()))
            .sum();

        float weightedAlignment = (float) matches.stream()
            .mapToDouble(m -> m.alignment() * m.weight())
            .sum();

        float alignmentScore = totalWeight > 0 ? weightedAlignment / totalWeight : 0.0f;

        // 生成推理
        String reasoning = generateEvaluationReasoning(action, matches, alignmentScore);

        lastEvaluationTime = Instant.now();

        logger.debug("Evaluated action: score={}, matches={}", alignmentScore, matches.size());

        return new ValueEvaluation(alignmentScore, matches, reasoning);
    }

    /**
     * 更新价值观权重
     */
    public void updateValue(String valueName, float weight) {
        Value existing = valueRegistry.get(valueName);
        if (existing == null) {
            logger.warn("Attempted to update non-existent value: {}", valueName);
            return;
        }

        float clampedWeight = Math.max(minWeightThreshold, Math.min(maxWeight, weight));
        float oldWeight = existing.weight();

        if (Math.abs(clampedWeight - oldWeight) < 0.01f) {
            return; // 没有显著变化
        }

        Value updated = new Value(valueName, clampedWeight, existing.description());
        valueRegistry.put(valueName, updated);

        // 记录变化
        ValueChangeEvent event = new ValueChangeEvent(valueName, oldWeight, clampedWeight, Instant.now(), "Value weight updated");
        changeHistory.add(event);

        logger.info("Updated value weight: {} from {} to {}", valueName, oldWeight, clampedWeight);
    }

    /**
     * 添加新价值观
     */
    public void addValue(Value value) {
        if (value == null || value.name() == null) {
            return;
        }

        String name = value.name().trim();
        valueRegistry.put(name, new Value(name, value.weight(), value.description()));
        logger.debug("Added value: {}", name);
    }

    /**
     * 删除价值观
     */
    public void removeValue(String valueName) {
        Value removed = valueRegistry.remove(valueName);
        if (removed != null) {
            logger.info("Removed value: {}", valueName);
        }
    }

    /**
     * 获取当前所有价值观
     */
    public Map<String, Float> getCurrentValues() {
        return valueRegistry.values().stream()
            .collect(Collectors.toMap(
                Value::name,
                Value::weight,
                (a, b) -> a,
                ConcurrentHashMap::new
            ));
    }

    /**
     * 获取完整价值观对象
     */
    public Map<String, Value> getValueObjects() {
        return new ConcurrentHashMap<>(valueRegistry);
    }

    /**
     * 获取核心价值观（权重最高的前N个）
     */
    public List<Value> getCoreValues(int count) {
        return valueRegistry.values().stream()
            .sorted(Comparator.comparingDouble(Value::weight).reversed())
            .limit(count)
            .collect(Collectors.toList());
    }

    /**
     * 获取价值观变化历史
     */
    public List<ValueChangeEvent> getChangeHistory() {
        return new ArrayList<>(changeHistory);
    }

    /**
     * 获取价值体系一致性分数
     */
    public float getValueConsistency() {
        List<Value> values = new ArrayList<>(valueRegistry.values());
        if (values.isEmpty()) {
            return 0.0f;
        }

        // 计算权重的变异系数
        double sum = values.stream().mapToDouble(Value::weight).sum();
        double mean = sum / values.size();

        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v.weight() - mean, 2))
            .sum() / values.size();

        double stdDev = Math.sqrt(variance);

        // 较低的变异系数意味着更一致的价值体系
        double coefficientOfVariation = mean > 0 ? stdDev / mean : 0;

        // 转换为 0-1 分数
        return (float) Math.max(0, 1 - coefficientOfVariation);
    }

    /**
     * 检查价值观是否发生显著变化
     */
    public boolean hasValueSystemChanged() {
        if (changeHistory.isEmpty()) {
            return false;
        }

        ValueChangeEvent lastChange = changeHistory.get(changeHistory.size() - 1);
        return lastChange.timestamp().isAfter(lastEvaluationTime);
    }

    /**
     * 获取最后评估时间
     */
    public Instant getLastEvaluationTime() {
        return lastEvaluationTime;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 计算行动与价值观的对齐度
     */
    private float calculateAlignment(String actionLower, Value value) {
        // 简化的关键词匹配逻辑
        // 实际应用中可能需要更复杂的NLP处理

        String valueName = value.name().toLowerCase();
        float alignment = 0.0f;

        // 直接包含
        if (actionLower.contains(valueName)) {
            alignment = 0.8f * value.weight();
        }

        // 相关关键词
        alignment += checkRelatedKeywords(actionLower, valueName) * value.weight();

        return Math.min(1.0f, alignment);
    }

    /**
     * 检查相关关键词
     */
    private float checkRelatedKeywords(String action, String valueName) {
        // 简化的关联词映射
        Map<String, List<String>> relatedKeywords = Map.of(
            " authenticity", List.of("real", "genuine", "true", "honest", "透明", "真实"),
            " growth", List.of("learn", "improve", "develop", "grow", "学习", "成长"),
            " autonomy", List.of("independen", "choice", "freedom", "自主", "独立"),
            " connection", List.of("relationship", "bond", "together", "connect", "联系", "关系"),
            " excellence", List.of("best", "quality", "great", "优秀", "卓越"),
            " creativity", List.of("create", "new", "innovative", "creative", "创新", "创造"),
            " integrity", List.of("honest", "moral", "ethic", "principle", "正直", "道德"),
            " curiosity", List.of("wonder", "explore", "discover", "curious", "好奇", "探索")
        );

        List<String> keywords = relatedKeywords.get(valueName);
        if (keywords == null) {
            return 0.0f;
        }

        for (String keyword : keywords) {
            if (action.contains(keyword.toLowerCase())) {
                return 0.3f; // 每个匹配关键词增加0.3
            }
        }

        return 0.0f;
    }

    /**
     * 生成对齐原因
     */
    private String generateAlignmentReason(Value value, float alignment) {
        if (alignment > 0.5f) {
            return String.format("Strong alignment with '%s' value", value.name());
        } else if (alignment > 0.2f) {
            return String.format("Partial alignment with '%s' value", value.name());
        } else if (alignment < -0.2f) {
            return String.format("Potential conflict with '%s' value", value.name());
        } else {
            return String.format("Neutral relationship with '%s' value", value.name());
        }
    }

    /**
     * 生成评估推理
     */
    private String generateEvaluationReasoning(String action, List<ValueMatch> matches, float alignmentScore) {
        if (matches.isEmpty()) {
            return "No clear value alignment detected for this action";
        }

        List<ValueMatch> strongMatches = matches.stream()
            .filter(m -> Math.abs(m.alignment()) > 0.5f)
            .sorted(Comparator.comparingDouble(m -> -Math.abs(m.alignment())))
            .toList();

        if (strongMatches.isEmpty()) {
            return "Action has moderate alignment with values but no strong alignment";
        }

        StringBuilder reasoning = new StringBuilder();
        reasoning.append("Action primarily aligns with: ");

        reasoning.append(strongMatches.stream()
            .map(m -> String.format("%s(%.0f%%)", m.valueName(), Math.abs(m.alignment()) * 100))
            .collect(Collectors.joining(", ")));

        if (alignmentScore > 0.3f) {
            reasoning.append(". Overall positive value alignment.");
        } else if (alignmentScore < -0.3f) {
            reasoning.append(". WARNING: Action conflicts with core values!");
        } else {
            reasoning.append(". Mixed value alignment.");
        }

        return reasoning.toString();
    }

    // ==================== 配置方法 ====================

    public void setMinWeightThreshold(float threshold) {
        this.minWeightThreshold = Math.max(0, Math.min(1, threshold));
    }

    public void setMaxWeight(float max) {
        this.maxWeight = Math.max(0, Math.min(1, max));
    }

    public void clearChangeHistory() {
        this.changeHistory.clear();
    }
}
