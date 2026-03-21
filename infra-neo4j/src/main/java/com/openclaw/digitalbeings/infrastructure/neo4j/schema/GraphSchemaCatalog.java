package com.openclaw.digitalbeings.infrastructure.neo4j.schema;

import java.util.List;

public final class GraphSchemaCatalog {

    public static final List<String> NODE_LABELS = List.of(
            "Being",
            "IdentityFacet",
            "RelationshipEntity",
            "HostContract",
            "RuntimeSession",
            "AuthorityLease",
            "ReviewItem",
            "CanonicalProjection",
            "OwnerProfileFact",
            "ManagedAgentSpec",
            "ContinuitySnapshot",
            "DomainEvent"
    );

    public static final List<String> EDGE_TYPES = List.of(
            "HAS_IDENTITY",
            "RELATES_TO",
            "ALLOWED_ON",
            "HAS_SESSION",
            "HOLDS_LEASE",
            "HAS_REVIEW_ITEM",
            "HAS_PROFILE_FACT",
            "GOVERNS",
            "HAS_SNAPSHOT",
            "EMITTED_EVENT",
            "AFFECTS"
    );

    private GraphSchemaCatalog() {
    }
}
