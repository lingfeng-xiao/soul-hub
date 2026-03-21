# Task Board

## Task Governance

- `Requirement` is mandatory for any `in_progress`, `planned`, or post-backlog completed task.
- Historical stage 0 and stage 1 tasks created before `REQ-*` governance may omit the field.
- New execution scope must be created or updated in `docs/REQUIREMENT-BACKLOG.md` before a new `JAVA-*` task is added here.

## In Progress

### JAVA-033 Stage 5 Legacy Importer Full Replay And Reporting

- Requirement: `REQ-030`
- Stage: 5
- Status: in_progress
- Dependencies: `JAVA-032`
- Inputs:
  - dry-run validated importer mapping core
- Outputs:
  - full import replay
  - count report
  - anomaly report
  - graph consistency report
- Acceptance:
  - full import produces counts, anomalies, and graph consistency output
- Related Files:
  - `legacy-importer/`
  - `docs/MIGRATION-LEDGER.md`
- Related Tests:
  - importer full replay tests

### JAVA-035 Stage 6 Governance Jobs And Operational Reports

- Requirement: `REQ-040`
- Stage: 6
- Status: in_progress
- Dependencies: `JAVA-034`
- Inputs:
  - governance backend flows
  - imported or native graph data
- Outputs:
  - stale lease cleanup job
  - graph consistency job
  - operational run reports
- Acceptance:
  - governance loop is end-to-end and auditable
- Related Files:
  - `jobs/`
  - `docs/PROGRAM-STATUS.md`
- Related Tests:
  - governance job tests

## Planned

### JAVA-008 Stage 7 Host Adapters

- Requirement: `REQ-050`
- Stage: 7
- Status: planned
- Dependencies: `JAVA-007`
- Inputs:
  - stable governance core
- Outputs:
  - explicit host event APIs
  - host drift reporting
- Acceptance:
  - OpenClaw and Codex style event submission replaces file-driven runtime writes
- Related Files:
  - future host adapter modules
  - `docs/API-CONTRACT.md`
- Related Tests:
  - host adapter tests

### JAVA-009 Stage 8 Productionization

- Requirement: `REQ-060`
- Stage: 8
- Status: planned
- Dependencies: `JAVA-008`
- Inputs:
  - full platform
- Outputs:
  - auth, backup, restore, disaster recovery, monitoring
- Acceptance:
  - migration and restore drills pass
- Related Files:
  - future ops modules
  - `docs/RESUME-RUNBOOK.md`
- Related Tests:
  - recovery and resilience tests

## Completed

### JAVA-034 Stage 6 Governance Backend Slice

- Requirement: `REQ-040`
- Stage: 6
- Status: completed
- Dependencies: `JAVA-029`, `JAVA-030`, `JAVA-031`, `JAVA-032`
- Inputs:
  - V1 API and CLI
  - stage 5 importer dry-run contract
- Outputs:
  - review cockpit backend
  - projection rebuilds
  - owner profile compilation paths
- Acceptance:
  - governance backend is executable and auditable
- Related Files:
  - `application/`
  - `docs/PROGRAM-STATUS.md`
- Related Tests:
  - `./gradlew.bat :application:test`
  - `./gradlew.bat build`

### JAVA-032 Stage 5 Legacy Importer Dry-Run And Mapping Core

- Requirement: `REQ-030`
- Stage: 5
- Status: completed
- Dependencies: `JAVA-026`, `JAVA-027`, `JAVA-028`
- Inputs:
  - current Python repository data
- Outputs:
  - import mappings
  - dry-run pipeline
  - mapping validation output
- Acceptance:
  - importer can parse and report without mutating the target graph
- Related Files:
  - `legacy-importer/`
  - `docs/MIGRATION-LEDGER.md`
- Related Tests:
  - `./gradlew.bat :legacy-importer:test`
  - `./gradlew.bat build`

### JAVA-031 Stage 4 Contract Unification

- Requirement: `REQ-021`
- Stage: 4
- Status: completed
- Dependencies: `JAVA-029`, `JAVA-030`
- Inputs:
  - expanded REST and CLI surface
- Outputs:
  - unified error-code model
  - normalized response envelope and CLI output conventions
- Acceptance:
  - interface contracts are documented and consistent across REST and CLI
- Related Files:
  - `interfaces-rest/`
  - `interfaces-cli/`
  - `docs/API-CONTRACT.md`
- Related Tests:
  - `./gradlew.bat :interfaces-rest:test`
  - `./gradlew.bat :interfaces-cli:test`
  - `./gradlew.bat build`
  - `py -3.12 ops/remote/run_neo4j_smoke.py`

### JAVA-029 Stage 4 Resource Family A REST And CLI

- Requirement: `REQ-020`
- Stage: 4
- Status: completed
- Dependencies: `JAVA-026`, `JAVA-028`
- Inputs:
  - completed stage 3 seams for relationships, host contracts, and snapshots
- Outputs:
  - REST and CLI resources for `relationships`, `host-contracts`, and `snapshots`
- Acceptance:
  - V1 resource family A is reachable through both REST and CLI
- Related Files:
  - `interfaces-rest/`
  - `interfaces-cli/`
  - `docs/API-CONTRACT.md`
- Related Tests:
  - `./gradlew.bat :interfaces-rest:test :interfaces-cli:test`

### JAVA-030 Stage 4 Resource Family B REST And CLI

- Requirement: `REQ-020`
- Stage: 4
- Status: completed
- Dependencies: `JAVA-027`
- Inputs:
  - completed stage 3 seams for owner profile facts and managed agent specs
- Outputs:
  - REST and CLI resources for `owner-profile-facts` and `managed-agent-specs`
- Acceptance:
  - V1 resource family B is reachable through both REST and CLI
- Related Files:
  - `interfaces-rest/`
  - `interfaces-cli/`
  - `boot-app/`
  - `docs/API-CONTRACT.md`
- Related Tests:
  - `./gradlew.bat :interfaces-rest:test :interfaces-cli:test`

### JAVA-028 Stage 3 Snapshot Continuity Service Slice

- Requirement: `REQ-012`
- Stage: 3
- Status: completed
- Dependencies: `JAVA-025`
- Inputs:
  - stable snapshot domain rules
  - persistence baseline
- Outputs:
  - snapshot application services
  - create, read, and restore protection flows
- Acceptance:
  - snapshot continuity flows are executable through the application layer
- Related Files:
  - `application/`
  - `docs/SCHEMA-GRAPH.md`
- Related Tests:
  - `:application:test`

### JAVA-027 Stage 3 Owner Profile And Managed Agent Service Slice

- Requirement: `REQ-011`
- Stage: 3
- Status: completed
- Dependencies: `JAVA-025`
- Inputs:
  - stable governance and persistence baseline
- Outputs:
  - owner profile fact services
  - managed agent specification services
- Acceptance:
  - governance services exist for owner profile facts and managed agent specs
- Related Files:
  - `application/`
  - `docs/API-CONTRACT.md`
- Related Tests:
  - `:application:test`

### JAVA-026 Stage 3 Relationship And Host Contract Service Slice

- Requirement: `REQ-010`
- Stage: 3
- Status: completed
- Dependencies: `JAVA-025`
- Inputs:
  - stable runtime and graph persistence baseline
- Outputs:
  - relationship and host contract application services
  - command and query DTOs for the relationship context
- Acceptance:
  - relationship and host contract flows can run without legacy scripts
- Related Files:
  - `application/`
  - `docs/SCHEMA-GRAPH.md`
- Related Tests:
  - `:application:test`

### JAVA-025 Stage 3 Shared Extension Seam Decomposition

- Requirement: `REQ-013`
- Stage: 3
- Status: completed
- Dependencies: `JAVA-003`, `JAVA-010`
- Inputs:
  - stable post-stage-2 persistence baseline
  - current stage 3 hotspot map from `docs/PARALLEL-EXECUTION-PLAN.md`
- Outputs:
  - explicit stage 3 shared extension seam
  - recalibrated task dependencies for the three stage 3 service slices
  - documented serial vs parallel write boundaries for stage 3
- Acceptance:
  - stage 3 hotspot ownership is explicit
  - `JAVA-026`, `JAVA-027`, and `JAVA-028` no longer start from an undefined shared seam
- Related Files:
  - `application/`
  - `infra-neo4j/`
  - `docs/PARALLEL-EXECUTION-PLAN.md`
  - `docs/REQUIREMENT-BACKLOG.md`
- Related Tests:
  - `:domain-core:test`
  - `:application:test`
  - `:infra-neo4j:test`
  - `py -3.12 ops/remote/run_neo4j_smoke.py`

### JAVA-018 Stage 2 Remote Persistence Smoke

- Requirement: `REQ-002`
- Stage: 2
- Status: completed
- Dependencies: `JAVA-010`, `JAVA-011`, `JAVA-016`
- Inputs:
  - live remote Neo4j verification node
  - `neo4j` boot profile
  - existing REST and application service paths
- Outputs:
  - reproducible persistence-backed smoke verification flow
  - documented remote runtime command sequence
- Acceptance:
  - a real app path runs against the remote Neo4j node without falling back to `InMemoryBeingStore`
  - verification result is recorded in `PROGRAM-STATUS.md` and `TEST-STATUS.md`
- Related Files:
  - `boot-app/`
  - `ops/remote/`
  - `docs/REMOTE-VERIFICATION-NODE.md`
- Related Tests:
  - `py -3.12 ops/remote/run_neo4j_smoke.py`

### JAVA-010 Stage 2 Application To Neo4j Adapter Integration

- Requirement: `REQ-003`
- Stage: 2
- Status: completed
- Dependencies: `JAVA-003`
- Inputs:
  - `BeingStore` application port
  - `infra-neo4j` SDN entities and repository slice
- Outputs:
  - Neo4j-backed `BeingStore` adapter
  - mapping path between domain aggregate, application services, and persistence layer
- Acceptance:
  - application services can run on a real persistence adapter instead of only the in-memory store
  - integration tests are ready to target Neo4j through the real adapter path
- Related Files:
  - `application/`
  - `infra-neo4j/`
  - `testkit/`
- Related Tests:
  - `:infra-neo4j:test`
  - `:boot-app:test`

### JAVA-003 Stage 2 Neo4j Persistence And Events

- Requirement: `REQ-003`
- Stage: 2
- Status: completed
- Dependencies: `JAVA-002`
- Inputs:
  - stable stage 1 aggregate and invariants
  - graph node and edge model
- Outputs:
  - Neo4j constraint catalog
  - migration baseline
  - persistence mappings
  - `DomainEvent` audit persistence path
- Acceptance:
  - baseline migration assets exist in the repo
  - persistence mappings compile cleanly
  - migration and persistence integration verification can run against a real Neo4j target
- Related Files:
  - `infra-neo4j/`
  - `docs/SCHEMA-GRAPH.md`
  - `docs/REQUIREMENT-BACKLOG.md`
- Related Tests:
  - `:infra-neo4j:test`

### JAVA-024 Stage 2 Requirement Backlog Governance

- Requirement: `REQ-001`
- Stage: 2
- Status: completed
- Dependencies: none
- Inputs:
  - current tracking docs
  - approved requirement governance plan
- Outputs:
  - `docs/REQUIREMENT-BACKLOG.md`
  - requirement links across status, task, and resume docs
  - requirement backlinks from contract and migration docs
- Acceptance:
  - all in-progress and planned tasks trace back to `REQ-*`
  - `PROGRAM-STATUS`, `TASK-BOARD`, and `REQUIREMENT-BACKLOG` do not conflict
- Related Files:
  - `docs/REQUIREMENT-BACKLOG.md`
  - `docs/PROGRAM-STATUS.md`
  - `docs/TASK-BOARD.md`
  - `docs/RESUME-RUNBOOK.md`
- Related Tests:
  - documentation consistency review

### JAVA-011 Remote Verification Node Enablement

- Requirement: `REQ-002`
- Stage: 2
- Status: completed
- Dependencies: none
- Inputs:
  - provided server `114.67.156.250`
  - local remote probe script
- Outputs:
  - remote Neo4j verification node or an explicit blocked-state record
- Acceptance:
  - remote account can run Docker workloads for Neo4j verification
  - Neo4j container can be started or the exact image/runtime blocker is documented with command evidence
- Related Files:
  - `ops/remote/`
  - `docs/REMOTE-VERIFICATION-NODE.md`
  - `docs/PARALLEL-EXECUTION-PLAN.md`
- Related Tests:
  - `py -3.12 ops/remote/probe_server.py`

### JAVA-015 Stage 4 CLI Delivery Slice

- Requirement: `REQ-020`
- Stage: 4
- Status: completed
- Dependencies: none
- Inputs:
  - `BeingService`
  - `LeaseService`
  - `ReviewService`
  - `interfaces-cli` Picocli baseline
- Outputs:
  - first functional CLI commands for currently implemented service flows
  - focused CLI tests
- Acceptance:
  - supported being, lease, and review flows can be invoked from Picocli commands
  - `:interfaces-cli:test` passes
- Related Files:
  - `interfaces-cli/`
  - `docs/API-CONTRACT.md`
- Related Tests:
  - `:interfaces-cli:test`

### JAVA-016 Stage 2 Boot Profile Activation

- Requirement: `REQ-002`
- Stage: 2
- Status: completed
- Dependencies: `JAVA-003`, `JAVA-010`
- Inputs:
  - `Neo4jBeingStore`
  - existing `boot-app` configuration
- Outputs:
  - profile-safe boot wiring for `memory` and `neo4j`
  - focused Spring tests for profile behavior
- Acceptance:
  - default `memory` boot path remains green
  - `neo4j` profile can resolve the real adapter when the infrastructure beans are present
- Related Files:
  - `boot-app/`
  - `docs/PROGRAM-STATUS.md`
- Related Tests:
  - `:boot-app:test`

### JAVA-017 Stage 4 REST Admin Slice

- Requirement: `REQ-020`
- Stage: 4
- Status: completed
- Dependencies: none
- Inputs:
  - current application service contracts
  - REST envelope design
- Outputs:
  - controllers and request DTOs for beings, sessions, leases, reviews, and canonical projection rebuilds
  - focused controller tests
- Acceptance:
  - `:interfaces-rest:test` passes
  - documented REST surface matches current implementation
- Related Files:
  - `interfaces-rest/src/main/java/com/openclaw/digitalbeings/interfaces/rest/`
  - `docs/API-CONTRACT.md`
- Related Tests:
  - `:interfaces-rest:test`

### JAVA-001 Stage 0 Bootstrap Baseline

- Stage: 0
- Status: completed
- Dependencies: none
- Inputs:
  - approved architecture and documentation protocol
  - target project root `C:\Users\16343\.openclaw\digital-beings-java`
- Outputs:
  - Gradle multi-module project
  - Spring Boot startup baseline
  - Neo4j docker-compose baseline
  - full-trace documentation set
- Acceptance:
  - `gradlew.bat build` succeeds
  - app startup baseline exists through `:boot-app:test`
  - `docs/` set is complete
  - `RESUME-RUNBOOK.md` is usable
- Related Files:
  - `build.gradle.kts`
  - `settings.gradle.kts`
  - `boot-app/`
  - `docs/`
- Related Tests:
  - `:domain-core:test`
  - `:boot-app:test`
  - root `build`

### JAVA-002 Stage 1 Domain Model And Invariants

- Stage: 1
- Status: completed
- Dependencies: `JAVA-001`
- Inputs:
  - stage 0 baseline
  - fixed node and edge model
- Outputs:
  - bounded context packages
  - domain entities and value objects
  - invariants and state transitions
- Acceptance:
  - domain unit tests cover core invariants
- Related Files:
  - `domain-core/`
  - `docs/SCHEMA-GRAPH.md`
  - `docs/DECISIONS.md`
- Related Tests:
  - `:domain-core:test`

### JAVA-012 Parallel Infra Neo4j Persistence Slice

- Stage: 2
- Status: completed
- Dependencies: `JAVA-002`
- Inputs:
  - stage 1 aggregate model
- Outputs:
  - SDN node entities
  - repository interfaces
  - aggregate mapper
- Acceptance:
  - `:infra-neo4j:test` passes
- Related Files:
  - `infra-neo4j/src/main/java/com/openclaw/digitalbeings/infrastructure/neo4j/persistence/`
- Related Tests:
  - `:infra-neo4j:test`

### JAVA-013 Parallel Application Service Contracts

- Stage: 3 prep
- Status: completed
- Dependencies: `JAVA-002`
- Inputs:
  - stage 1 aggregate model
- Outputs:
  - `BeingStore` port
  - in-memory store
  - service contracts and DTOs
- Acceptance:
  - `:application:test` passes
- Related Files:
  - `application/src/main/java/com/openclaw/digitalbeings/application/`
- Related Tests:
  - `:application:test`

### JAVA-014 Parallel Testkit Neo4j Foundation

- Stage: 2 support
- Status: completed
- Dependencies: `JAVA-001`
- Inputs:
  - Testcontainers baseline
- Outputs:
  - reusable Neo4j container factory
  - connection details value object
  - container session wrapper
- Acceptance:
  - `:testkit:test` passes
- Related Files:
  - `testkit/src/main/java/com/openclaw/digitalbeings/testkit/`
- Related Tests:
  - `:testkit:test`

## Archived / Superseded

### JAVA-004 Stage 3 Application Services

- Status: superseded
- Replaced By:
  - `JAVA-026`
  - `JAVA-027`
  - `JAVA-028`
- Reason:
  - requirement backlog governance split the generic stage 3 umbrella into requirement-aligned tasks

### JAVA-005 Stage 4 REST And CLI V1

- Status: superseded
- Replaced By:
  - `JAVA-029`
  - `JAVA-030`
  - `JAVA-031`
- Reason:
  - requirement backlog governance split the generic stage 4 umbrella into requirement-aligned interface tasks

### JAVA-019 Stage 3 Relationship And Host Contract Services

- Status: superseded
- Replaced By:
  - `JAVA-026`
- Reason:
  - the implementation plan split the stage 3 relationship work into a dedicated post-seam service slice

### JAVA-020 Stage 3 Owner Profile And Managed Agent Services

- Status: superseded
- Replaced By:
  - `JAVA-027`
- Reason:
  - the implementation plan split the stage 3 governance work into a dedicated post-seam service slice

### JAVA-021 Stage 3 Snapshot Continuity Services

- Status: superseded
- Replaced By:
  - `JAVA-028`
- Reason:
  - the implementation plan split the stage 3 snapshot work into a dedicated post-seam service slice

### JAVA-022 Stage 4 REST And CLI Resource Completion

- Status: superseded
- Replaced By:
  - `JAVA-029`
  - `JAVA-030`
- Reason:
  - the implementation plan split stage 4 resource delivery into two resource families for safer parallel work

### JAVA-023 Stage 4 Contract Unification

- Status: superseded
- Replaced By:
  - `JAVA-031`
- Reason:
  - the implementation plan reissued the contract-normalization work under the new stage 4 split

### JAVA-006 Stage 5 Legacy Importer

- Status: superseded
- Replaced By:
  - `JAVA-032`
  - `JAVA-033`
- Reason:
  - the implementation plan split importer work into dry-run/mapping and full replay/reporting phases

### JAVA-007 Stage 6 Governance Loop Hardening

- Status: superseded
- Replaced By:
  - `JAVA-034`
  - `JAVA-035`
- Reason:
  - the implementation plan split governance work into backend capability and jobs/reporting phases
