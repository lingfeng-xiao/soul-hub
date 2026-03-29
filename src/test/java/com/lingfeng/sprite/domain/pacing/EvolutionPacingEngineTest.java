package com.lingfeng.sprite.domain.pacing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EvolutionPacingEngine 单元测试
 *
 * 测试进化节速引擎的核心功能：
 * - 变化分层分类
 * - 应用策略确定
 * - 待处理决策管理
 * - 决策历史追踪
 * - 解释文本生成
 * - 统计信息
 */
class EvolutionPacingEngineTest {

    private EvolutionPacingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new EvolutionPacingEngine();
    }

    // ==================== 初始化测试 ====================

    @Test
    @DisplayName("默认构造应该创建空的引擎")
    void testDefaultConstruction_createsEmptyEngine() {
        assertTrue(engine.getPendingDecisions().isEmpty());
        assertTrue(engine.getRecentHistory(10).isEmpty());
    }

    @Test
    @DisplayName("getStats 应该在空引擎时返回零统计")
    void testGetStats_whenEmpty_returnsZeroCounts() {
        EvolutionPacingEngine.PacingStats stats = engine.getStats();

        assertEquals(0, stats.fastChanges());
        assertEquals(0, stats.mediumChanges());
        assertEquals(0, stats.slowChanges());
        assertEquals(0, stats.pendingChanges());
        assertEquals(0, stats.totalChanges());
    }

    // ==================== 分类测试 ====================

    @Test
    @DisplayName("classify FAST 层变化应该立即应用")
    void testClassify_fastLayer_changesAppliedImmediately() {
        PacingDecision decision = engine.classify(
                "change-1",
                "prompt_adjustment",
                "skill",
                "调整prompt参数"
        );

        assertEquals(ChangeLayer.FAST, decision.getLayer());
        assertEquals(ApplyStrategy.IMMEDIATE, decision.getStrategy());
        assertTrue(engine.getPendingDecisions().isEmpty());
    }

    @Test
    @DisplayName("classify MEDIUM 层变化应该需要解释")
    void testClassify_mediumLayer_needsExplanation() {
        PacingDecision decision = engine.classify(
                "change-2",
                "reminder_style",
                "collaboration",
                "调整提醒风格"
        );

        assertEquals(ChangeLayer.MEDIUM, decision.getLayer());
        assertEquals(ApplyStrategy.EXPLAIN_THEN_APPLY, decision.getStrategy());
        assertEquals(1, engine.getPendingDecisions().size());
        assertTrue(decision.needsExplanation());
    }

    @Test
    @DisplayName("classify SLOW 层变化应该需要确认")
    void testClassify_slowLayer_needsConfirmation() {
        PacingDecision decision = engine.classify(
                "change-3",
                "identity_narrative",
                "identity",
                "更新身份叙事"
        );

        assertEquals(ChangeLayer.SLOW, decision.getLayer());
        assertEquals(ApplyStrategy.CONFIRM_THEN_APPLY, decision.getStrategy());
        assertEquals(1, engine.getPendingDecisions().size());
        assertTrue(decision.needsConfirmation());
    }

    @Test
    @DisplayName("classify 未知核心应该默认为 FAST")
    void testClassify_unknownCore_defaultsToFast() {
        PacingDecision decision = engine.classify(
                "change-unknown",
                "some_change",
                "unknown_core",
                "未知变化"
        );

        assertEquals(ChangeLayer.FAST, decision.getLayer());
    }

    @Test
    @DisplayName("classify null 核心应该默认为 FAST")
    void testClassify_nullCore_defaultsToFast() {
        PacingDecision decision = engine.classify(
                "change-null",
                "some_change",
                null,
                "空核心变化"
        );

        assertEquals(ChangeLayer.FAST, decision.getLayer());
    }

    // ==================== 按核心分类测试 ====================

    @Test
    @DisplayName("classify identity 核心应该返回 SLOW")
    void testClassifyByCore_identity_returnsSlow() {
        PacingDecision decision = engine.classify(
                "change-id",
                "update",
                "identity",
                "身份更新"
        );

        assertEquals(ChangeLayer.SLOW, decision.getLayer());
    }

    @Test
    @DisplayName("classify self_narrative 核心应该返回 SLOW")
    void testClassifyByCore_selfNarrative_returnsSlow() {
        PacingDecision decision = engine.classify(
                "change-narr",
                "update",
                "self_narrative",
                "叙事更新"
        );

        assertEquals(ChangeLayer.SLOW, decision.getLayer());
    }

    @Test
    @DisplayName("classify goal 核心应该返回 MEDIUM")
    void testClassifyByCore_goal_returnsMedium() {
        PacingDecision decision = engine.classify(
                "change-goal",
                "update",
                "goal",
                "目标更新"
        );

        assertEquals(ChangeLayer.MEDIUM, decision.getLayer());
    }

    @Test
    @DisplayName("classify memory 核心应该返回 MEDIUM")
    void testClassifyByCore_memory_returnsMedium() {
        PacingDecision decision = engine.classify(
                "change-mem",
                "update",
                "memory",
                "记忆更新"
        );

        assertEquals(ChangeLayer.MEDIUM, decision.getLayer());
    }

    @Test
    @DisplayName("classify skill 核心应该返回 FAST")
    void testClassifyByCore_skill_returnsFast() {
        PacingDecision decision = engine.classify(
                "change-skill",
                "update",
                "skill",
                "技能更新"
        );

        assertEquals(ChangeLayer.FAST, decision.getLayer());
    }

    // ==================== 待处理决策管理测试 ====================

    @Test
    @DisplayName("getPendingDecisions 应该返回待处理决策")
    void testGetPendingDecisions_returnsPending() {
        engine.classify("change-1", "reminder_style", "collab", "调整提醒");
        engine.classify("change-2", "identity_narrative", "identity", "更新身份");

        assertEquals(2, engine.getPendingDecisions().size());
    }

    @Test
    @DisplayName("getPendingExplanations 应该只返回需要解释的决策")
    void testGetPendingExplanations_filtersCorrectly() {
        engine.classify("change-1", "reminder_style", "collab", "调整提醒"); // MEDIUM
        engine.classify("change-2", "identity_narrative", "identity", "更新身份"); // SLOW

        var explanations = engine.getPendingExplanations();

        assertEquals(1, explanations.size());
        assertEquals(ChangeLayer.MEDIUM, explanations.get(0).getLayer());
    }

    @Test
    @DisplayName("getPendingConfirmations 应该只返回需要确认的决策")
    void testGetPendingConfirmations_filtersCorrectly() {
        engine.classify("change-1", "reminder_style", "collab", "调整提醒"); // MEDIUM
        engine.classify("change-2", "identity_narrative", "identity", "更新身份"); // SLOW

        var confirmations = engine.getPendingConfirmations();

        assertEquals(1, confirmations.size());
        assertEquals(ChangeLayer.SLOW, confirmations.get(0).getLayer());
    }

    @Test
    @DisplayName("confirmApplied 应该移除待处理决策")
    void testConfirmApplied_removesPendingDecision() {
        engine.classify("change-1", "identity_narrative", "identity", "更新身份");
        assertEquals(1, engine.getPendingDecisions().size());

        engine.confirmApplied("change-1");

        assertTrue(engine.getPendingDecisions().isEmpty());
    }

    @Test
    @DisplayName("rejectChange 应该移除待处理决策")
    void testRejectChange_removesPendingDecision() {
        engine.classify("change-1", "identity_narrative", "identity", "更新身份");
        assertEquals(1, engine.getPendingDecisions().size());

        engine.rejectChange("change-1");

        assertTrue(engine.getPendingDecisions().isEmpty());
    }

    @Test
    @DisplayName("confirmApplied 应该忽略不存在的 ID")
    void testConfirmApplied_ignoresNonExistent() {
        assertDoesNotThrow(() -> engine.confirmApplied("non-existent"));
    }

    @Test
    @DisplayName("rejectChange 应该忽略不存在的 ID")
    void testRejectChange_ignoresNonExistent() {
        assertDoesNotThrow(() -> engine.rejectChange("non-existent"));
    }

    // ==================== 历史记录测试 ====================

    @Test
    @DisplayName("getRecentHistory 应该返回最近的决策")
    void testGetRecentHistory_returnsRecentDecisions() {
        // Create 5 FAST changes (they go to history immediately)
        for (int i = 0; i < 5; i++) {
            engine.classify("fast-" + i, "prompt", "skill", "快速变化" + i);
        }

        var history = engine.getRecentHistory(3);

        assertEquals(3, history.size());
    }

    @Test
    @DisplayName("getRecentHistory limit 大于历史长度应该返回全部")
    void testGetRecentHistory_limitExceedsSize_returnsAll() {
        engine.classify("fast-1", "prompt", "skill", "快速变化");
        engine.classify("fast-2", "prompt", "skill", "快速变化2");

        var history = engine.getRecentHistory(100);

        assertEquals(2, history.size());
    }

    @Test
    @DisplayName("getRecentHistory 在空历史时应该返回空列表")
    void testGetRecentHistory_emptyHistory_returnsEmptyList() {
        var history = engine.getRecentHistory(10);

        assertTrue(history.isEmpty());
    }

    // ==================== 统计测试 ====================

    @Test
    @DisplayName("getStats 应该正确统计各层变化")
    void testGetStats_countsCorrectly() {
        // FAST changes go directly to history
        engine.classify("fast-1", "prompt", "skill", "快速1");
        engine.classify("fast-2", "prompt", "skill", "快速2");

        // MEDIUM and SLOW changes stay pending (not in history)
        engine.classify("medium-1", "reminder_style", "collab", "中速1");
        engine.classify("slow-1", "identity_narrative", "identity", "慢速1");

        EvolutionPacingEngine.PacingStats stats = engine.getStats();

        assertEquals(2, stats.fastChanges()); // Only FAST is added to history
        assertEquals(0, stats.mediumChanges()); // MEDIUM stays pending
        assertEquals(0, stats.slowChanges()); // SLOW stays pending
        assertEquals(2, stats.pendingChanges()); // MEDIUM + SLOW
        assertEquals(2, stats.totalChanges()); // Only completed (FAST) ones
    }

    // ==================== 解释文本生成测试 ====================

    @Test
    @DisplayName("generateExplanation 应该生成格式化的解释")
    void testGenerateExplanation_formatsCorrectly() {
        PacingDecision decision = engine.classify(
                "change-1",
                "reminder_style",
                "collab",
                "调整提醒风格为更友好的方式"
        );

        String explanation = engine.generateExplanation(decision);

        assertNotNull(explanation);
        assertTrue(explanation.contains("调整提醒风格为更友好的方式"));
        assertTrue(explanation.contains("medium"));
        assertTrue(explanation.contains("解释")); // MEDIUM layer explains then applies
    }

    @Test
    @DisplayName("generateExplanation 应该包含变化描述")
    void testGenerateExplanation_containsChangeDescription() {
        PacingDecision decision = engine.classify(
                "change-1",
                "identity_narrative",
                "identity",
                "添加新的身份故事"
        );

        String explanation = engine.generateExplanation(decision);

        assertTrue(explanation.contains("添加新的身份故事"));
    }

    // ==================== 决策对象测试 ====================

    @Test
    @DisplayName("PacingDecision 应该正确存储所有字段")
    void testPacingDecision_storesAllFields() {
        PacingDecision decision = engine.classify(
                "test-change",
                "reminder_style",
                "collaboration",
                "测试变化"
        );

        assertEquals("test-change", decision.getChangeId());
        assertEquals(ChangeLayer.MEDIUM, decision.getLayer());
        assertNotNull(decision.getDecidedAt());
        assertEquals("测试变化", decision.getChangeDescription());
    }

    @Test
    @DisplayName("PacingDecision.needsExplanation 应该正确反映策略")
    void testPacingDecision_needsExplanation_reflectsStrategy() {
        PacingDecision mediumDecision = engine.classify(
                "medium-change", "reminder_style", "collab", "中速变化"
        );
        PacingDecision slowDecision = engine.classify(
                "slow-change", "identity_narrative", "identity", "慢速变化"
        );

        assertTrue(mediumDecision.needsExplanation());
        assertFalse(mediumDecision.needsConfirmation());

        assertFalse(slowDecision.needsExplanation());
        assertTrue(slowDecision.needsConfirmation());
    }

    // ==================== ChangeLayerClassifier 直接测试 ====================

    @Test
    @DisplayName("ChangeLayerClassifier 应该识别 FAST 关键词")
    void testClassifier_recognizesFastKeywords() {
        ChangeLayerClassifier classifier = new ChangeLayerClassifier();

        assertEquals(ChangeLayer.FAST, classifier.classify("update", "prompt_adjustment"));
        assertEquals(ChangeLayer.FAST, classifier.classify("update", "workflow_change"));
        assertEquals(ChangeLayer.FAST, classifier.classify("change", "task_strategy"));
    }

    @Test
    @DisplayName("ChangeLayerClassifier 应该识别 MEDIUM 关键词")
    void testClassifier_recognizesMediumKeywords() {
        ChangeLayerClassifier classifier = new ChangeLayerClassifier();

        assertEquals(ChangeLayer.MEDIUM, classifier.classify("update", "reminder_style_change"));
        assertEquals(ChangeLayer.MEDIUM, classifier.classify("update", "collaboration_rhythm"));
        assertEquals(ChangeLayer.MEDIUM, classifier.classify("update", "learning_support"));
    }

    @Test
    @DisplayName("ChangeLayerClassifier 应该识别 SLOW 关键词")
    void testClassifier_recognizesSlowKeywords() {
        ChangeLayerClassifier classifier = new ChangeLayerClassifier();

        assertEquals(ChangeLayer.SLOW, classifier.classify("update", "identity_narrative"));
        assertEquals(ChangeLayer.SLOW, classifier.classify("update", "personality_change"));
        assertEquals(ChangeLayer.SLOW, classifier.classify("update", "relationship_pattern"));
    }

    @Test
    @DisplayName("ChangeLayerClassifier.getReason 应该返回正确的理由")
    void testClassifier_getReason_returnsCorrectReason() {
        ChangeLayerClassifier classifier = new ChangeLayerClassifier();

        String fastReason = classifier.getReason(ChangeLayer.FAST);
        assertTrue(fastReason.contains("快速"));

        String mediumReason = classifier.getReason(ChangeLayer.MEDIUM);
        assertTrue(mediumReason.contains("解释"));

        String slowReason = classifier.getReason(ChangeLayer.SLOW);
        assertTrue(slowReason.contains("确认"));
    }
}
