# Parallel Execution Plan

## Purpose

- This document is the execution scheduler for unfinished `REQ-*` backlog items.
- It exists to answer two questions before any new concurrent work starts:
  - which requirements can be executed in parallel without file or architecture conflicts
  - which requirements must remain serialized because they share aggregates, adapters, or interface contracts
- `TASK-BOARD.md` owns executable tasks.
- `REQUIREMENT-BACKLOG.md` owns scope and priority.
- This file owns wave planning, write-set separation, and merge order.

## Scheduling Rules

- A workstream is parallel-safe only if it has a disjoint primary write set and does not depend on in-flight contract changes from another workstream.
- Shared ownership of `domain-core/src/main/java/com/openclaw/digitalbeings/domain/being/Being.java`, `application/src/main/java/com/openclaw/digitalbeings/application/support/BeingStore.java`, or `infra-neo4j/src/main/java/com/openclaw/digitalbeings/infrastructure/neo4j/persistence/mapper/BeingNodeMapper.java` is treated as a merge hotspot and must be serialized unless a task is explicitly decomposed first.
- The local workstation currently has limited free memory, so only one heavy local Gradle verification lane may run at a time even if multiple coding threads are open.
- Docs that define current execution state may be updated only on the main thread:
  - `docs/PROGRAM-STATUS.md`
  - `docs/TASK-BOARD.md`
  - `docs/ITERATION-LOG.md`
  - `docs/TEST-STATUS.md`
  - `docs/HANDOFF-CHECKLIST.md`
- Worker threads may update code and focused requirement-owned docs, but the main thread remains responsible for final status synchronization.

## Completed Waves

### Wave 0 Foundation And Access

- Status:
  - completed
- Delivered:
  - stage 0 bootstrap baseline
  - stage 1 domain model and invariants
  - first Neo4j persistence slice
  - application service contracts
  - Testcontainers foundation
  - remote verification node
  - boot `neo4j` profile activation
  - first REST and CLI delivery slices
- Closed Tasks:
  - `JAVA-001`
  - `JAVA-002`
  - `JAVA-011`
  - `JAVA-012`
  - `JAVA-013`
  - `JAVA-014`
  - `JAVA-015`
  - `JAVA-016`
  - `JAVA-017`
  - `JAVA-024`

## Active Wave

### Wave 1 Stage 2 Closeout

- Status:
  - completed on 2026-03-21
- Goal:
  - close `REQ-002` and `REQ-003` with real persistence-backed verification before stage 3 domain expansion begins
- Why This Wave Is Parallel-Safe:
  - `REQ-002` is primarily boot/runtime/remote verification work
  - `REQ-003` is primarily infra-neo4j/testkit migration and integration verification work
  - the write sets can be kept separate if ownership is enforced
- Workstreams:

#### Stream A Remote Persistence Smoke

- Requirement:
  - `REQ-002`
- Tasks:
  - `JAVA-018`
- Primary Write Set:
  - `boot-app/**`
  - `ops/remote/**`
  - focused runtime smoke scripts and tests
- Must Not Edit:
  - `infra-neo4j/**`
  - `application/**`
  - shared status docs
- Deliverable:
  - reproducible `neo4j` profile smoke flow against `bolt://114.67.156.250:17687`

#### Stream B Neo4j Migration And Integration Verification

- Requirement:
  - `REQ-003`
- Tasks:
  - `JAVA-003`
  - `JAVA-010`
- Primary Write Set:
  - `infra-neo4j/**`
  - `testkit/**`
  - focused adapter integration tests
- Must Not Edit:
  - `boot-app/**`
  - `ops/remote/**`
  - shared status docs
- Deliverable:
  - migration execution and adapter verification that can target a real Neo4j instance

- Merge Gate:
  - both streams must leave `memory` profile behavior unchanged
  - final integration and status-doc updates happen on the main thread after both streams land

## Planned Waves

### Wave 2 Stage 3 Service Expansion

- Status:
  - completed on 2026-03-21
- Requirements:
  - `REQ-013`
  - `REQ-010`
  - `REQ-011`
  - `REQ-012`
- Current Parallelism Decision:
  - completed through a serialized seam task followed by disjoint application slices
- Result:
  - `JAVA-025` landed the shared aggregate seam
  - `JAVA-026` and `JAVA-027` ran safely in parallel on disjoint application write sets
  - `JAVA-028` followed without reopening the shared mapper hotspot
- Acceptance Evidence:
  - `./gradlew.bat :domain-core:test :application:test :infra-neo4j:test`
  - `./gradlew.bat build`
  - `py -3.12 ops/remote/run_neo4j_smoke.py`

### Wave 3 Stage 4 Interface Completion

- Status:
  - completed on 2026-03-21
- Requirements:
  - `REQ-020`
  - `REQ-021`
- Current Parallelism Decision:
  - completed through two parallel resource-family lanes and one serialized contract merge lane
- Notes:
  - `JAVA-029` and `JAVA-030` landed in parallel after stage 3 services stabilized
  - `JAVA-031` then merged the shared envelope, error handling, and CLI output conventions without reopening stage 3 hotspots

### Wave 4 Stage 5 To Stage 6 Platform Expansion

- Status:
  - active
- Requirements:
  - `REQ-030`
  - `REQ-040`
- Current Parallelism Decision:
  - moderate parallelism possible
- Notes:
  - `JAVA-032` and `JAVA-034` completed in parallel because the importer dry-run core stayed in `legacy-importer/` while the governance backend slice stayed in `application/`
  - `JAVA-033` is now the lead lane
  - `JAVA-035` remains a follow-up lane until the governance surface and importer reporting contract stabilize

### Wave 5 Stage 7 To Stage 8 Runtime And Production Hardening

- Status:
  - blocked by Wave 4 completion
- Requirements:
  - `REQ-050`
  - `REQ-060`
- Current Parallelism Decision:
  - defer detailed threading design until host event contracts and recovery constraints are concrete

## Current Thread Allocation

- Main Thread:
  - owns orchestration, shared documentation, final merges, and acceptance tracking
- Local Coding Thread A:
  - reserved for `JAVA-033`
- Local Coding Thread B:
  - reserved for `JAVA-035`
- Remote Verification Thread:
  - reserved for `./gradlew.bat build`, `py -3.12 ops/remote/run_neo4j_smoke.py`, and later long importer/governance replays
- Additional Threads:
  - not recommended until local free memory improves or stage 3 is decomposed into smaller write-scoped tasks

## Next Merge Point

- Keep `JAVA-033` and `JAVA-035` moving under `REQ-030` and `REQ-040`
- After replay/reporting and jobs stabilize, reopen the next merge point
- Update:
  - `docs/PROGRAM-STATUS.md`
  - `docs/TASK-BOARD.md`
  - `docs/TEST-STATUS.md`
  - `docs/ITERATION-LOG.md`
- Then reopen `JAVA-035` once the governance backend surface is stable
