CREATE CONSTRAINT being_beingId_unique IF NOT EXISTS FOR (n:Being) REQUIRE n.beingId IS UNIQUE;
CREATE CONSTRAINT identityFacet_facetId_unique IF NOT EXISTS FOR (n:IdentityFacet) REQUIRE n.facetId IS UNIQUE;
CREATE CONSTRAINT relationshipEntity_entityId_unique IF NOT EXISTS FOR (n:RelationshipEntity) REQUIRE n.entityId IS UNIQUE;
CREATE CONSTRAINT hostContract_contractId_unique IF NOT EXISTS FOR (n:HostContract) REQUIRE n.contractId IS UNIQUE;
CREATE CONSTRAINT runtimeSession_sessionId_unique IF NOT EXISTS FOR (n:RuntimeSession) REQUIRE n.sessionId IS UNIQUE;
CREATE CONSTRAINT authorityLease_leaseId_unique IF NOT EXISTS FOR (n:AuthorityLease) REQUIRE n.leaseId IS UNIQUE;
CREATE CONSTRAINT reviewItem_reviewItemId_unique IF NOT EXISTS FOR (n:ReviewItem) REQUIRE n.reviewItemId IS UNIQUE;
CREATE CONSTRAINT ownerProfileFact_factId_unique IF NOT EXISTS FOR (n:OwnerProfileFact) REQUIRE n.factId IS UNIQUE;
CREATE CONSTRAINT managedAgentSpec_managedAgentId_unique IF NOT EXISTS FOR (n:ManagedAgentSpec) REQUIRE n.managedAgentId IS UNIQUE;
CREATE CONSTRAINT continuitySnapshot_snapshotId_unique IF NOT EXISTS FOR (n:ContinuitySnapshot) REQUIRE n.snapshotId IS UNIQUE;
CREATE CONSTRAINT domainEvent_eventId_unique IF NOT EXISTS FOR (n:DomainEvent) REQUIRE n.eventId IS UNIQUE;

CREATE INDEX being_revision_idx IF NOT EXISTS FOR (n:Being) ON (n.revision);
CREATE INDEX authorityLease_status_idx IF NOT EXISTS FOR (n:AuthorityLease) ON (n.status);
CREATE INDEX reviewItem_status_idx IF NOT EXISTS FOR (n:ReviewItem) ON (n.status);
CREATE INDEX domainEvent_occurredAt_idx IF NOT EXISTS FOR (n:DomainEvent) ON (n.occurredAt);
