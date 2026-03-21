# ADR-002 Neo4j As The Authoritative Graph Store

## Background

The target model is graph-heavy: beings, relationships, contracts, leases, review items, projections, and audit links are all first-class connected entities.

## Decision

Use Neo4j as the single authoritative state store for the Java rewrite. All durable writes must persist state and a corresponding audit event into the graph.

## Impact

- aligns storage with the conceptual graph model
- reduces impedance mismatch for relationship and review navigation
- enables graph consistency checks as a first-class platform capability

## Rejected Alternatives

- relational first model in Postgres
- continued file-backed authority
- full event sourcing before basic operational stability

## Rollback Conditions

Revisit only if Neo4j proves unable to support required transactional guarantees or operational complexity overwhelms delivery.
