package com.lingfeng.sprite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CognitionDashboardService 单元测试
 */
class CognitionDashboardServiceTest {

    private CognitionDashboardService service;

    @BeforeEach
    void setUp() {
        service = new CognitionDashboardService();
    }

    @Test
    void testRecordEvent() {
        service.recordEvent(CognitionDashboardService.CognitionPhase.PERCEPTION, "test event", 10.5f, true);

        List<CognitionDashboardService.CognitionEvent> events = service.getRecentEvents(10);
        assertEquals(1, events.size());
        assertEquals(CognitionDashboardService.CognitionPhase.PERCEPTION, events.get(0).phase());
        assertEquals("test event", events.get(0).description());
        assertEquals(10.5f, events.get(0).durationMs());
        assertTrue(events.get(0).success());
    }

    @Test
    void testRecordPerception() {
        service.recordPerception("perception test", 5.0f, true);

        List<CognitionDashboardService.CognitionEvent> events = service.getRecentEvents(1);
        assertEquals(1, events.size());
        assertEquals(CognitionDashboardService.CognitionPhase.PERCEPTION, events.get(0).phase());
    }

    @Test
    void testRecordContextBuilding() {
        service.recordContextBuilding("context build", 8.0f, true);

        List<CognitionDashboardService.CognitionEvent> events = service.getRecentEvents(1);
        assertEquals(1, events.size());
        assertEquals(CognitionDashboardService.CognitionPhase.CONTEXT_BUILD, events.get(0).phase());
    }

    @Test
    void testRecordReasoning() {
        service.recordReasoning("reasoning test", 15.0f, true);

        List<CognitionDashboardService.CognitionEvent> events = service.getRecentEvents(1);
        assertEquals(1, events.size());
        assertEquals(CognitionDashboardService.CognitionPhase.REASONING, events.get(0).phase());
    }

    @Test
    void testRecordDecision() {
        service.recordDecision("decision test", 3.0f, true);

        List<CognitionDashboardService.CognitionEvent> events = service.getRecentEvents(1);
        assertEquals(1, events.size());
        assertEquals(CognitionDashboardService.CognitionPhase.DECISION, events.get(0).phase());
    }

    @Test
    void testRecordAction() {
        service.recordAction("action test", 2.0f, true);

        List<CognitionDashboardService.CognitionEvent> events = service.getRecentEvents(1);
        assertEquals(1, events.size());
        assertEquals(CognitionDashboardService.CognitionPhase.ACTION, events.get(0).phase());
    }

    @Test
    void testRecordLearning() {
        service.recordLearning("learning test", 7.0f, true);

        List<CognitionDashboardService.CognitionEvent> events = service.getRecentEvents(1);
        assertEquals(1, events.size());
        assertEquals(CognitionDashboardService.CognitionPhase.LEARNING, events.get(0).phase());
    }

    @Test
    void testGetRecentEvents() {
        for (int i = 0; i < 5; i++) {
            service.recordPerception("event " + i, 1.0f, true);
        }

        List<CognitionDashboardService.CognitionEvent> recent = service.getRecentEvents(3);
        assertEquals(3, recent.size());
    }

    @Test
    void testGetRecentEventsWithCountExceedingSize() {
        service.recordPerception("single event", 1.0f, true);

        List<CognitionDashboardService.CognitionEvent> events = service.getRecentEvents(100);
        assertEquals(1, events.size());
    }

    @Test
    void testGetRecentEventsEmpty() {
        List<CognitionDashboardService.CognitionEvent> events = service.getRecentEvents(10);
        assertTrue(events.isEmpty());
    }

    @Test
    void testGetAllEvents() {
        service.recordPerception("event 1", 1.0f, true);
        service.recordReasoning("event 2", 2.0f, true);

        List<CognitionDashboardService.CognitionEvent> all = service.getAllEvents();
        assertEquals(2, all.size());
    }

    @Test
    void testGetEventsByPhase() {
        service.recordPerception("perception 1", 1.0f, true);
        service.recordReasoning("reasoning 1", 2.0f, true);
        service.recordPerception("perception 2", 1.0f, true);

        List<CognitionDashboardService.CognitionEvent> perceptionEvents = service.getEventsByPhase(CognitionDashboardService.CognitionPhase.PERCEPTION);
        assertEquals(2, perceptionEvents.size());

        List<CognitionDashboardService.CognitionEvent> reasoningEvents = service.getEventsByPhase(CognitionDashboardService.CognitionPhase.REASONING);
        assertEquals(1, reasoningEvents.size());
    }

    @Test
    void testGetDashboardData() {
        service.recordPerception("perception", 10.0f, true);
        service.recordReasoning("reasoning", 20.0f, true);
        service.recordDecision("decision", 5.0f, true);
        service.recordAction("action", 2.0f, true);

        CognitionDashboardService.CognitionDashboardData data = service.getDashboardData();

        assertNotNull(data);
        assertNotNull(data.timestamp());
        assertEquals(4, data.totalEvents());
        assertEquals(4, data.successCount());
        assertEquals(100.0f, data.successRate());
        assertNotNull(data.phaseStats());
        assertNotNull(data.recentCycles());
        assertTrue(data.totalHistorySize() > 0);
    }

    @Test
    void testGetDashboardDataEmpty() {
        CognitionDashboardService.CognitionDashboardData data = service.getDashboardData();

        assertNotNull(data);
        assertEquals(0, data.totalEvents());
        assertEquals(0, data.successCount());
        assertEquals(0.0f, data.successRate());
    }

    @Test
    void testGetDashboardDataPartialSuccess() {
        service.recordPerception("success", 10.0f, true);
        service.recordReasoning("failure", 20.0f, false);

        CognitionDashboardService.CognitionDashboardData data = service.getDashboardData();

        assertEquals(2, data.totalEvents());
        assertEquals(1, data.successCount());
        assertEquals(50.0f, data.successRate());
    }

    @Test
    void testMaxHistoryLimit() {
        // Record more events than MAX_HISTORY (200)
        for (int i = 0; i < 250; i++) {
            service.recordPerception("event " + i, 1.0f, true);
        }

        List<CognitionDashboardService.CognitionEvent> all = service.getAllEvents();
        assertEquals(200, all.size());
    }

    @Test
    void testGetPhaseSummary() {
        service.recordPerception("perception", 10.0f, true);
        service.recordReasoning("reasoning", 20.0f, true);

        String summary = service.getPhaseSummary();

        assertNotNull(summary);
        assertTrue(summary.contains("PERCEPTION"));
        assertTrue(summary.contains("REASONING"));
        assertTrue(summary.contains("1 events"));
    }

    @Test
    void testGetPhaseSummaryEmpty() {
        String summary = service.getPhaseSummary();

        assertNotNull(summary);
        assertEquals("No cognition events recorded", summary);
    }

    @Test
    void testCognitionPhaseEnumValues() {
        CognitionDashboardService.CognitionPhase[] phases = CognitionDashboardService.CognitionPhase.values();
        assertEquals(6, phases.length);
        assertEquals(CognitionDashboardService.CognitionPhase.PERCEPTION, phases[0]);
        assertEquals(CognitionDashboardService.CognitionPhase.LEARNING, phases[5]);
    }
}