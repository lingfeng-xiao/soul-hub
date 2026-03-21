package com.openclaw.digitalbeings.application.relationship;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.being.CreateBeingCommand;
import com.openclaw.digitalbeings.application.support.InMemoryBeingStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelationshipServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-21T08:40:00Z"), ZoneOffset.UTC);

    @Test
    void createRelationshipEntityUpdatesTheSameAggregateAndCanBeReadBack() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        RelationshipService relationshipService = new RelationshipService(store, CLOCK);
        BeingView beingView = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        RelationshipEntityView relationshipEntityView = relationshipService.createRelationshipEntity(
                new CreateRelationshipEntityCommand(beingView.beingId(), "friend", "lingfeng", "codex")
        );

        assertEquals(beingView.beingId(), relationshipEntityView.beingId());
        assertEquals("friend", relationshipEntityView.kind());
        assertEquals("lingfeng", relationshipEntityView.displayName());
        assertEquals(1, relationshipService.listRelationshipEntities(beingView.beingId()).size());
        assertEquals(1, relationshipService.getBeing(beingView.beingId()).relationshipCount());
    }
}
