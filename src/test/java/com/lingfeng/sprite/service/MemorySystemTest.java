package com.lingfeng.sprite.service;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemorySystem 单元测试 - S2 三层记忆架构测试
 */
class MemorySystemTest {

    private MemorySystem.Memory memory;

    @BeforeEach
    void setUp() {
        memory = new MemorySystem.Memory();
    }

    // ==================== 感官记忆测试 ====================

    @Test
    void testSensoryMemory_addAndRetrieve() {
        Stimulus stimulus = new Stimulus(
            "test-1",
            StimulusType.TEXT,
            "Hello World",
            "test-source",
            Instant.now()
        );

        memory.getSensory().add(stimulus);

        List<Stimulus> recent = memory.getSensory().getRecentStimuli();
        assertEquals(1, recent.size());
        assertEquals("Hello World", recent.get(0).content());
    }

    @Test
    void testSensoryMemory_expiresOldStimuli() throws Exception {
        // Add old stimulus
        Instant oldTime = Instant.now().minusSeconds(60);
        Stimulus oldStimulus = new Stimulus(
            "old-1",
            StimulusType.TEXT,
            "Old content",
            "test",
            oldTime
        );
        memory.getSensory().add(oldStimulus);

        // Add recent stimulus
        Stimulus recentStimulus = new Stimulus(
            "recent-1",
            StimulusType.TEXT,
            "Recent content",
            "test",
            Instant.now()
        );
        memory.getSensory().add(recentStimulus);

        // Old should be filtered out by getRecentStimuli
        List<Stimulus> recent = memory.getSensory().getRecentStimuli();
        assertEquals(1, recent.size());
        assertEquals("Recent content", recent.get(0).content());
    }

    @Test
    void testSensoryMemory_getRecentByType() {
        memory.getSensory().add(new Stimulus("1", StimulusType.TEXT, "text1", "test", Instant.now()));
        memory.getSensory().add(new Stimulus("2", StimulusType.VISUAL, "visual1", "test", Instant.now()));
        memory.getSensory().add(new Stimulus("3", StimulusType.TEXT, "text2", "test", Instant.now()));

        List<Stimulus> textStimuli = memory.getSensory().getRecentByType(StimulusType.TEXT);
        assertEquals(2, textStimuli.size());

        List<Stimulus> visualStimuli = memory.getSensory().getRecentByType(StimulusType.VISUAL);
        assertEquals(1, visualStimuli.size());
    }

    @Test
    void testSensoryMemory_detectPattern() {
        // Add multiple stimuli of same type
        for (int i = 0; i < 3; i++) {
            memory.getSensory().add(new Stimulus(
                "pattern-" + i,
                StimulusType.TEXT,
                "pattern content " + i,
                "test",
                Instant.now()
            ));
        }

        List<Pattern> patterns = memory.getSensory().detectPattern();
        assertFalse(patterns.isEmpty());
        assertTrue(patterns.stream().anyMatch(p -> p.type() == StimulusType.TEXT));
    }

    @Test
    void testSensoryMemory_clear() {
        memory.getSensory().add(new Stimulus("1", StimulusType.TEXT, "test", "test", Instant.now()));
        memory.getSensory().clear();

        List<Stimulus> recent = memory.getSensory().getRecentStimuli();
        assertTrue(recent.isEmpty());
    }

    // ==================== 工作记忆测试 ====================

    @Test
    void testWorkingMemory_addAndAccess() {
        Stimulus source = new Stimulus("src-1", StimulusType.TEXT, "source", "test", Instant.now());
        WorkingMemoryItem item = new WorkingMemoryItem("wm-1", "content", "abstraction", source);

        memory.getWorking().add(item);

        WorkingMemoryItem accessed = memory.getWorking().access("wm-1");
        assertNotNull(accessed);
        assertEquals("content", accessed.content());
        assertEquals(1, accessed.accessCount());
    }

    @Test
    void testWorkingMemory_maxItemsLimit() {
        Stimulus source = new Stimulus("src", StimulusType.TEXT, "source", "test", Instant.now());

        // Add more than WORKING_MEMORY_MAX_ITEMS (7)
        for (int i = 0; i < 10; i++) {
            WorkingMemoryItem item = new WorkingMemoryItem(
                "wm-" + i,
                "content " + i,
                "abstraction " + i,
                source
            );
            memory.getWorking().add(item);
        }

        // Should be limited to max items
        assertEquals(MemorySystem.WORKING_MEMORY_MAX_ITEMS, memory.getWorking().size());
    }

    @Test
    void testWorkingMemory_recall() {
        Stimulus source = new Stimulus("src", StimulusType.TEXT, "source", "test", Instant.now());
        WorkingMemoryItem item = new WorkingMemoryItem(
            "wm-1",
            "apple banana cherry",
            "fruits summary",
            source
        );
        memory.getWorking().add(item);

        List<WorkingMemoryItem> results = memory.getWorking().recall("banana");
        assertFalse(results.isEmpty());
        assertEquals("wm-1", results.get(0).id());
    }

    @Test
    void testWorkingMemory_updateRelevance() {
        Stimulus source = new Stimulus("src", StimulusType.TEXT, "source", "test", Instant.now());
        WorkingMemoryItem item = new WorkingMemoryItem(
            "wm-1",
            "content",
            "abstraction",
            source,
            0.5f
        );
        memory.getWorking().add(item);

        memory.getWorking().updateRelevance("wm-1", 0.9f);

        WorkingMemoryItem updated = memory.getWorking().access("wm-1");
        assertEquals(0.9f, updated.relevance());
    }

    @Test
    void testWorkingMemory_consolidatePattern() {
        Pattern pattern = new Pattern(
            StimulusType.TEXT,
            3,
            Instant.now().minusSeconds(30),
            Instant.now(),
            "Test pattern"
        );

        WorkingMemoryItem item = memory.getWorking().consolidate(pattern, "abstraction", "content");
        assertNotNull(item);
        assertEquals("abstraction", item.abstraction());
    }

    @Test
    void testWorkingMemory_clear() {
        Stimulus source = new Stimulus("src", StimulusType.TEXT, "source", "test", Instant.now());
        memory.getWorking().add(new WorkingMemoryItem("wm-1", "content", "abstraction", source));
        memory.getWorking().clear();

        assertEquals(0, memory.getWorking().size());
    }

    // ==================== 长期记忆测试 ====================

    @Test
    void testLongTermMemory_storeAndRecallEpisodic() {
        EpisodicEntry entry = new EpisodicEntry(
            "ep-1",
            Instant.now(),
            "Test experience"
        );
        memory.getLongTerm().storeEpisodic(entry);

        List<EpisodicEntry> recalled = memory.getLongTerm().recallEpisodic("Test");
        assertFalse(recalled.isEmpty());
        assertEquals("ep-1", recalled.get(0).id());
    }

    @Test
    void testLongTermMemory_storeAndRecallSemantic() {
        SemanticEntry entry = new SemanticEntry(
            "sem-1",
            "Java",
            "Programming language"
        );
        memory.getLongTerm().storeSemantic(entry);

        List<SemanticEntry> recalled = memory.getLongTerm().recallSemantic("Java");
        assertFalse(recalled.isEmpty());
        assertEquals("sem-1", recalled.get(0).id());
    }

    @Test
    void testLongTermMemory_storeAndRecallProcedural() {
        ProceduralEntry entry = new ProceduralEntry(
            "proc-1",
            "coding",
            "Write clean code"
        );
        memory.getLongTerm().storeProcedural(entry);

        ProceduralEntry recalled = memory.getLongTerm().recallProcedural("coding");
        assertNotNull(recalled);
        assertEquals("proc-1", recalled.id());
    }

    @Test
    void testLongTermMemory_storeAndRecallPerceptive() {
        PerceptiveEntry entry = new PerceptiveEntry(
            "perc-1",
            "patternA",
            "associated response",
            "trigger1"
        );
        memory.getLongTerm().storePerceptive(entry);

        List<PerceptiveEntry> recalled = memory.getLongTerm().recallPerceptive("patternA");
        assertFalse(recalled.isEmpty());
        assertEquals("perc-1", recalled.get(0).id());
    }

    @Test
    void testLongTermMemory_getRecentEpisodic() {
        // Store entries with different timestamps
        memory.getLongTerm().storeEpisodic(new EpisodicEntry("ep-1", Instant.now(), "Recent"));
        memory.getLongTerm().storeEpisodic(new EpisodicEntry("ep-2", Instant.now().minusSeconds(86400 * 10), "Old"));

        List<EpisodicEntry> recent = memory.getLongTerm().getRecentEpisodic(7);
        assertEquals(1, recent.size());
        assertEquals("ep-1", recent.get(0).id());
    }

    @Test
    void testLongTermMemory_updateSkillLevel() {
        ProceduralEntry entry = new ProceduralEntry(
            "proc-1",
            "testing",
            "Run tests"
        );
        memory.getLongTerm().storeProcedural(entry);

        memory.getLongTerm().updateSkillLevel("testing", "ADVANCED", true);

        ProceduralEntry updated = memory.getLongTerm().recallProcedural("testing");
        assertEquals("ADVANCED", updated.level());
        assertEquals(1, updated.timesPerformed());
        assertEquals(1.0f, updated.successRate());
    }

    @Test
    void testLongTermMemory_getStats() {
        memory.getLongTerm().storeEpisodic(new EpisodicEntry("ep-1", Instant.now(), "test"));
        memory.getLongTerm().storeSemantic(new SemanticEntry("sem-1", "concept", "def"));

        MemoryStats stats = memory.getLongTerm().getStats();
        assertEquals(1, stats.episodicCount());
        assertEquals(1, stats.semanticCount());
    }

    @Test
    void testLongTermMemory_pruneOldEntries() {
        Instant oldTime = Instant.now().minusSeconds(86400 * 400); // 400 days ago
        EpisodicEntry oldEntry = new EpisodicEntry("ep-old", oldTime, "Old memory");
        memory.getLongTerm().storeEpisodic(oldEntry);

        memory.getLongTerm().storeEpisodic(new EpisodicEntry("ep-new", Instant.now(), "New memory"));

        memory.getLongTerm().pruneOldEntries(365); // Keep 365 days

        List<EpisodicEntry> remaining = memory.getLongTerm().recallEpisodic("Old");
        assertTrue(remaining.isEmpty());

        List<EpisodicEntry> newRemaining = memory.getLongTerm().recallEpisodic("New");
        assertEquals(1, newRemaining.size());
    }

    // ==================== 完整记忆系统测试 ====================

    @Test
    void testMemory_perceive() {
        Stimulus stimulus = new Stimulus(
            "perc-1",
            StimulusType.TEXT,
            "Perceived content",
            "sensor",
            Instant.now()
        );

        memory.perceive(stimulus);

        List<Stimulus> sensory = memory.getSensory().getRecentStimuli();
        assertEquals(1, sensory.size());
        assertEquals("Perceived content", sensory.get(0).content());
    }

    @Test
    void testMemory_consolidateToWorking() {
        Pattern pattern = new Pattern(
            StimulusType.TEXT,
            5,
            Instant.now().minusSeconds(30),
            Instant.now(),
            "Frequent pattern"
        );

        WorkingMemoryItem item = memory.consolidateToWorking(pattern, "summary", "detailed content");

        assertNotNull(item);
        assertEquals("summary", item.abstraction());
        assertEquals(1, memory.getWorking().size());
    }

    @Test
    void testMemory_storeToLongTerm_episodic() {
        Stimulus source = new Stimulus("src", StimulusType.TEXT, "source", "test", Instant.now());
        WorkingMemoryItem item = new WorkingMemoryItem("wm-1", "content", "abstraction", source);

        memory.storeToLongTerm(item, StoreType.EPISODIC);

        List<EpisodicEntry> episodic = memory.getLongTerm().recallEpisodic("abstraction");
        assertFalse(episodic.isEmpty());
    }

    @Test
    void testMemory_recall() {
        // Store directly in long term
        memory.getLongTerm().storeEpisodic(new EpisodicEntry("ep-1", Instant.now(), "Test experience"));

        RecallResult result = memory.recall("Test");

        assertFalse(result.episodic().isEmpty());
        assertEquals("ep-1", result.episodic().get(0).id());
    }

    @Test
    void testMemory_getStatus() {
        memory.perceive(new Stimulus("1", StimulusType.TEXT, "test", "test", Instant.now()));

        MemoryStatus status = memory.getStatus();

        assertNotNull(status);
        assertEquals(1, status.sensoryStimuliCount());
        assertEquals(0, status.workingMemoryItems());
    }

    // ==================== 增强检索测试 ====================

    @Test
    void testEnhancedRecall() {
        memory.getLongTerm().storeEpisodic(new EpisodicEntry("ep-1", Instant.now(), "A happy day"));
        memory.getLongTerm().storeSemantic(new SemanticEntry("sem-1", "happiness", "state of joy"));

        EnhancedRecallResult result = memory.enhancedRecall("happy", 10);

        assertNotNull(result);
        assertTrue(result.overallScore() > 0);
    }

    @Test
    void testCrossModalRecall() {
        memory.getLongTerm().storeEpisodic(new EpisodicEntry("ep-1", Instant.now(), "coding session"));
        memory.getLongTerm().storeSemantic(new SemanticEntry("sem-1", "java", "language"));

        CrossModalRecallResult result = memory.crossModalRecall("java", List.of(StoreType.SEMANTIC));

        assertNotNull(result);
        assertFalse(result.semantic().isEmpty());
    }

    @Test
    void testExtractCrossModalKeywords() {
        EpisodicEntry entry = new EpisodicEntry(
            "ep-1",
            Instant.now(),
            "Worked on a challenging project at the office",
            "office",
            List.of("colleague"),
            "excited",
            "success",
            "collaboration is key"
        );

        List<String> keywords = memory.extractCrossModalKeywords(entry);

        assertNotNull(keywords);
        assertFalse(keywords.isEmpty());
    }

    @Test
    void testGenerateMemorySignature() {
        String sig1 = memory.generateMemorySignature("test content");
        String sig2 = memory.generateMemorySignature("test content");
        String sig3 = memory.generateMemorySignature("different content");

        assertEquals(sig1, sig2); // Same content = same signature
        assertNotEquals(sig1, sig3); // Different content = different signature
    }

    // ==================== 记录类型测试 ====================

    @Test
    void testStimulus_record() {
        Stimulus stimulus = new Stimulus(
            "stim-1",
            StimulusType.TEXT,
            "Test content",
            "source",
            Instant.now(),
            0.8f
        );

        assertEquals("stim-1", stimulus.id());
        assertEquals(StimulusType.TEXT, stimulus.type());
        assertEquals("Test content", stimulus.content());
        assertEquals(0.8f, stimulus.intensity());
    }

    @Test
    void testStimulus_constructorDefaults() {
        Stimulus stimulus = new Stimulus("id", StimulusType.TEXT, "content", "source", Instant.now());

        assertEquals(1.0f, stimulus.intensity());
        assertTrue(stimulus.metadata().isEmpty());
    }

    @Test
    void testPattern_record() {
        Pattern pattern = new Pattern(
            StimulusType.AUDITORY,
            5,
            Instant.now().minusSeconds(30),
            Instant.now(),
            "Sound pattern"
        );

        assertEquals(StimulusType.AUDITORY, pattern.type());
        assertEquals(5, pattern.frequency());
    }

    @Test
    void testWorkingMemoryItem_record() {
        Stimulus source = new Stimulus("src", StimulusType.TEXT, "source", "test", Instant.now());
        WorkingMemoryItem item = new WorkingMemoryItem(
            "wm-1",
            "content",
            "abstraction",
            source,
            0.7f
        );

        assertEquals("wm-1", item.id());
        assertEquals(0.7f, item.relevance());
        assertEquals(0, item.accessCount());
    }

    @Test
    void testEpisodicEntry_record() {
        Instant now = Instant.now();
        EpisodicEntry entry = new EpisodicEntry(
            "ep-1",
            now,
            "location",
            List.of("person1", "person2"),
            "experience",
            "emotion",
            "outcome",
            "lesson"
        );

        assertEquals("ep-1", entry.id());
        assertEquals("location", entry.location());
        assertEquals(2, entry.people().size());
    }

    @Test
    void testSemanticEntry_record() {
        SemanticEntry entry = new SemanticEntry(
            "sem-1",
            "concept",
            "definition",
            List.of("example1"),
            List.of("related1"),
            0.9f,
            Instant.now(),
            Instant.now()
        );

        assertEquals("sem-1", entry.id());
        assertEquals(0.9f, entry.confidence());
    }

    @Test
    void testProceduralEntry_record() {
        ProceduralEntry entry = new ProceduralEntry(
            "proc-1",
            "skill",
            "procedure",
            "EXPERT",
            Instant.now(),
            10,
            0.95f
        );

        assertEquals("proc-1", entry.id());
        assertEquals("EXPERT", entry.level());
        assertEquals(10, entry.timesPerformed());
    }

    @Test
    void testPerceptiveEntry_record() {
        PerceptiveEntry entry = new PerceptiveEntry(
            "perc-1",
            "pattern",
            "association",
            "trigger",
            0.8f,
            5
        );

        assertEquals("perc-1", entry.id());
        assertEquals(0.8f, entry.strength());
        assertEquals(5, entry.timesTriggered());
    }

    @Test
    void testMemoryStats_record() {
        MemoryStats stats = new MemoryStats(10, 20, 30, 40);

        assertEquals(10, stats.episodicCount());
        assertEquals(20, stats.semanticCount());
        assertEquals(30, stats.proceduralCount());
        assertEquals(40, stats.perceptiveCount());
    }

    @Test
    void testMemoryStatus_record() {
        MemoryStats longTermStats = new MemoryStats(5, 10, 15, 20);
        MemoryStatus status = new MemoryStatus(3, 7, longTermStats);

        assertEquals(3, status.sensoryStimuliCount());
        assertEquals(7, status.workingMemoryItems());
        assertEquals(5, status.longTermStats().episodicCount());
    }

    @Test
    void testRecallResult_record() {
        RecallResult result = new RecallResult(List.of(), List.of(), List.of());

        assertTrue(result.workingItems().isEmpty());
        assertTrue(result.episodic().isEmpty());
        assertTrue(result.semantic().isEmpty());
    }

    @Test
    void testEnhancedRecallResult_record() {
        EnhancedRecallResult result = new EnhancedRecallResult(
            List.of(),
            List.of(),
            List.of(),
            0.75f
        );

        assertEquals(0.75f, result.overallScore());
    }

    @Test
    void testCrossModalRecallResult_record() {
        CrossModalRecallResult result = new CrossModalRecallResult(
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        assertTrue(result.episodic().isEmpty());
        assertTrue(result.semantic().isEmpty());
    }
}
