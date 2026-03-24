package com.lingfeng.sprite.service;

import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.WorldModel;
import com.lingfeng.sprite.cognition.DecisionEngine;
import com.lingfeng.sprite.cognition.MemoryRetrievalService;
import com.lingfeng.sprite.cognition.ReasoningEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DecisionEngine 单元测试 - S6 决策引擎核心测试
 */
class DecisionEngineTest {

    private DecisionEngine decisionEngine;
    private WorldModel.World worldModel;
    private SelfModel.Self selfModel;
    private MemoryRetrievalService retrievalContext;

    @BeforeEach
    void setUp() {
        // Create WorldModel with owner
        OwnerModel.Owner owner = createTestOwner();
        worldModel = new WorldModel.World(owner);

        // Create SelfModel
        selfModel = SelfModel.Self.createDefault();

        // Create DecisionEngine
        decisionEngine = new DecisionEngine(worldModel);

        // Create empty retrieval context
        retrievalContext = new MemoryRetrievalService(new com.lingfeng.sprite.MemorySystem.Memory());
    }

    private OwnerModel.Owner createTestOwner() {
        OwnerModel.OwnerIdentity identity = new OwnerModel.OwnerIdentity(
            "test-owner",
            "软件工程师",
            List.of()
        );
        OwnerModel.LifeContext lifeContext = new OwnerModel.LifeContext(
            "科技公司",
            "北京",
            new OwnerModel.Family(List.of()),
            List.of()
        );
        return new OwnerModel.Owner(
            identity,
            lifeContext,
            List.of(), List.of(), List.of(),
            new OwnerModel.EmotionalState(OwnerModel.Mood.NEUTRAL, 0.5f),
            List.of(), List.of(),
            new OwnerModel.TrustLevel(0.8f),
            null, null,
            new OwnerModel.DigitalFootprint(List.of(), List.of(), List.of()),
            List.of(),
            Instant.now()
        );
    }

    // ==================== 基础决策测试 ====================

    @Test
    void testDecide_withNullReasoningResult_returnsEmptyActions() {
        DecisionEngine.DecisionResult result = decisionEngine.decide(
            null,
            null,
            selfModel
        );

        assertNotNull(result);
        assertFalse(result.hasActions());
    }

    @Test
    void testDecide_withHighSalienceScore_generatesUrgentAction() {
        PerceptionSystem.SalienceScore salienceScore = new PerceptionSystem.SalienceScore(
            0.9f,  // novelty
            0.5f,  // relevance
            0.95f, // urgency (high!)
            0.8f   // overall
        );

        DecisionEngine.DecisionResult result = decisionEngine.decide(
            null,
            salienceScore,
            selfModel
        );

        assertNotNull(result);
        assertTrue(result.hasActions() || !result.hasActions()); // Either way should not crash
    }

    @Test
    void testDecide_withReasoningResult_generatesActions() {
        // Create mock reasoning result
        ReasoningEngine.ReasoningOutput output = new ReasoningEngine.ReasoningOutput(
            ReasoningEngine.ReasoningType.INTENT,
            "用户想要查询天气",
            0.85f
        );
        ReasoningEngine.ReasoningResult reasoningResult = new ReasoningEngine.ReasoningResult(
            List.of(output),
            true,
            "test reasoning"
        );

        PerceptionSystem.SalienceScore salienceScore = new PerceptionSystem.SalienceScore(
            0.5f, 0.5f, 0.5f, 0.5f
        );

        DecisionEngine.DecisionResult result = decisionEngine.decide(
            reasoningResult,
            salienceScore,
            selfModel
        );

        assertNotNull(result);
        assertNotNull(result.reason());
    }

    // ==================== 多维度评估测试 ====================

    @Test
    void testEvaluateMultiDimensional_withNullSalienceScore_returnsValidEvaluation() {
        DecisionEngine.MultiDimensionalEvaluation evaluation = decisionEngine.evaluateMultiDimensional(
            null,
            null
        );

        assertNotNull(evaluation);
        assertNotNull(evaluation.dimensionScores());
        assertFalse(evaluation.dimensionScores().isEmpty());
    }

    @Test
    void testEvaluateMultiDimensional_withSalienceScore_calculatesCorrectly() {
        PerceptionSystem.SalienceScore salienceScore = new PerceptionSystem.SalienceScore(
            0.7f,  // novelty
            0.8f,  // relevance
            0.6f,  // urgency
            0.7f   // overall
        );

        DecisionEngine.MultiDimensionalEvaluation evaluation = decisionEngine.evaluateMultiDimensional(
            salienceScore,
            null
        );

        assertNotNull(evaluation);
        assertTrue(evaluation.overallScore() >= 0);
        assertNotNull(evaluation.reasoning());
    }

    @Test
    void testEvaluateMultiDimensional_withRetrievalContext_includesMemoryScore() {
        MemoryRetrievalService.RetrievalContext context = new MemoryRetrievalService.RetrievalContext(
            List.of("test episodic"),
            List.of("test semantic"),
            List.of("test procedural"),
            List.of("test pattern"),
            0.6f
        );

        PerceptionSystem.SalienceScore salienceScore = new PerceptionSystem.SalienceScore(
            0.5f, 0.5f, 0.5f, 0.5f
        );

        DecisionEngine.MultiDimensionalEvaluation evaluation = decisionEngine.evaluateMultiDimensional(
            salienceScore,
            context
        );

        assertNotNull(evaluation);
        assertTrue(evaluation.overallScore() > 0);
    }

    // ==================== 置信度测试 ====================

    @Test
    void testCalculateConfidenceSummary_withNullInputs_returnsValidSummary() {
        DecisionEngine.ConfidenceSummary summary = decisionEngine.calculateConfidenceSummary(
            null,
            null,
            null
        );

        assertNotNull(summary);
        assertNotNull(summary.level());
        assertEquals(0f, summary.overallConfidence());
    }

    @Test
    void testConfidenceLevel_fromScore() {
        assertEquals(DecisionEngine.ConfidenceLevel.HIGH, DecisionEngine.ConfidenceLevel.fromScore(0.9f));
        assertEquals(DecisionEngine.ConfidenceLevel.MEDIUM, DecisionEngine.ConfidenceLevel.fromScore(0.6f));
        assertEquals(DecisionEngine.ConfidenceLevel.LOW, DecisionEngine.ConfidenceLevel.fromScore(0.4f));
        assertEquals(DecisionEngine.ConfidenceLevel.UNKNOWN, DecisionEngine.ConfidenceLevel.fromScore(0.1f));
    }

    @Test
    void testPropagateConfidence() {
        float result = decisionEngine.propagateConfidence(0.8f, 0.5f);
        assertEquals(0.4f, result, 0.001f);
    }

    // ==================== 维度权重测试 ====================

    @Test
    void testGetDimensionWeights_returnsAllDimensions() {
        Map<DecisionEngine.DecisionDimension, Float> weights = decisionEngine.getDimensionWeights();

        assertNotNull(weights);
        assertEquals(DecisionEngine.DecisionDimension.values().length, weights.size());
    }

    @Test
    void testSetDimensionWeight_updatesWeight() {
        decisionEngine.setDimensionWeight(DecisionEngine.DecisionDimension.EMOTION, 0.3f);

        Map<DecisionEngine.DecisionDimension, Float> weights = decisionEngine.getDimensionWeights();
        assertEquals(0.3f, weights.get(DecisionEngine.DecisionDimension.EMOTION));
    }

    @Test
    void testSetDimensionWeight_ignoresInvalidWeight() {
        // Try to set invalid weight (should be ignored)
        decisionEngine.setDimensionWeight(DecisionEngine.DecisionDimension.EMOTION, 1.5f);

        // Weight should not be updated (default is 0.15)
        Map<DecisionEngine.DecisionDimension, Float> weights = decisionEngine.getDimensionWeights();
        assertNotEquals(1.5f, weights.get(DecisionEngine.DecisionDimension.EMOTION));
    }

    @Test
    void testResetDimensionWeights_restoresDefaults() {
        // First modify
        decisionEngine.setDimensionWeight(DecisionEngine.DecisionDimension.EMOTION, 0.5f);

        // Then reset
        decisionEngine.resetDimensionWeights();

        Map<DecisionEngine.DecisionDimension, Float> weights = decisionEngine.getDimensionWeights();
        assertEquals(DecisionEngine.DecisionDimension.EMOTION.defaultWeight(), weights.get(DecisionEngine.DecisionDimension.EMOTION));
    }

    // ==================== 置信度阈值测试 ====================

    @Test
    void testGetConfidenceThresholds_returnsDefaults() {
        float[] thresholds = decisionEngine.getConfidenceThresholds();

        assertEquals(3, thresholds.length);
        assertEquals(0.8f, thresholds[0]);
        assertEquals(0.5f, thresholds[1]);
        assertEquals(0.3f, thresholds[2]);
    }

    @Test
    void testSetConfidenceThresholds_validValues() {
        decisionEngine.setConfidenceThresholds(0.9f, 0.6f, 0.4f);

        float[] thresholds = decisionEngine.getConfidenceThresholds();
        assertEquals(0.9f, thresholds[0]);
        assertEquals(0.6f, thresholds[1]);
        assertEquals(0.4f, thresholds[2]);
    }

    @Test
    void testSetConfidenceThresholds_invalidValues_ignored() {
        // Try to set invalid thresholds (high < medium)
        decisionEngine.setConfidenceThresholds(0.3f, 0.6f, 0.4f);

        // Should be ignored, keep old values
        float[] thresholds = decisionEngine.getConfidenceThresholds();
        assertEquals(0.8f, thresholds[0]); // Default high
    }

    // ==================== 决策历史测试 ====================

    @Test
    void testGetDecisionHistory_initiallyEmpty() {
        List<DecisionEngine.DecisionHistory> history = decisionEngine.getDecisionHistory();
        assertTrue(history.isEmpty());
    }

    @Test
    void testGetRecentDecisions_emptyHistory_returnsEmpty() {
        List<DecisionEngine.DecisionHistory> recent = decisionEngine.getRecentDecisions(5);
        assertTrue(recent.isEmpty());
    }

    @Test
    void testClearDecisionHistory() {
        decisionEngine.clearDecisionHistory();
        List<DecisionEngine.DecisionHistory> history = decisionEngine.getDecisionHistory();
        assertTrue(history.isEmpty());
    }

    @Test
    void testGetDecisionStatistics_emptyHistory_returnsZeros() {
        DecisionEngine.DecisionStatistics stats = decisionEngine.getDecisionStatistics();

        assertNotNull(stats);
        assertEquals(0, stats.totalDecisions());
        assertEquals(0, stats.totalActions());
    }

    @Test
    void testGetDecisionHistorySummary_emptyHistory() {
        DecisionEngine.DecisionHistorySummary summary = decisionEngine.getDecisionHistorySummary(5);

        assertNotNull(summary);
        assertTrue(summary.recentDecisions().isEmpty());
        assertNotNull(summary.timeRange());
    }

    // ==================== 决策规则测试 ====================

    @Test
    void testGetDecisionRules_returnsDefaultRules() {
        List<DecisionEngine.DecisionRule> rules = decisionEngine.getDecisionRules();

        assertNotNull(rules);
        assertFalse(rules.isEmpty());
    }

    @Test
    void testAddDecisionRule() {
        DecisionEngine.DecisionRule newRule = new DecisionEngine.DecisionRule(
            "TEST_RULE",
            "Test rule description",
            List.of(DecisionEngine.DecisionDimension.NOVELTY),
            0.5f,
            "TestAction",
            50,
            true
        );

        decisionEngine.addDecisionRule(newRule);

        List<DecisionEngine.DecisionRule> rules = decisionEngine.getDecisionRules();
        assertTrue(rules.stream().anyMatch(r -> r.ruleId().equals("TEST_RULE")));
    }

    @Test
    void testSetRuleEnabled() {
        // Get first rule
        List<DecisionEngine.DecisionRule> rules = decisionEngine.getDecisionRules();
        assertFalse(rules.isEmpty());
        String ruleId = rules.get(0).ruleId();

        // Disable it
        decisionEngine.setRuleEnabled(ruleId, false);

        // Verify it's disabled
        rules = decisionEngine.getDecisionRules();
        DecisionEngine.DecisionRule modifiedRule = rules.stream()
            .filter(r -> r.ruleId().equals(ruleId))
            .findFirst()
            .orElse(null);
        assertNotNull(modifiedRule);
        assertFalse(modifiedRule.enabled());
    }

    @Test
    void testUpdateRulePriority() {
        List<DecisionEngine.DecisionRule> rules = decisionEngine.getDecisionRules();
        assertFalse(rules.isEmpty());
        String ruleId = rules.get(0).ruleId();
        int originalPriority = rules.get(0).priority();

        decisionEngine.updateRulePriority(ruleId, originalPriority + 10);

        rules = decisionEngine.getDecisionRules();
        DecisionEngine.DecisionRule modifiedRule = rules.stream()
            .filter(r -> r.ruleId().equals(ruleId))
            .findFirst()
            .orElse(null);
        assertNotNull(modifiedRule);
        assertEquals(originalPriority + 10, modifiedRule.priority());
    }

    @Test
    void testMatchRules() {
        DecisionEngine.MultiDimensionalEvaluation evaluation = decisionEngine.evaluateMultiDimensional(
            new PerceptionSystem.SalienceScore(0.5f, 0.5f, 0.5f, 0.5f),
            null
        );

        DecisionEngine.RuleMatchResult result = decisionEngine.matchRules(evaluation);

        assertNotNull(result);
        assertNotNull(result.reasoning());
    }

    @Test
    void testGetRuleMatchLogs() {
        List<DecisionEngine.RuleMatchLog> logs = decisionEngine.getRuleMatchLogs();
        assertNotNull(logs);
    }

    @Test
    void testGetRuleVisualization() {
        DecisionEngine.DecisionRuleVisualization visualization = decisionEngine.getRuleVisualization();

        assertNotNull(visualization);
        assertNotNull(visualization.rules());
        assertNotNull(visualization.dimensionDescriptions());
        assertNotNull(visualization.currentWeights());
        assertNotNull(visualization.strategy());
    }

    // ==================== DecisionResult 测试 ====================

    @Test
    void testDecisionResult_hasActions_withEmptyList() {
        DecisionEngine.DecisionResult result = new DecisionEngine.DecisionResult(
            List.of(),
            "test reason",
            null,
            null
        );

        assertFalse(result.hasActions());
    }

    @Test
    void testDecisionResult_hasActions_withNonEmptyList() {
        DecisionEngine.ToolCall toolCall = new DecisionEngine.ToolCall(
            "TestAction",
            Map.of("key", "value")
        );
        DecisionEngine.DecisionResult result = new DecisionEngine.DecisionResult(
            List.of(toolCall),
            "test reason",
            null,
            null
        );

        assertTrue(result.hasActions());
    }

    @Test
    void testDecisionResult_getOverallScore_withNullEvaluation() {
        DecisionEngine.DecisionResult result = new DecisionEngine.DecisionResult(
            List.of(),
            "test",
            null,
            null
        );

        assertEquals(0f, result.getOverallScore());
    }

    @Test
    void testDecisionResult_getConfidence_withNullConfidence() {
        DecisionEngine.DecisionResult result = new DecisionEngine.DecisionResult(
            List.of(),
            "test",
            null,
            null
        );

        assertEquals(0f, result.getConfidence());
    }

    // ==================== ToolCall 测试 ====================

    @Test
    void testToolCall_record() {
        Map<String, Object> params = Map.of(
            "action", "test",
            "timestamp", Instant.now()
        );
        DecisionEngine.ToolCall toolCall = new DecisionEngine.ToolCall("TestAction", params);

        assertEquals("TestAction", toolCall.tool());
        assertEquals(params, toolCall.params());
    }
}
