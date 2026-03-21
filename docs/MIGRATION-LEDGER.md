# Migration Ledger

## Status

- Phase: not started
- Source Repository: `C:\Users\16343\.openclaw\digital-beings`
- Mode: one-time import only
- Backlog Requirement: `REQ-030`
- Planned Downstream Task: `JAVA-006`

## Planned Import Coverage

| Source Area | Target Area | Status | Notes |
| --- | --- | --- | --- |
| `digital-beings/<agent>/being.yaml` | `Being` + `IdentityFacet` | planned | needs explicit field mapping |
| `relationships.yaml` | `RelationshipEntity` + `RELATES_TO` | planned | graph-first mapping |
| `review-queue/*/review-state.json` | `ReviewItem` + `DomainEvent` | planned | accepted items prioritized |
| `shared/owner-profile.yaml` | `OwnerProfileFact` | planned | summary-only import |
| `runtime-hub/*/lease.json` | `AuthorityLease` | planned | historical migration only |
| snapshots and generated continuity assets | `ContinuitySnapshot` | planned | initial lineage import |

## Deferred Legacy Areas

- runtime patches
- generated markdown mirrors
- live log scanning behavior
- direct host-specific file protocols

## Import Rules

- do not dual-write into the legacy repository
- import must be repeatable in a dry-run mode
- all anomalies must be written into the import report before accepting the run

## Requirement Link Notes

- `REQ-030` owns importer scope, dry-run behavior, anomaly reporting, and final acceptance of the migration path.
- `REQ-011` and `REQ-012` are upstream functional dependencies because owner profile and snapshot targets must exist before importer scope can be closed.
