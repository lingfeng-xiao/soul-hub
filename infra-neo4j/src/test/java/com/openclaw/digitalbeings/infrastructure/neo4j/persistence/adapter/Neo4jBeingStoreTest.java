package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.adapter;

import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.core.ReviewItemStatus;
import com.openclaw.digitalbeings.domain.core.SnapshotType;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.BeingNode;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.mapper.BeingNodeMapper;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.repository.BeingNodeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Neo4jBeingStoreTest {

    @Test
    void saveFindAndListRoundTripThroughExpandedGraphMapper() {
        BeingNodeRepository repository = mock(BeingNodeRepository.class);
        Neo4jBeingStore store = new Neo4jBeingStore(repository);
        Instant now = Instant.parse("2026-03-21T08:30:00Z");

        Being being = createExpandedBeing(now);
        when(repository.save(any(BeingNode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Being persisted = store.save(being);
        assertEquals(being.beingId().value(), persisted.beingId().value());
        assertEquals(1, persisted.identityFacets().size());
        assertEquals(1, persisted.relationships().size());
        assertEquals(1, persisted.hostContracts().size());
        assertEquals(1, persisted.runtimeSessions().size());
        assertEquals(1, persisted.authorityLeases().size());
        assertEquals(1, persisted.reviewItems().size());
        assertTrue(persisted.canonicalProjection().isPresent());
        assertEquals(1, persisted.ownerProfileFacts().size());
        assertEquals(1, persisted.managedAgentSpecs().size());
        assertEquals(1, persisted.continuitySnapshots().size());
        assertEquals(13, persisted.domainEvents().size());

        BeingNode persistedNode = BeingNodeMapper.fromDomain(being);
        when(repository.findByBeingId(being.beingId().value())).thenReturn(Optional.of(persistedNode));
        when(repository.findAll()).thenReturn(List.of(persistedNode));

        assertTrue(store.findById(being.beingId().value()).isPresent());
        assertEquals(1, store.findAll().size());
        assertEquals(being.beingId().value(), store.findById(being.beingId().value()).orElseThrow().beingId().value());
        assertTrue(store.findById(being.beingId().value()).orElseThrow().canonicalProjection().isPresent());
    }

    private static Being createExpandedBeing(Instant now) {
        Being being = Being.create("guan-guan", "codex", now);
        being.addIdentityFacet("persona", "playful and observant", "codex", now.plusSeconds(1));
        being.addRelationshipEntity("friend", "lingfeng", "codex", now.plusSeconds(2));
        being.registerHostContract("codex", "codex", now.plusSeconds(3));
        String sessionId = being.registerRuntimeSession("codex", "codex", now.plusSeconds(4)).sessionId();
        being.acquireAuthorityLease(sessionId, "codex", now.plusSeconds(5));
        String reviewId = being.draftReview("canonical", "identity", "accept a canonical tweak", "codex", now.plusSeconds(6)).reviewItemId();
        being.submitReview(reviewId, "codex", now.plusSeconds(7));
        being.decideReview(reviewId, ReviewItemStatus.ACCEPTED, "codex", now.plusSeconds(8));
        being.rebuildCanonicalProjection("codex", now.plusSeconds(9));
        being.recordOwnerProfileFact("preferences", "tone", "prefers warm collaboration", "codex", now.plusSeconds(10));
        being.registerManagedAgentSpec("planner", "ACTIVE", "codex", now.plusSeconds(11));
        being.createSnapshot(SnapshotType.MILESTONE, "phase-1 seam checkpoint", "codex", now.plusSeconds(12));
        return being;
    }
}
