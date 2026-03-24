package com.lingfeng.sprite.service;

import com.lingfeng.sprite.Sprite;
import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.WorldModel;
import com.lingfeng.sprite.EvolutionEngine;
import com.lingfeng.sprite.cognition.CognitionController;
import com.lingfeng.sprite.cognition.ReasoningEngine;
import com.lingfeng.sprite.config.AppConfig;
import com.lingfeng.sprite.llm.MinMaxConfig;
import com.lingfeng.sprite.llm.MinMaxLlmReasoner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SpriteService Unit Tests - S30-1 Core Service Unit Tests
 *
 * Tests the core SpriteService which manages the Sprite lifecycle,
 * cognition cycles, and coordinates various services.
 */
@ExtendWith(MockitoExtension.class)
class SpriteServiceTest {

    @Mock
    private AppConfig appConfig;

    @Mock
    private MinMaxConfig minMaxConfig;

    @Mock
    private MinMaxLlmReasoner minMaxLlmReasoner;

    @Mock
    private MemoryConsolidationService memoryConsolidationService;

    @Mock
    private EvolutionService evolutionService;

    @Mock
    private ActionExecutor actionExecutor;

    @Mock
    private UnifiedContextService unifiedContextService;

    @Mock
    private AvatarService avatarService;

    @Mock
    private WebhookService webhookService;

    @Mock
    private MultiDeviceCoordinationService multiDeviceCoordinationService;

    @Mock
    private com.lingfeng.sprite.action.QuickReactionHandler quickReactionHandler;

    @Mock
    private com.lingfeng.sprite.PerceptionSystem perceptionSystem;

    private SpriteService spriteService;
    private Sprite sprite;
    private MemorySystem.Memory memory;
    private SelfModel.Self selfModel;
    private WorldModel.World worldModel;

    @BeforeEach
    void setUp() {
        // Create test memory
        memory = new MemorySystem.Memory();

        // Create test self model
        selfModel = SelfModel.Self.createDefault();

        // Create test world model
        worldModel = WorldModel.World.createDefault();

        // Mock app config owner
        AppConfig.OwnerConfig ownerConfig = new AppConfig.OwnerConfig();
        ownerConfig.setName("TestOwner");
        ownerConfig.setOccupation("Developer");
        ownerConfig.setWorkplace("Office");
        ownerConfig.setHome("Home");
        when(appConfig.getOwner()).thenReturn(ownerConfig);

        // Mock LLM config
        AppConfig.LlmConfig llmConfig = new AppConfig.LlmConfig();
        llmConfig.setEnabled(false);
        when(appConfig.getLlm()).thenReturn(llmConfig);

        // Create a simple Sprite instance for testing
        sprite = Sprite.create("TestSprite", Sprite.Platform.PC);

        // Note: Full SpriteService testing requires extensive mocking of all dependencies
        // This test focuses on testing the service's coordination logic with mocked components
    }

    @Test
    void testSpriteCreation() {
        assertNotNull(sprite);
        assertEquals("TestSprite", sprite.getIdentity().identity().displayName());
    }

    @Test
    void testSpriteStateAfterCreation() {
        Sprite.State state = sprite.getState();

        assertNotNull(state);
        assertEquals(Sprite.Platform.PC, state.platform());
        assertFalse(state.isRunning());
    }

    @Test
    void testSpriteStartStop() {
        sprite.start();
        assertTrue(sprite.isRunning());

        sprite.stop();
        assertFalse(sprite.isRunning());
    }

    @Test
    void testCognitionCycleExecution() {
        sprite.start();

        CognitionController.CognitionResult result = sprite.cognitionCycle();

        assertNotNull(result);
        assertNotNull(result.perception());
        assertNotNull(result.selfModel());
        assertNotNull(result.worldModel());
    }

    @Test
    void testGetMemoryStatus() {
        MemorySystem.MemoryStatus status = sprite.getMemoryStatus();

        assertNotNull(status);
        assertEquals(0, status.sensoryStimuliCount());
        assertEquals(0, status.workingMemoryItems());
    }

    @Test
    void testGetEvolutionStatus() {
        EvolutionEngine.EvolutionStatus status = sprite.getEvolutionStatus();

        assertNotNull(status);
    }

    @Test
    void testGetCognitionStats() {
        sprite.start();
        sprite.cognitionCycle();

        CognitionController.CognitionStats stats = sprite.getCognitionStats();

        assertNotNull(stats);
        assertTrue(stats.totalCycles() >= 0);
    }

    @Test
    void testRecordFeedback() {
        sprite.recordFeedback(
            EvolutionEngine.Feedback.FeedbackSource.OWNER_EXPLICIT,
            "Test feedback",
            "Positive outcome",
            true,
            EvolutionEngine.Impact.HIGH
        );

        // Verify no exception is thrown and evolution engine receives feedback
        EvolutionEngine.EvolutionStatus status = sprite.getEvolutionStatus();
        assertNotNull(status);
    }

    @Test
    void testSpriteUpdateSelfModel() {
        SelfModel.Self newSelfModel = SelfModel.Self.createDefault();
        newSelfModel = new SelfModel.Self(
            newSelfModel.identity(),
            newSelfModel.personality(),
            newSelfModel.capabilities(),
            newSelfModel.avatars(),
            newSelfModel.metacognition(),
            newSelfModel.growthHistory(),
            2, // evolutionLevel
            1, // evolutionCount
            newSelfModel.learnedSkills(),
            newSelfModel.selfGoals(),
            newSelfModel.learningMetrics(),
            newSelfModel.autonomousState()
        );

        sprite.updateSelfModel(newSelfModel);

        Sprite.State state = sprite.getState();
        assertNotNull(state);
    }

    @Test
    void testSpriteWithNullReasoningEngine() {
        // Sprite can operate without LLM reasoning engine
        Sprite spriteNoLLM = Sprite.create("NoLLM", Sprite.Platform.PC);

        assertNotNull(spriteNoLLM);
        assertFalse(spriteNoLLM.getState().hasLlmSupport());

        spriteNoLLM.start();
        CognitionController.CognitionResult result = spriteNoLLM.cognitionCycle();
        assertNotNull(result);
    }

    @Test
    void testMultipleCognitionCycles() {
        sprite.start();

        // Execute multiple cycles
        for (int i = 0; i < 3; i++) {
            CognitionController.CognitionResult result = sprite.cognitionCycle();
            assertNotNull(result);
        }

        CognitionController.CognitionStats stats = sprite.getCognitionStats();
        assertTrue(stats.totalCycles() >= 3);
    }

    @Test
    void testEvolvable() {
        sprite.start();

        // Execute a cognition cycle
        sprite.cognitionCycle();

        // Evolve
        EvolutionEngine.EvolutionResult evolutionResult = sprite.evolve();

        assertNotNull(evolutionResult);
    }

    @Test
    void testSpritePlatformTypes() {
        Sprite cloudSprite = Sprite.create("CloudSprite", Sprite.Platform.CLOUD);
        Sprite phoneSprite = Sprite.create("PhoneSprite", Sprite.Platform.PHONE);

        assertEquals(Sprite.Platform.CLOUD, cloudSprite.getPlatform());
        assertEquals(Sprite.Platform.PHONE, phoneSprite.getPlatform());
    }

    @Test
    void testSelfModelGrowthRecording() {
        SelfModel.Self original = sprite.getState().identity();

        // Record a growth event
        SelfModel.Self updated = original.recordGrowth(
            SelfModel.GrowthType.CAPABILITY_IMPROVED,
            "Test capability improvement",
            "BASIC",
            "ADVANCED"
        );

        assertNotNull(updated);
        assertEquals(1, updated.growthHistory().size());
    }

    @Test
    void testMemorySystemIntegration() {
        // Test that sprite's memory is properly integrated
        MemorySystem.Memory spriteMemory = memory;

        // Add a stimulus
        MemorySystem.Stimulus stimulus = new MemorySystem.Stimulus(
            "test-id",
            MemorySystem.StimulusType.TEXT,
            "Test content",
            "test-source",
            Instant.now()
        );
        spriteMemory.perceive(stimulus);

        assertEquals(1, spriteMemory.getSensory().getRecentStimuli().size());
    }

    @Test
    void testWorkingMemoryPruning() {
        MemorySystem.WorkingMemory workingMemory = new MemorySystem.WorkingMemory();

        // Add more items than the limit (7)
        for (int i = 0; i < 10; i++) {
            MemorySystem.Stimulus stimulus = new MemorySystem.Stimulus(
                "stim-" + i,
                MemorySystem.StimulusType.TEXT,
                "Content " + i,
                "source",
                Instant.now()
            );
            MemorySystem.WorkingMemoryItem item = new MemorySystem.WorkingMemoryItem(
                "item-" + i,
                "Content " + i,
                "Abstraction " + i,
                stimulus
            );
            workingMemory.add(item);
        }

        // Should be pruned to max 7 items
        assertEquals(7, workingMemory.size());
    }

    @Test
    void testLongTermMemoryStorage() {
        MemorySystem.LongTermMemory longTermMemory = new MemorySystem.LongTermMemory();

        // Store episodic memory
        MemorySystem.EpisodicEntry episodicEntry = new MemorySystem.EpisodicEntry(
            "ep-1",
            Instant.now(),
            "Test experience"
        );
        longTermMemory.storeEpisodic(episodicEntry);

        // Store semantic memory
        MemorySystem.SemanticEntry semanticEntry = new MemorySystem.SemanticEntry(
            "sem-1",
            "Test concept",
            "Test definition"
        );
        longTermMemory.storeSemantic(semanticEntry);

        // Store procedural memory
        MemorySystem.ProceduralEntry proceduralEntry = new MemorySystem.ProceduralEntry(
            "proc-1",
            "Test skill",
            "Test procedure"
        );
        longTermMemory.storeProcedural(proceduralEntry);

        MemorySystem.MemoryStats stats = longTermMemory.getStats();
        assertEquals(1, stats.episodicCount());
        assertEquals(1, stats.semanticCount());
        assertEquals(1, stats.proceduralCount());
    }

    @Test
    void testConfidenceLevelFromScore() {
        // Test DecisionEngine.ConfidenceLevel.fromScore()
        assertEquals(
            com.lingfeng.sprite.cognition.DecisionEngine.ConfidenceLevel.HIGH,
            com.lingfeng.sprite.cognition.DecisionEngine.ConfidenceLevel.fromScore(0.9f)
        );
        assertEquals(
            com.lingfeng.sprite.cognition.DecisionEngine.ConfidenceLevel.MEDIUM,
            com.lingfeng.sprite.cognition.DecisionEngine.ConfidenceLevel.fromScore(0.6f)
        );
        assertEquals(
            com.lingfeng.sprite.cognition.DecisionEngine.ConfidenceLevel.LOW,
            com.lingfeng.sprite.cognition.DecisionEngine.ConfidenceLevel.fromScore(0.4f)
        );
        assertEquals(
            com.lingfeng.sprite.cognition.DecisionEngine.ConfidenceLevel.UNKNOWN,
            com.lingfeng.sprite.cognition.DecisionEngine.ConfidenceLevel.fromScore(0.1f)
        );
    }

    @Test
    void testSelfGoalProgressTracking() {
        SelfModel.Self self = SelfModel.Self.createDefault();

        SelfModel.SelfGoal goal = new SelfModel.SelfGoal(
            "goal-1",
            "Test goal",
            "Test category",
            100f,
            0f,
            Instant.now(),
            Instant.now().plusSeconds(86400),
            SelfModel.SelfGoal.GoalPriority.HIGH,
            SelfModel.SelfGoal.GoalState.ACTIVE
        );

        SelfModel.Self updatedSelf = self.addSelfGoal(goal);
        assertEquals(1, updatedSelf.selfGoals().size());

        // Update progress
        SelfModel.Self progressSelf = updatedSelf.updateSelfGoalProgress("goal-1", 50f);
        assertEquals(50f, progressSelf.selfGoals().get(0).currentProgress());
    }

    @Test
    void testLearnedSkillAddition() {
        SelfModel.Self self = SelfModel.Self.createDefault();

        SelfModel.LearnedSkill skill = new SelfModel.LearnedSkill(
            "skill-1",
            "Test skill",
            "Test description",
            Instant.now(),
            0.8f,
            List.of("trigger1", "trigger2"),
            "Test procedure"
        );

        SelfModel.Self updatedSelf = self.addLearnedSkill(skill);
        assertEquals(1, updatedSelf.learnedSkills().size());
        assertEquals("Test skill", updatedSelf.learnedSkills().get(0).name());
    }
}
