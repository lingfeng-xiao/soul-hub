package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.mapper;

import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.core.ReviewItemStatus;
import com.openclaw.digitalbeings.domain.core.SnapshotType;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.BeingNode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeingNodeMapperTest {

    @Test
    void mapsExpandedAggregateStateIntoGraphNodeModel() {
        Instant now = Instant.parse("2026-03-21T08:00:00Z");
        Being being = createExpandedBeing(now);

        BeingNode node = BeingNodeMapper.fromDomain(being);

        assertEquals(being.beingId().value(), node.getBeingId());
        assertEquals("guan-guan", node.getDisplayName());
        assertEquals(1, node.getIdentityFacets().size());
        assertEquals(1, node.getRelationships().size());
        assertEquals(1, node.getHostContracts().size());
        assertEquals(1, node.getRuntimeSessions().size());
        assertEquals(1, node.getAuthorityLeases().size());
        assertEquals(1, node.getReviewItems().size());
        assertNotNull(node.getCanonicalProjection());
        assertEquals(1, node.getOwnerProfileFacts().size());
        assertEquals(1, node.getManagedAgentSpecs().size());
        assertEquals(1, node.getContinuitySnapshots().size());
        assertEquals(13, node.getDomainEvents().size());

        Being rehydrated = BeingNodeMapper.toDomain(node);

        assertEquals(being.beingId().value(), rehydrated.beingId().value());
        assertEquals(1, rehydrated.identityFacets().size());
        assertEquals(1, rehydrated.relationships().size());
        assertEquals(1, rehydrated.hostContracts().size());
        assertEquals(1, rehydrated.runtimeSessions().size());
        assertEquals(1, rehydrated.authorityLeases().size());
        assertEquals(1, rehydrated.reviewItems().size());
        assertTrue(rehydrated.canonicalProjection().isPresent());
        assertEquals(1, rehydrated.ownerProfileFacts().size());
        assertEquals(1, rehydrated.managedAgentSpecs().size());
        assertEquals(1, rehydrated.continuitySnapshots().size());
        assertEquals(13, rehydrated.domainEvents().size());
        assertEquals(1L, rehydrated.canonicalProjection().orElseThrow().version());
        assertTrue(rehydrated.activeAuthorityLease().isPresent());
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
