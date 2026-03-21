package com.openclaw.digitalbeings.infrastructure.neo4j.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphConstraintCatalogTest {

    @Test
    void constraintCatalogContainsUniqueLeaseConstraint() {
        assertTrue(
                GraphConstraintCatalog.CONSTRAINTS.stream()
                        .anyMatch(statement -> statement.contains("authorityLease_leaseId_unique"))
        );
    }

    @Test
    void constraintCatalogContainsCanonicalProjectionConstraint() {
        assertTrue(
                GraphConstraintCatalog.CONSTRAINTS.stream()
                        .anyMatch(statement -> statement.contains("canonicalProjection_projectionId_unique"))
        );
    }

    @Test
    void indexCatalogContainsOperationalIndexes() {
        assertFalse(GraphConstraintCatalog.INDEXES.isEmpty());
    }

    @Test
    void catalogNameHelpersStayAlignedWithCatalogStatements() {
        assertEquals(GraphConstraintCatalog.CONSTRAINTS.size(), GraphConstraintCatalog.constraintNames().size());
        assertEquals(GraphConstraintCatalog.INDEXES.size(), GraphConstraintCatalog.indexNames().size());
        assertTrue(GraphConstraintCatalog.constraintNames().contains("being_beingId_unique"));
        assertTrue(GraphConstraintCatalog.indexNames().contains("being_revision_idx"));
    }
}
