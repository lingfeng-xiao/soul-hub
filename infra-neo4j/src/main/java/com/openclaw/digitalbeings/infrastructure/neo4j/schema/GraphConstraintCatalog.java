package com.openclaw.digitalbeings.infrastructure.neo4j.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GraphConstraintCatalog {

    private static final Pattern NAMED_STATEMENT_PATTERN = Pattern.compile(
            "CREATE\\s+(?:CONSTRAINT|INDEX)\\s+([a-zA-Z0-9_]+)\\s+IF\\s+NOT\\s+EXISTS.*"
    );

    public static final List<String> CONSTRAINTS = List.of(
            "CREATE CONSTRAINT being_beingId_unique IF NOT EXISTS FOR (n:Being) REQUIRE n.beingId IS UNIQUE",
            "CREATE CONSTRAINT identityFacet_facetId_unique IF NOT EXISTS FOR (n:IdentityFacet) REQUIRE n.facetId IS UNIQUE",
            "CREATE CONSTRAINT relationshipEntity_entityId_unique IF NOT EXISTS FOR (n:RelationshipEntity) REQUIRE n.entityId IS UNIQUE",
            "CREATE CONSTRAINT hostContract_contractId_unique IF NOT EXISTS FOR (n:HostContract) REQUIRE n.contractId IS UNIQUE",
            "CREATE CONSTRAINT runtimeSession_sessionId_unique IF NOT EXISTS FOR (n:RuntimeSession) REQUIRE n.sessionId IS UNIQUE",
            "CREATE CONSTRAINT authorityLease_leaseId_unique IF NOT EXISTS FOR (n:AuthorityLease) REQUIRE n.leaseId IS UNIQUE",
            "CREATE CONSTRAINT reviewItem_reviewItemId_unique IF NOT EXISTS FOR (n:ReviewItem) REQUIRE n.reviewItemId IS UNIQUE",
            "CREATE CONSTRAINT canonicalProjection_projectionId_unique IF NOT EXISTS FOR (n:CanonicalProjection) REQUIRE n.projectionId IS UNIQUE",
            "CREATE CONSTRAINT ownerProfileFact_factId_unique IF NOT EXISTS FOR (n:OwnerProfileFact) REQUIRE n.factId IS UNIQUE",
            "CREATE CONSTRAINT managedAgentSpec_managedAgentId_unique IF NOT EXISTS FOR (n:ManagedAgentSpec) REQUIRE n.managedAgentId IS UNIQUE",
            "CREATE CONSTRAINT continuitySnapshot_snapshotId_unique IF NOT EXISTS FOR (n:ContinuitySnapshot) REQUIRE n.snapshotId IS UNIQUE",
            "CREATE CONSTRAINT domainEvent_eventId_unique IF NOT EXISTS FOR (n:DomainEvent) REQUIRE n.eventId IS UNIQUE"
    );

    public static final List<String> INDEXES = List.of(
            "CREATE INDEX being_revision_idx IF NOT EXISTS FOR (n:Being) ON (n.revision)",
            "CREATE INDEX authorityLease_status_idx IF NOT EXISTS FOR (n:AuthorityLease) ON (n.status)",
            "CREATE INDEX reviewItem_status_idx IF NOT EXISTS FOR (n:ReviewItem) ON (n.status)",
            "CREATE INDEX domainEvent_occurredAt_idx IF NOT EXISTS FOR (n:DomainEvent) ON (n.occurredAt)"
    );

    private GraphConstraintCatalog() {
    }

    public static List<String> constraintNames() {
        return extractNames(CONSTRAINTS);
    }

    public static List<String> indexNames() {
        return extractNames(INDEXES);
    }

    private static List<String> extractNames(List<String> statements) {
        List<String> names = new ArrayList<>(statements.size());
        for (String statement : statements) {
            Matcher matcher = NAMED_STATEMENT_PATTERN.matcher(statement);
            if (!matcher.matches()) {
                throw new IllegalStateException("Unable to extract catalog name from statement: " + statement);
            }
            names.add(matcher.group(1));
        }
        return List.copyOf(names);
    }
}
