package com.openclaw.digitalbeings.application.being;

import com.openclaw.digitalbeings.application.support.InMemoryBeingStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BeingServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-21T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void createBeingReturnsAStableView() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService service = new BeingService(store, CLOCK);

        BeingView view = service.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        assertEquals("guan-guan", view.displayName());
        assertEquals(1L, view.revision());
        assertEquals(1, store.findAll().size());
    }

    @Test
    void listBeingsReflectsSavedAggregates() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService service = new BeingService(store, CLOCK);
        service.createBeing(new CreateBeingCommand("guan-guan", "codex"));
        service.createBeing(new CreateBeingCommand("xiao-xiao", "codex"));

        assertEquals(2, service.listBeings().size());
    }
}
