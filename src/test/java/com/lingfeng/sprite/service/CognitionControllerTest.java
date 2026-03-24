package com.lingfeng.sprite.service;

import com.lingfeng.sprite.*;
import com.lingfeng.sprite.cognition.*;
import com.lingfeng.sprite.sensor.RealEnvironmentSensor;
import com.lingfeng.sprite.sensor.RealPlatformSensor;
import com.lingfeng.sprite.sensor.RealUserSensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CognitionController 单元测试 - S5 感知→认知闭环测试
 */
class CognitionControllerTest {

    private CognitionController controller;
    private MemorySystem.Memory memory;
    private SelfModel.Self selfModel;
    private WorldModel.World worldModel;

    @BeforeEach
    void setUp() {
        // Create memory
        memory = new MemorySystem.Memory();

        // Create self model
        selfModel = SelfModel.Self.createDefault();

        // Create world model with owner
        OwnerModel.Owner owner = createTestOwner();
        worldModel = new WorldModel.World(owner);

        // Create perception system
        PerceptionSystem.DeviceType deviceType = PerceptionSystem.DeviceType.PC;
        PerceptionSystem.System perceptionSystem = new PerceptionSystem.System(
            List.of(
                new RealPlatformSensor("test-device", deviceType),
                new RealUserSensor(),
                new RealEnvironmentSensor()
            )
        );

        // Create controller with null reasoning engine (use heuristic mode)
        controller = new CognitionController(
            perceptionSystem,
            memory,
            selfModel,
            worldModel,
            null // No LLM reasoning engine for unit tests
        );
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

    // ==================== 认知闭环测试 ====================

    @Test
    void testCognitionCycle_completesWithoutError() {
        CognitionController.CognitionResult result = controller.cognitionCycle();

        assertNotNull(result);
        assertNotNull(result.perception());
        assertNotNull(result.selfModel());
        assertNotNull(result.worldModel());
    }

    @Test
    void testCognitionCycle_increasesCycleCount() {
        // Initial stats
        CognitionController.CognitionStats initialStats = controller.getStats();
        int initialCycles = initialStats.totalCycles();

        // Run cycle
        controller.cognitionCycle();

        CognitionController.CognitionStats newStats = controller.getStats();
        assertEquals(initialCycles + 1, newStats.totalCycles());
    }

    @Test
    void testCognitionCycle_updatesLastCycleTime() {
        // Run cycle
        CognitionController.CognitionResult result = controller.cognitionCycle();

        // Check state has recent cycle time
        CognitionController.CognitionState state = controller.getCurrentState();
        assertNotNull(state.lastCycleTime());
    }

    @Test
    void testCognitionCycle_withPerception_storesStimulus() {
        controller.cognitionCycle();

        // Memory should have been updated
        List<MemorySystem.Stimulus> stimuli = memory.getSensory().getRecentStimuli();
        assertFalse(stimuli.isEmpty());
    }

    // ==================== 状态获取测试 ====================

    @Test
    void testGetCurrentState_returnsValidState() {
        CognitionController.CognitionState state = controller.getCurrentState();

        assertNotNull(state);
        assertNotNull(state.selfModel());
        assertNotNull(state.worldModel());
        assertNotNull(state.memoryStatus());
    }

    @Test
    void testGetStats_returnsValidStats() {
        CognitionController.CognitionStats stats = controller.getStats();

        assertNotNull(stats);
        assertEquals(0, stats.totalCycles()); // Initial
        assertTrue(stats.avgSalience() >= 0);
        assertTrue(stats.reflectionCount() >= 0);
    }

    @Test
    void testGetStats_afterCognitionCycle() {
        controller.cognitionCycle();

        CognitionController.CognitionStats stats = controller.getStats();
        assertEquals(1, stats.totalCycles());
    }

    // ==================== 自我模型更新测试 ====================

    @Test
    void testUpdateSelfModel_validModel() {
        SelfModel.Self newSelfModel = SelfModel.Self.createDefault();

        controller.updateSelfModel(newSelfModel);

        CognitionController.CognitionState state = controller.getCurrentState();
        assertEquals(newSelfModel, state.selfModel());
    }

    @Test
    void testUpdateSelfModel_nullModel_unchanged() {
        SelfModel.Self originalModel = controller.getCurrentState().selfModel();

        controller.updateSelfModel(null);

        CognitionController.CognitionState state = controller.getCurrentState();
        assertEquals(originalModel, state.selfModel());
    }

    // ==================== 快速反应测试 ====================

    @Test
    void testHandleQuickReaction_nullInput_returnsNull() {
        var result = controller.handleQuickReaction(null, null);
        assertNull(result);
    }

    @Test
    void testHandleQuickReaction_blankInput_returnsNull() {
        var result = controller.handleQuickReaction("   ", null);
        assertNull(result);
    }

    @Test
    void testHandleQuickReaction_nullHandler_returnsNull() {
        var result = controller.handleQuickReaction("test input", null);
        assertNull(result);
    }

    // ==================== 行动建议生成测试 ====================

    @Test
    void testCognitionCycle_generatesActionRecommendation() {
        CognitionController.CognitionResult result = controller.cognitionCycle();

        assertNotNull(result.actionRecommendation());
        assertNotNull(result.actionRecommendation().recommendations());
        assertNotNull(result.actionRecommendation().reason());
    }

    @Test
    void testCognitionCycle_generatesDecisionResult() {
        CognitionController.CognitionResult result = controller.cognitionCycle();

        assertNotNull(result.decisionResult());
    }

    @Test
    void testCognitionCycle_calculatesSalienceScore() {
        CognitionController.CognitionResult result = controller.cognitionCycle();

        assertNotNull(result.salienceScore());
        assertTrue(result.salienceScore().overall() >= 0);
        assertTrue(result.salienceScore().overall() <= 1);
    }

    // ==================== 显著性检测测试 ====================

    @Test
    void testCognitionCycle_significantDetection() {
        CognitionController.CognitionResult result = controller.cognitionCycle();

        assertNotNull(result.isSignificant()); // Should be either true or false
    }

    // ==================== 记录类型测试 ====================

    @Test
    void testCognitionState_record() {
        CognitionController.CognitionState state = new CognitionController.CognitionState(
            selfModel,
            worldModel,
            Instant.now(),
            memory.getStatus()
        );

        assertEquals(selfModel, state.selfModel());
        assertEquals(worldModel, state.worldModel());
        assertNotNull(state.lastCycleTime());
        assertNotNull(state.memoryStatus());
    }

    @Test
    void testCognitionStats_record() {
        CognitionController.CognitionStats stats = new CognitionController.CognitionStats(
            10,
            0.75f,
            5,
            100,
            50
        );

        assertEquals(10, stats.totalCycles());
        assertEquals(0.75f, stats.avgSalience());
        assertEquals(5, stats.reflectionCount());
        assertEquals(100, stats.worldModelFacts());
        assertEquals(50, stats.longTermMemories());
    }

    @Test
    void testActionRecommendation_record() {
        CognitionController.ActionRecommendation rec = new CognitionController.ActionRecommendation(
            List.of("action1", "action2"),
            3,
            "test reason"
        );

        assertEquals(2, rec.recommendations().size());
        assertEquals(3, rec.priority());
        assertEquals("test reason", rec.reason());
    }

    @Test
    void testCognitionResult_record() {
        CognitionController.CognitionResult result = new CognitionController.CognitionResult(
            null, // perception
            selfModel,
            worldModel,
            null, // reflection
            null, // reasoningResult
            new CognitionController.ActionRecommendation(List.of(), 0, ""),
            new PerceptionSystem.SalienceScore(0.5f, 0.5f, 0.5f, 0.5f),
            true,
            null // decisionResult
        );

        assertNotNull(result.selfModel());
        assertNotNull(result.worldModel());
        assertNotNull(result.salienceScore());
        assertTrue(result.isSignificant());
    }

    @Test
    void testQuickReactionResult_record() {
        com.lingfeng.sprite.action.ActionResult actionResult =
            com.lingfeng.sprite.action.ActionResult.success("test");

        CognitionController.QuickReactionResult qResult = new CognitionController.QuickReactionResult(
            true,
            false,
            actionResult,
            "event-123"
        );

        assertTrue(qResult.bypassed());
        assertFalse(qResult.isUrgent());
        assertEquals("event-123", qResult.eventId());
        assertTrue(qResult.result().success());
    }
}
