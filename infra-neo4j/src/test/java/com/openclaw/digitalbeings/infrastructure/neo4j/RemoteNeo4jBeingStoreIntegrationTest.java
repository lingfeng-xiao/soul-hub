package com.openclaw.digitalbeings.infrastructure.neo4j;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.core.ReviewItemStatus;
import com.openclaw.digitalbeings.domain.core.SnapshotType;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.adapter.Neo4jBeingStore;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.BeingNode;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.repository.BeingNodeRepository;
import com.openclaw.digitalbeings.testkit.RemoteNeo4jConnectionSupport;
import com.openclaw.digitalbeings.testkit.RequiresRemoteNeo4j;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiresRemoteNeo4j
@DataNeo4jTest
@org.springframework.test.context.ContextConfiguration(classes = RemoteNeo4jBeingStoreIntegrationTest.TestApplication.class)
class RemoteNeo4jBeingStoreIntegrationTest {

    @Autowired
    private BeingNodeRepository repository;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableNeo4jRepositories(basePackageClasses = BeingNodeRepository.class)
    @EntityScan(basePackageClasses = BeingNode.class)
    static class TestApplication {
    }

    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        var details = RemoteNeo4jConnectionSupport.requireConnectionDetails();
        registry.add("spring.neo4j.uri", details::boltUrl);
        registry.add("spring.neo4j.authentication.username", details::username);
        registry.add("spring.neo4j.authentication.password", details::password);
    }

    @Test
    void neo4jStorePersistsAndReloadsTheExpandedAggregateAgainstTheRemoteTarget() {
        BeingStore store = new Neo4jBeingStore(repository);
        Instant now = Instant.parse("2026-03-21T08:30:00Z");

        Being being = createExpandedBeing(now);

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

        BeingNode reloaded = repository.findByBeingId(being.beingId().value()).orElseThrow();
        assertEquals(being.beingId().value(), reloaded.getBeingId());
        assertEquals(1, reloaded.getIdentityFacets().size());
        assertEquals(1, reloaded.getRelationships().size());
        assertEquals(1, reloaded.getHostContracts().size());
        assertEquals(1, reloaded.getRuntimeSessions().size());
        assertEquals(1, reloaded.getAuthorityLeases().size());
        assertEquals(1, reloaded.getReviewItems().size());
        assertNotNull(reloaded.getCanonicalProjection());
        assertEquals(1, reloaded.getOwnerProfileFacts().size());
        assertEquals(1, reloaded.getManagedAgentSpecs().size());
        assertEquals(1, reloaded.getContinuitySnapshots().size());
        assertEquals(13, reloaded.getDomainEvents().size());
        assertTrue(store.findById(being.beingId().value()).isPresent());
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
