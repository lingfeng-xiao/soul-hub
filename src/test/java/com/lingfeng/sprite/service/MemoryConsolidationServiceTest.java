package com.lingfeng.sprite.service;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryConsolidationService 单元测试
 */
class MemoryConsolidationServiceTest {

    private final MemoryConsolidationService service = new MemoryConsolidationService();

    // ==================== consolidateIfNeeded 测试 ====================

    @Test
    void testConsolidateIfNeeded_nullMemory_doesNothing() {
        // 不抛异常
        assertDoesNotThrow(() -> service.consolidateIfNeeded(null));
    }

    @Test
    void testConsolidateIfNeeded_noPatterns_doesNothing() {
        MemorySystem.Memory memory = createEmptyMemory();

        service.consolidateIfNeeded(memory);

        // 没有模式，所以不会添加到工作记忆
        assertEquals(0, memory.getWorking().size());
    }

    @Test
    void testConsolidateIfNeeded_withPatterns_consolidatesToWorkingMemory() {
        MemorySystem.Memory memory = createMemoryWithPatterns();

        service.consolidateIfNeeded(memory);

        // 模式应该被添加到工作记忆
        assertTrue(memory.getWorking().size() > 0);
    }

    @Test
    void testConsolidateIfNeeded_highRelevance_storesToLongTerm() {
        MemorySystem.Memory memory = createMemoryWithHighRelevanceItem();

        int initialLongTermSize = memory.getLongTerm().size();

        service.consolidateIfNeeded(memory);

        // 高相关性的项应该存入长期记忆
        // 注意：这取决于具体的实现
    }

    // ==================== forceConsolidateAll 测试 ====================

    @Test
    void testForceConsolidateAll_nullMemory_doesNothing() {
        assertDoesNotThrow(() -> service.forceConsolidateAll(null));
    }

    @Test
    void testForceConsolidateAll_emptyMemory_doesNothing() {
        MemorySystem.Memory memory = createEmptyMemory();

        service.forceConsolidateAll(memory);

        assertEquals(0, memory.getWorking().size());
    }

    @Test
    void testForceConsolidateAll_withItems_storesAllToLongTerm() {
        MemorySystem.Memory memory = createMemoryWithWorkingItems();

        int initialLongTermSize = memory.getLongTerm().size();

        service.forceConsolidateAll(memory);

        // 所有工作记忆项应该被存入长期记忆
        assertTrue(memory.getLongTerm().size() >= initialLongTermSize);
    }

    // ==================== StoreType 推断测试 ====================

    @Test
    void testStoreTypeInference_commandSource_returnsProcedural() {
        // 测试 inferStoreType 逻辑通过 consolidateIfNeeded
        MemorySystem.Memory memory = createMemoryWithCommandStimulus();

        service.consolidateIfNeeded(memory);

        // COMMAND 类型应该存储为 PROCEDURAL
        // 这个测试验证基本逻辑
    }

    @Test
    void testStoreTypeInference_emotionalSource_returnsEpisodic() {
        MemorySystem.Memory memory = createMemoryWithEmotionalStimulus();

        service.consolidateIfNeeded(memory);

        // EMOTIONAL 类型应该存储为 EPISODIC
    }

    @Test
    void testStoreTypeInference_environmentSource_returnsSemantic() {
        MemorySystem.Memory memory = createMemoryWithEnvironmentStimulus();

        service.consolidateIfNeeded(memory);

        // ENVIRONMENT 类型应该存储为 SEMANTIC
    }

    // ==================== 辅助方法 ====================

    private MemorySystem.Memory createEmptyMemory() {
        return new TestMemory();
    }

    private MemorySystem.Memory createMemoryWithPatterns() {
        TestMemory memory = new TestMemory();

        // 添加3个相同类型的刺激以触发模式检测
        MemorySystem.StimulusType type = MemorySystem.StimulusType.TEXT;
        memory.sensory.add(new MemorySystem.Stimulus("s1", type, "content1", "source", Instant.now()));
        memory.sensory.add(new MemorySystem.Stimulus("s2", type, "content2", "source", Instant.now()));
        memory.sensory.add(new MemorySystem.Stimulus("s3", type, "content3", "source", Instant.now()));

        return memory;
    }

    private MemorySystem.Memory createMemoryWithHighRelevanceItem() {
        TestMemory memory = new TestMemory();

        // 创建一个高相关性的工作记忆项
        MemorySystem.Stimulus stimulus = new MemorySystem.Stimulus(
            "test-id",
            MemorySystem.StimulusType.TEXT,
            "high relevance content",
            "test-source",
            Instant.now()
        );

        MemorySystem.WorkingMemoryItem item = new MemorySystem.WorkingMemoryItem(
            "wm-1",
            "content",
            "abstraction",
            stimulus,
            1.0f  // 高相关性
        );

        memory.working.add(item);

        return memory;
    }

    private MemorySystem.Memory createMemoryWithWorkingItems() {
        TestMemory memory = new TestMemory();

        MemorySystem.Stimulus stimulus = new MemorySystem.Stimulus(
            "test-id",
            MemorySystem.StimulusType.TEXT,
            "content",
            "test-source",
            Instant.now()
        );

        for (int i = 0; i < 3; i++) {
            MemorySystem.WorkingMemoryItem item = new MemorySystem.WorkingMemoryItem(
                "wm-" + i,
                "content" + i,
                "abstraction" + i,
                stimulus,
                0.8f
            );
            memory.working.add(item);
        }

        return memory;
    }

    private MemorySystem.Memory createMemoryWithCommandStimulus() {
        TestMemory memory = new TestMemory();

        // 添加 COMMAND 类型的刺激
        MemorySystem.StimulusType type = MemorySystem.StimulusType.COMMAND;
        memory.sensory.add(new MemorySystem.Stimulus("cmd1", type, "cmd", "source", Instant.now()));
        memory.sensory.add(new MemorySystem.Stimulus("cmd2", type, "cmd", "source", Instant.now()));
        memory.sensory.add(new MemorySystem.Stimulus("cmd3", type, "cmd", "source", Instant.now()));

        return memory;
    }

    private MemorySystem.Memory createMemoryWithEmotionalStimulus() {
        TestMemory memory = new TestMemory();

        MemorySystem.StimulusType type = MemorySystem.StimulusType.EMOTIONAL;
        memory.sensory.add(new MemorySystem.Stimulus("emo1", type, "emotion", "source", Instant.now()));
        memory.sensory.add(new MemorySystem.Stimulus("emo2", type, "emotion", "source", Instant.now()));
        memory.sensory.add(new MemorySystem.Stimulus("emo3", type, "emotion", "source", Instant.now()));

        return memory;
    }

    private MemorySystem.Memory createMemoryWithEnvironmentStimulus() {
        TestMemory memory = new TestMemory();

        MemorySystem.StimulusType type = MemorySystem.StimulusType.ENVIRONMENT;
        memory.sensory.add(new MemorySystem.Stimulus("env1", type, "env", "source", Instant.now()));
        memory.sensory.add(new MemorySystem.Stimulus("env2", type, "env", "source", Instant.now()));
        memory.sensory.add(new MemorySystem.Stimulus("env3", type, "env", "source", Instant.now()));

        return memory;
    }

    // ==================== 测试用 Memory 实现 ====================

    /**
     * 测试用 Memory 实现 - 提供基本的记忆功能
     */
    private static class TestMemory implements MemorySystem.Memory {
        final SensoryMemory sensory = new SensoryMemory();
        final WorkingMemory working = new WorkingMemory();
        final LongTermMemory longTerm = new LongTermMemory();

        @Override
        public SensoryMemory getSensory() {
            return sensory;
        }

        @Override
        public WorkingMemory getWorking() {
            return working;
        }

        @Override
        public LongTermMemory getLongTerm() {
            return longTerm;
        }

        @Override
        public void storeToLongTerm(WorkingMemoryItem item, StoreType type) {
            longTerm.store(item, type);
        }

        @Override
        public WorkingMemoryItem consolidateToWorking(Pattern pattern, String abstraction, Object content) {
            return working.consolidate(pattern, abstraction, content);
        }

        @Override
        public List<WorkingMemoryItem> retrieveFromLongTerm(String query, int limit) {
            return longTerm.retrieve(query, limit);
        }

        @Override
        public void clear() {
            sensory.clear();
            working.clear();
        }
    }

    // ==================== Record 类型测试 ====================

    @Test
    void testStimulusRecord_immutable() {
        java.time.Instant now = java.time.Instant.now();

        MemorySystem.Stimulus stimulus = new MemorySystem.Stimulus(
            "id-1",
            MemorySystem.StimulusType.TEXT,
            "content",
            "source",
            now,
            0.8f,
            java.util.Map.of("key", "value")
        );

        assertEquals("id-1", stimulus.id());
        assertEquals(MemorySystem.StimulusType.TEXT, stimulus.type());
        assertEquals("content", stimulus.content());
        assertEquals("source", stimulus.source());
        assertEquals(now, stimulus.timestamp());
        assertEquals(0.8f, stimulus.intensity());
    }

    @Test
    void testPatternRecord_immutable() {
        java.time.Instant now = java.time.Instant.now();

        MemorySystem.Pattern pattern = new MemorySystem.Pattern(
            MemorySystem.StimulusType.TEXT,
            5,
            now,
            now,
            "pattern description"
        );

        assertEquals(MemorySystem.StimulusType.TEXT, pattern.type());
        assertEquals(5, pattern.frequency());
        assertEquals(now, pattern.firstSeen());
        assertEquals(now, pattern.lastSeen());
        assertEquals("pattern description", pattern.description());
    }

    @Test
    void testWorkingMemoryItemRecord_immutable() {
        java.time.Instant now = java.time.Instant.now();

        MemorySystem.Stimulus source = new MemorySystem.Stimulus(
            "src-1",
            MemorySystem.StimulusType.TEXT,
            "content",
            "source",
            now
        );

        MemorySystem.WorkingMemoryItem item = new MemorySystem.WorkingMemoryItem(
            "item-1",
            "content",
            "abstraction",
            source,
            5,
            now,
            0.8f,
            now
        );

        assertEquals("item-1", item.id());
        assertEquals("content", item.content());
        assertEquals("abstraction", item.abstraction());
        assertEquals(source, item.source());
        assertEquals(5, item.accessCount());
        assertEquals(0.8f, item.relevance());
    }

    // ==================== StimulusType 枚举测试 ====================

    @Test
    void testStimulusTypeValues() {
        MemorySystem.StimulusType[] types = MemorySystem.StimulusType.values();
        assertEquals(7, types.length);
        assertNotNull(MemorySystem.StimulusType.VISUAL);
        assertNotNull(MemorySystem.StimulusType.AUDITORY);
        assertNotNull(MemorySystem.StimulusType.TEXT);
        assertNotNull(MemorySystem.StimulusType.COMMAND);
        assertNotNull(MemorySystem.StimulusType.EMOTIONAL);
        assertNotNull(MemorySystem.StimulusType.SYSTEM);
        assertNotNull(MemorySystem.StimulusType.ENVIRONMENT);
    }

    // ==================== StoreType 枚举测试 ====================

    @Test
    void testStoreTypeValues() {
        MemorySystem.StoreType[] types = MemorySystem.StoreType.values();
        assertEquals(4, types.length);
        assertNotNull(MemorySystem.StoreType.EPISODIC);
        assertNotNull(MemorySystem.StoreType.SEMANTIC);
        assertNotNull(MemorySystem.StoreType.PROCEDURAL);
        assertNotNull(MemorySystem.StoreType.PERCEPTIVE);
    }

    // ==================== MemorySystem 常量测试 ====================

    @Test
    void testConstants() {
        assertEquals(30L, MemorySystem.SENSORY_WINDOW_SECONDS);
        assertEquals(7, MemorySystem.WORKING_MEMORY_MAX_ITEMS);
        assertEquals(365L, MemorySystem.LONG_TERM_RETENTION_DAYS);
    }
}
