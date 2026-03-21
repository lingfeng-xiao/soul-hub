# Schema Graph

## Bounded Context Mapping

| Context | Primary Package | Notes |
| --- | --- | --- |
| Aggregate Root | `com.openclaw.digitalbeings.domain.being` | `Being` owns the first transactional boundary for stage 1 and stage 2 |
| Identity | `com.openclaw.digitalbeings.domain.identity` | identity facets and relationship entities |
| Runtime Authority | `com.openclaw.digitalbeings.domain.runtime` | host contracts, runtime sessions, and authority leases |
| Review Canonicalization | `com.openclaw.digitalbeings.domain.review` | review state machine and canonical projection rebuild |
| Governance | `com.openclaw.digitalbeings.domain.governance` | owner profile facts and managed agent specs |
| Snapshot Continuity | `com.openclaw.digitalbeings.domain.snapshot` | continuity snapshot lifecycle |
| Audit Events | `com.openclaw.digitalbeings.domain.events` | state-changing domain event records |

## Node Labels

| Label | Purpose | Planned Key Properties |
| --- | --- | --- |
| `Being` | top-level aggregate root | `beingId`, `name`, `status`, `revision`, `createdAt` |
| `IdentityFacet` | stable identity traits and facets | `facetId`, `kind`, `summary`, `revision` |
| `RelationshipEntity` | users, siblings, hosts, or external entities | `entityId`, `kind`, `displayName` |
| `HostContract` | allowed host contract for a being | `contractId`, `hostType`, `status` |
| `RuntimeSession` | runtime session metadata | `sessionId`, `hostType`, `status`, `startedAt` |
| `AuthorityLease` | authoritative write lease | `leaseId`, `status`, `grantedAt`, `releasedAt` |
| `ReviewItem` | governance review candidate | `reviewItemId`, `status`, `lane`, `kind` |
| `CanonicalProjection` | accepted projection output | `projectionId`, `version`, `generatedAt` |
| `OwnerProfileFact` | accepted owner profile summary fact | `factId`, `section`, `key`, `summary` |
| `ManagedAgentSpec` | managed ordinary agent specification | `managedAgentId`, `role`, `status` |
| `ContinuitySnapshot` | continuity export or restore marker | `snapshotId`, `type`, `createdAt` |
| `DomainEvent` | durable audit event | `eventId`, `eventType`, `occurredAt`, `actor` |

## Relationship Types

| Type | From | To | Purpose |
| --- | --- | --- | --- |
| `HAS_IDENTITY` | `Being` | `IdentityFacet` | attach identity facets to a being |
| `RELATES_TO` | `Being` | `RelationshipEntity` | maintain first-class relationship graph |
| `ALLOWED_ON` | `Being` | `HostContract` | declare legal host contracts |
| `HAS_SESSION` | `Being` | `RuntimeSession` | attach runtime sessions |
| `HOLDS_LEASE` | `Being` | `AuthorityLease` | record active or historical leases |
| `HAS_REVIEW_ITEM` | `Being` | `ReviewItem` | record governance review items |
| `HAS_PROFILE_FACT` | `Being` | `OwnerProfileFact` | record shared owner profile facts |
| `GOVERNS` | `Being` | `ManagedAgentSpec` | link governance owner to managed agent specs |
| `HAS_SNAPSHOT` | `Being` | `ContinuitySnapshot` | link continuity snapshots |
| `EMITTED_EVENT` | `Being` | `DomainEvent` | attach audit events to a being |
| `AFFECTS` | `Being` | `CanonicalProjection` | attach the current canonical projection to the aggregate root; future event-impact edges remain planned |

## Planned Constraints

- `Being.beingId` unique
- `IdentityFacet.facetId` unique
- `RelationshipEntity.entityId` unique
- `HostContract.contractId` unique
- `RuntimeSession.sessionId` unique
- `AuthorityLease.leaseId` unique
- `ReviewItem.reviewItemId` unique
- `CanonicalProjection.projectionId` unique
- `OwnerProfileFact.factId` unique
- `ManagedAgentSpec.managedAgentId` unique
- `ContinuitySnapshot.snapshotId` unique
- `DomainEvent.eventId` unique
- at most one `AuthorityLease` with status `ACTIVE` per `Being` via aggregate and service-layer transaction checks

## Lifecycle Rules

- only accepted review items may influence canonical projections
- only one active authoritative lease may exist for a given being
- snapshot restore may not silently overwrite an active lease
- every state-changing write must emit a `DomainEvent`

## Current State

- stage 1 domain aggregate and bounded-context classes are implemented in `domain-core`
- invariant coverage exists for:
  - single active authoritative lease
  - review state transitions
  - accepted-only canonical projection inputs
  - post-restore snapshot safety
- stage 2 and stage 3 schema assets exist in:
  - `infra-neo4j/src/main/java/com/openclaw/digitalbeings/infrastructure/neo4j/schema/GraphConstraintCatalog.java`
  - `infra-neo4j/src/main/resources/neo4j/migrations/V001__baseline_graph.cypher`
  - `infra-neo4j/src/main/resources/neo4j/migrations/V002__canonical_projection_graph.cypher`
- Spring Data Neo4j node mappings and the repository-backed aggregate adapter are implemented for:
  - `Being`
  - `IdentityFacet`
  - `RelationshipEntity`
  - `HostContract`
  - `RuntimeSession`
  - `AuthorityLease`
  - `ReviewItem`
  - `CanonicalProjection`
  - `OwnerProfileFact`
  - `ManagedAgentSpec`
  - `ContinuitySnapshot`
  - `DomainEvent`
- stage 3 application service coverage now exists for:
  - `RelationshipService`
  - `HostContractService`
  - `GovernanceService`
  - `SnapshotService`
- real migration execution, persistence-backed smoke validation, and expanded aggregate roundtrip verification are now closed stage 2 and stage 3 prerequisites

## Requirement Links

- `REQ-003` owns stage 2 migration and persistence validation closeout for the graph slice and its canonical projection constraint follow-up.
- `REQ-010` owns relationship and host-contract service completion against this graph model.
- `REQ-011` owns owner profile fact and managed agent spec expansion against this graph model.
- `REQ-012` owns snapshot continuity service expansion against this graph model.
