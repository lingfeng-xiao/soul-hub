package com.lingfeng.sprite.domain.pacing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EvolutionPacingEngine - 进化节速引擎
 *
 * Sprite 学会为了主人而调节进化速度，而不是一味加速。
 * 决定哪些变化可以立即发生，哪些需要解释后发生，哪些需要主人确认。
 *
 * 对应 IGN-090
 *
 * 核心职责：
 * - 判断哪些变化可立即发生
 * - 哪些变化需解释后发生
 * - 哪些变化需主人确认
 */
public final class EvolutionPacingEngine {

    private static final Logger logger = LoggerFactory.getLogger(EvolutionPacingEngine.class);

    private final ChangeLayerClassifier classifier;
    private final List<PacingDecision> decisionHistory;
    private final List<PacingDecision> pendingDecisions;

    public EvolutionPacingEngine() {
        this.classifier = new ChangeLayerClassifier();
        this.decisionHistory = new ArrayList<>();
        this.pendingDecisions = new ArrayList<>();
    }

    /**
     * 对变化进行节速决策
     */
    public PacingDecision classify(String changeId, String changeType, String affectedCore, String description) {
        ChangeLayer layer = classifier.classify(changeType, affectedCore);
        ApplyStrategy strategy = determineStrategy(layer);
        String reason = classifier.getReason(layer);

        PacingDecision decision = new PacingDecision.Builder()
                .changeId(changeId)
                .layer(layer)
                .strategy(strategy)
                .reason(reason)
                .decidedAt(Instant.now())
                .changeDescription(description)
                .build();

        if (strategy == ApplyStrategy.CONFIRM_THEN_APPLY) {
            pendingDecisions.add(decision);
            logger.info("Change {} requires confirmation: {}", changeId, description);
        } else if (strategy == ApplyStrategy.EXPLAIN_THEN_APPLY) {
            pendingDecisions.add(decision);
            logger.info("Change {} needs explanation: {}", changeId, description);
        } else {
            decisionHistory.add(decision);
            logger.debug("Change {} classified as FAST, will apply immediately", changeId);
        }

        return decision;
    }

    /**
     * 根据层级确定应用策略
     */
    private ApplyStrategy determineStrategy(ChangeLayer layer) {
        return switch (layer) {
            case FAST -> ApplyStrategy.IMMEDIATE;
            case MEDIUM -> ApplyStrategy.EXPLAIN_THEN_APPLY;
            case SLOW -> ApplyStrategy.CONFIRM_THEN_APPLY;
        };
    }

    /**
     * 确认变化已被应用
     */
    public void confirmApplied(String changeId) {
        pendingDecisions.removeIf(d -> d.getChangeId().equals(changeId));
        logger.info("Change {} confirmed as applied", changeId);
    }

    /**
     * 放弃变化（拒绝或回退）
     */
    public void rejectChange(String changeId) {
        pendingDecisions.removeIf(d -> d.getChangeId().equals(changeId));
        logger.info("Change {} rejected/rolled back", changeId);
    }

    /**
     * 获取所有待处理决策
     */
    public List<PacingDecision> getPendingDecisions() {
        return new ArrayList<>(pendingDecisions);
    }

    /**
     * 获取需要解释的变化（尚未向主人说明）
     */
    public List<PacingDecision> getPendingExplanations() {
        return pendingDecisions.stream()
                .filter(PacingDecision::needsExplanation)
                .toList();
    }

    /**
     * 获取需要确认的变化（等待主人确认）
     */
    public List<PacingDecision> getPendingConfirmations() {
        return pendingDecisions.stream()
                .filter(PacingDecision::needsConfirmation)
                .toList();
    }

    /**
     * 获取最近的决策历史
     */
    public List<PacingDecision> getRecentHistory(int limit) {
        int size = decisionHistory.size();
        int from = Math.max(0, size - limit);
        return new ArrayList<>(decisionHistory.subList(from, size));
    }

    /**
     * 获取决策统计
     */
    public PacingStats getStats() {
        long fastCount = decisionHistory.stream()
                .filter(d -> d.getLayer() == ChangeLayer.FAST).count();
        long mediumCount = decisionHistory.stream()
                .filter(d -> d.getLayer() == ChangeLayer.MEDIUM).count();
        long slowCount = decisionHistory.stream()
                .filter(d -> d.getLayer() == ChangeLayer.SLOW).count();
        long pendingCount = pendingDecisions.size();

        return new PacingStats(fastCount, mediumCount, slowCount, pendingCount,
                decisionHistory.size());
    }

    /**
     * 生成变化解释文本
     */
    public String generateExplanation(PacingDecision decision) {
        return String.format(
                "我将进行以下变化：\n\n" +
                "**变化内容**：%s\n\n" +
                "**变化类型**：%s层\n\n" +
                "**原因**：%s\n\n" +
                "这个变化对您有什么影响？是否同意应用？",
                decision.getChangeDescription(),
                decision.getLayer().name().toLowerCase(),
                decision.getReason()
        );
    }

    /**
     * 节速统计记录
     */
    public record PacingStats(
            long fastChanges,
            long mediumChanges,
            long slowChanges,
            long pendingChanges,
            long totalChanges
    ) {}
}
