package com.lingfeng.sprite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryVisualizationService 单元测试
 */
class MemoryVisualizationServiceTest {

    private MemoryVisualizationService service;

    @BeforeEach
    void setUp() {
        service = new MemoryVisualizationService();
    }

    @Test
    void testGenerateVisualizationWithData() {
        List<String> episodic = List.of("memory1", "memory2");
        List<String> semantic = List.of("fact1");
        List<String> procedural = List.of("skill1", "skill2", "skill3");
        List<String> perceptive = List.of("percept1");
        List<String> working = List.of("work1");

        MemoryVisualizationService.MemoryVisualizationData data = service.generateVisualization(
            episodic, semantic, procedural, perceptive, working
        );

        assertNotNull(data);
        assertNotNull(data.timestamp());
        assertNotNull(data.typeStats());
        assertEquals(2, data.typeStats().episodicCount());
        assertEquals(1, data.typeStats().semanticCount());
        assertEquals(3, data.typeStats().proceduralCount());
        assertEquals(1, data.typeStats().perceptiveCount());
        assertEquals(1, data.typeStats().workingMemoryCount());
    }

    @Test
    void testGenerateVisualizationWithEmptyLists() {
        MemoryVisualizationService.MemoryVisualizationData data = service.generateVisualization(
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        );

        assertNotNull(data);
        assertEquals(0, data.typeStats().episodicCount());
        assertEquals(0, data.typeStats().semanticCount());
        assertEquals(0, data.typeStats().proceduralCount());
        assertEquals(0, data.typeStats().perceptiveCount());
        assertEquals(0, data.totalMemoryCount());
    }

    @Test
    void testGenerateVisualizationWithNullLists() {
        MemoryVisualizationService.MemoryVisualizationData data = service.generateVisualization(
            null, null, null, null, null
        );

        assertNotNull(data);
        assertEquals(0, data.typeStats().episodicCount());
        assertEquals(0, data.typeStats().semanticCount());
        assertEquals(0, data.totalMemoryCount());
    }

    @Test
    void testGenerateVisualizationWithPartialNulls() {
        MemoryVisualizationService.MemoryVisualizationData data = service.generateVisualization(
            List.of("episodic1"),
            null,
            List.of("procedural1"),
            null,
            List.of("working1")
        );

        assertNotNull(data);
        assertEquals(1, data.typeStats().episodicCount());
        assertEquals(0, data.typeStats().semanticCount());
        assertEquals(1, data.typeStats().proceduralCount());
        assertEquals(0, data.typeStats().perceptiveCount());
        assertEquals(1, data.typeStats().workingMemoryCount());
    }

    @Test
    void testGetMemoryTypeDescription() {
        MemoryVisualizationService.MemoryTypeStats stats = new MemoryVisualizationService.MemoryTypeStats(
            5, 3, 2, 1, 1
        );

        String description = service.getMemoryTypeDescription(stats);

        assertNotNull(description);
        assertTrue(description.contains("Total: 11 memories"));
        assertTrue(description.contains("Episodic: 5"));
        assertTrue(description.contains("Semantic: 3"));
        assertTrue(description.contains("Procedural: 2"));
        assertTrue(description.contains("Perceptive: 1"));
    }

    @Test
    void testGetMemoryTypeDescriptionEmpty() {
        MemoryVisualizationService.MemoryTypeStats stats = new MemoryVisualizationService.MemoryTypeStats(
            0, 0, 0, 0, 0
        );

        String description = service.getMemoryTypeDescription(stats);

        assertEquals("No memories stored", description);
    }

    @Test
    void testGetStrengthDistributionDescription() {
        MemoryVisualizationService.StrengthDistribution dist = new MemoryVisualizationService.StrengthDistribution(
            2, 5, 10, 7, 3
        );

        String description = service.getStrengthDistributionDescription(dist);

        assertNotNull(description);
        assertTrue(description.contains("Very High(3)"));
        assertTrue(description.contains("High(7)"));
        assertTrue(description.contains("Medium(10)"));
        assertTrue(description.contains("Low(5)"));
        assertTrue(description.contains("Very Low(2)"));
    }

    @Test
    void testGetStrengthDistributionDescriptionEmpty() {
        MemoryVisualizationService.StrengthDistribution dist = new MemoryVisualizationService.StrengthDistribution(
            0, 0, 0, 0, 0
        );

        String description = service.getStrengthDistributionDescription(dist);

        assertEquals("No memory strength data", description);
    }

    @Test
    void testGenerateTimeline() {
        Instant start = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant end = Instant.now();

        MemoryVisualizationService.MemoryTimeline timeline = service.generateTimeline(
            new ArrayList<>(), start, end
        );

        assertNotNull(timeline);
        assertNotNull(timeline.startDate());
        assertNotNull(timeline.endDate());
        assertNotNull(timeline.entries());
    }

    @Test
    void testGenerateTimelineWithNullDates() {
        MemoryVisualizationService.MemoryTimeline timeline = service.generateTimeline(
            new ArrayList<>(), null, null
        );

        assertNotNull(timeline);
        assertNotNull(timeline.startDate());
        assertNotNull(timeline.endDate());
    }

    @Test
    void testMemoryTypeStatsRecord() {
        MemoryVisualizationService.MemoryTypeStats stats = new MemoryVisualizationService.MemoryTypeStats(
            10, 20, 30, 40, 5
        );

        assertEquals(10, stats.episodicCount());
        assertEquals(20, stats.semanticCount());
        assertEquals(30, stats.proceduralCount());
        assertEquals(40, stats.perceptiveCount());
        assertEquals(5, stats.workingMemoryCount());
    }

    @Test
    void testStrengthDistributionRecord() {
        MemoryVisualizationService.StrengthDistribution dist = new MemoryVisualizationService.StrengthDistribution(
            1, 2, 3, 4, 5
        );

        assertEquals(1, dist.veryLowCount());
        assertEquals(2, dist.lowCount());
        assertEquals(3, dist.mediumCount());
        assertEquals(4, dist.highCount());
        assertEquals(5, dist.veryHighCount());
    }

    @Test
    void testMemoryActivityRecord() {
        Instant now = Instant.now();
        MemoryVisualizationService.MemoryActivity activity = new MemoryVisualizationService.MemoryActivity(
            "mem-1", "episodic", now, 10, 0.75f, "preview text"
        );

        assertEquals("mem-1", activity.memoryId());
        assertEquals("episodic", activity.memoryType());
        assertEquals(now, activity.lastAccessed());
        assertEquals(10, activity.accessCount());
        assertEquals(0.75f, activity.strength());
        assertEquals("preview text", activity.preview());
    }

    @Test
    void testTimelineEntryRecord() {
        Instant now = Instant.now();
        MemoryVisualizationService.MemoryTimeline.TimelineEntry entry =
            new MemoryVisualizationService.MemoryTimeline.TimelineEntry(
                now, "episodic", "test memory", 0.8f
            );

        assertEquals(now, entry.date());
        assertEquals("episodic", entry.memoryType());
        assertEquals("test memory", entry.description());
        assertEquals(0.8f, entry.strength());
    }
}