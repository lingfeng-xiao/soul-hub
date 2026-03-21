# ADR-004 Being Aggregate Boundary

- Status: accepted
- Date: 2026-03-21

## Context

The Java rewrite needs a first domain model before persistence adapters exist. The model must express several cross-cutting invariants:

- a being may have only one active authoritative lease at a time
- only accepted review items may influence canonical projections
- post-restore snapshots may not be created while an active lease exists
- state-changing actions must emit domain events

Those rules span multiple concepts that belong to different bounded contexts.

## Decision

Use `Being` as the primary stage 1 aggregate and consistency boundary.

- `Being` owns runtime sessions, authority leases, review items, canonical projection state, owner profile facts, managed agent specs, continuity snapshots, and emitted domain events
- stage 1 invariants are enforced inside the aggregate before any persistence layer is introduced
- stage 2 persistence work will map this aggregate into Neo4j while preserving the same consistency rules

## Consequences

- stage 1 unit tests can validate important cross-context rules without a database
- stage 2 repository design has a clear transactional boundary
- future bounded contexts can still expose separate services, but writes that mutate a single being must continue to respect the aggregate rules

## Rejected Alternatives

- free-floating entities without a governing aggregate
- fully isolated per-context aggregates with cross-aggregate eventual consistency from day one

## Rollback Conditions

Revisit this ADR if:

- a single `Being` aggregate becomes too large to update transactionally in Neo4j
- host-adapter write volume requires splitting operational subgraphs into separate consistency boundaries
