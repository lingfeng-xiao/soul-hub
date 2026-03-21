package com.openclaw.digitalbeings.application.governance;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.being.CreateBeingCommand;
import com.openclaw.digitalbeings.application.support.InMemoryBeingStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GovernanceServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-21T08:40:00Z"), ZoneOffset.UTC);

    @Test
    void recordFactsAndManagedAgentSpecsUseTheSameAggregate() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        GovernanceService governanceService = new GovernanceService(store, CLOCK);
        BeingView view = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        OwnerProfileFactView factView = governanceService.recordOwnerProfileFact(
                new RecordOwnerProfileFactCommand(view.beingId(), "preferences", "tone", "prefers warm collaboration", "codex")
        );
        ManagedAgentSpecView specView = governanceService.registerManagedAgentSpec(
                new RegisterManagedAgentSpecCommand(view.beingId(), "planner", "ACTIVE", "codex")
        );

        assertEquals(view.beingId(), factView.beingId());
        assertEquals(view.beingId(), specView.beingId());
        assertEquals(1, governanceService.listOwnerProfileFacts(view.beingId()).size());
        assertEquals(1, governanceService.listManagedAgentSpecs(view.beingId()).size());
        assertEquals("preferences", governanceService.listOwnerProfileFacts(view.beingId()).getFirst().section());
        assertEquals("planner", governanceService.listManagedAgentSpecs(view.beingId()).getFirst().role());
    }

    @Test
    void listMethodsReturnEmptyCollectionsForNewBeings() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        GovernanceService governanceService = new GovernanceService(store, CLOCK);
        BeingView view = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        assertEquals(0, governanceService.listOwnerProfileFacts(view.beingId()).size());
        assertEquals(0, governanceService.listManagedAgentSpecs(view.beingId()).size());
    }
}
