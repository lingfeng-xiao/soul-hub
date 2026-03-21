# Test Status

## Current Snapshot

- Stage: 2 passed, Stage 3 passed, Stage 4 passed, Stage 5/6 in progress
- Overall Status: green build with a live remote Neo4j verification node, completed stage 4 interface closeout, and no local Docker
- Governance Tracking: `docs/REQUIREMENT-BACKLOG.md` is now the upstream phase-planning source
- Parallel Execution Tracking: Wave scheduling is now defined in `docs/PARALLEL-EXECUTION-PLAN.md`
- Next Planning Gate: `REQ-030` / `JAVA-033` is the lead lane now that `JAVA-032` and `JAVA-034` are complete
- Last Updated: 2026-03-21T20:00:00+08:00

## Test Matrix

| Layer | Command | Status | Notes |
| --- | --- | --- | --- |
| Domain unit tests | `./gradlew.bat :domain-core:test` | passed | aggregate invariants and core contracts are covered |
| Application services | `./gradlew.bat :application:test` | passed | now covers being, lease, review, relationship, host contract, governance, and snapshot application slices |
| Governance backend slice | `./gradlew.bat :application:test` | passed | `GovernanceBackendService` now covers cockpit summary, projection rebuild orchestration, and owner profile compilation |
| Infra schema and adapter tests | `./gradlew.bat :infra-neo4j:test` | passed | persistence slice now roundtrips the expanded aggregate, including canonical projection and stage 3 child nodes |
| Testkit unit tests | `./gradlew.bat :testkit:test` | passed | reusable Neo4j/Testcontainers helpers are covered without requiring Docker |
| Boot context | `./gradlew.bat :boot-app:test` | passed | Spring Boot context loads without requiring a live Neo4j instance |
| CLI slice | `./gradlew.bat :interfaces-cli:test` | passed | Picocli command tree now covers being, lease, review, projection, relationship, host contract, owner profile, managed agent, and snapshot flows, plus `table/json` output modes |
| REST slice | `./gradlew.bat :interfaces-rest:test` | passed | controller coverage now exists for beings, leases, reviews, relationship, host contract, governance, snapshot, and platform status, with unified success/error envelopes |
| Full baseline build | `./gradlew.bat build` | passed | full multi-module build is green |
| Legacy importer dry-run slice | `./gradlew.bat :legacy-importer:test` | passed | dry-run reporting and source discovery now have focused coverage |
| Local Neo4j compose | `docker compose up -d` | blocked | Docker is not installed on this machine |
| Remote verification node probe | `py -3.12 ops/remote/probe_server.py` | passed | server is reachable and now has Docker, Docker Compose, Java 21, and password-backed sudo |
| Remote Neo4j smoke container | `py -3.12 ops/remote/start_neo4j.py` | passed | remote Neo4j container is recreated successfully and exposes Bolt and HTTP endpoints |
| Remote Neo4j migration smoke | `./gradlew.bat :infra-neo4j:test --tests='com.openclaw.digitalbeings.infrastructure.neo4j.schema.RemoteNeo4jMigrationSmokeTest'` | passed | baseline graph constraints and indexes were applied and verified against the remote node |
| Remote Neo4j adapter integration | `./gradlew.bat :infra-neo4j:test --tests='com.openclaw.digitalbeings.infrastructure.neo4j.RemoteNeo4jBeingStoreIntegrationTest'` | passed | the real `Neo4jBeingStore` persisted and reloaded an aggregate against the remote target |
| App startup baseline | `./gradlew.bat :boot-app:test` | passed | context-load verification is the current non-interactive startup check |
| Persistence-backed app smoke | `py -3.12 ops/remote/run_neo4j_smoke.py` | passed | `DigitalBeingsNeo4jSmokeIT` confirmed the `neo4j` profile uses `Neo4jBeingStore` and writes to the remote database |

## Stage Gates

- Stage 0 gate:
  - satisfied for buildable baseline
  - local compose verification remains blocked by missing Docker
- Stage 1 gate:
  - passed
- Stage 2 gate:
  - passed
  - real remote smoke, migration execution, and adapter integration verification all passed against the remote Neo4j node
- Stage 3 gate:
  - passed
  - shared extension seam landed through `JAVA-025`
  - application service coverage landed for relationships, host contracts, governance, and snapshots through `JAVA-026`, `JAVA-027`, and `JAVA-028`
- Stage 4 gate:
  - passed
  - REST and CLI parity now covers the planned V1 resource surface through `JAVA-029` and `JAVA-030`
  - contract unification landed through `JAVA-031`, including shared envelopes, error-code families, and CLI `table/json` output
- Stage 5 gate:
  - in progress
  - `JAVA-032` is complete and provides the dry-run report core
  - `JAVA-033` is the remaining importer replay/reporting lane
- Stage 6 gate:
  - in progress
  - `JAVA-034` is complete and provides the governance backend slice
  - `JAVA-035` remains open for jobs and operational reports
