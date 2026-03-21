package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceEntityAnnotationTest {

    @Test
    void beingNodeIsAnnotatedAsNeo4jNode() {
        assertTrue(BeingNode.class.isAnnotationPresent(Node.class));
        assertEquals("Being", BeingNode.class.getAnnotation(Node.class).value()[0]);
    }

    @Test
    void beingNodeExposesExpandedRelationshipFields() throws Exception {
        Map<String, String> expectedRelationships = Map.ofEntries(
                Map.entry("identityFacets", "HAS_IDENTITY"),
                Map.entry("relationships", "RELATES_TO"),
                Map.entry("hostContracts", "ALLOWED_ON"),
                Map.entry("authorityLeases", "HOLDS_LEASE"),
                Map.entry("runtimeSessions", "HAS_SESSION"),
                Map.entry("reviewItems", "HAS_REVIEW_ITEM"),
                Map.entry("canonicalProjection", "AFFECTS"),
                Map.entry("ownerProfileFacts", "HAS_PROFILE_FACT"),
                Map.entry("managedAgentSpecs", "GOVERNS"),
                Map.entry("continuitySnapshots", "HAS_SNAPSHOT"),
                Map.entry("domainEvents", "EMITTED_EVENT")
        );

        for (var entry : expectedRelationships.entrySet()) {
            Field field = BeingNode.class.getDeclaredField(entry.getKey());
            Relationship relationship = field.getAnnotation(Relationship.class);
            assertTrue(relationship != null, () -> "Missing @Relationship on " + entry.getKey());
            assertEquals(entry.getValue(), relationship.type(), () -> "Unexpected type on " + entry.getKey());
        }
    }

    @Test
    void childEntitiesHaveStableNodeAnnotations() {
        assertEquals("IdentityFacet", IdentityFacetNode.class.getAnnotation(Node.class).value()[0]);
        assertEquals("RelationshipEntity", RelationshipEntityNode.class.getAnnotation(Node.class).value()[0]);
        assertEquals("HostContract", HostContractNode.class.getAnnotation(Node.class).value()[0]);
        assertEquals("RuntimeSession", RuntimeSessionNode.class.getAnnotation(Node.class).value()[0]);
        assertEquals("AuthorityLease", AuthorityLeaseNode.class.getAnnotation(Node.class).value()[0]);
        assertEquals("ReviewItem", ReviewItemNode.class.getAnnotation(Node.class).value()[0]);
        assertEquals("CanonicalProjection", CanonicalProjectionNode.class.getAnnotation(Node.class).value()[0]);
        assertEquals("OwnerProfileFact", OwnerProfileFactNode.class.getAnnotation(Node.class).value()[0]);
        assertEquals("ManagedAgentSpec", ManagedAgentSpecNode.class.getAnnotation(Node.class).value()[0]);
        assertEquals("ContinuitySnapshot", ContinuitySnapshotNode.class.getAnnotation(Node.class).value()[0]);
        assertEquals("DomainEvent", DomainEventNode.class.getAnnotation(Node.class).value()[0]);
    }
}
