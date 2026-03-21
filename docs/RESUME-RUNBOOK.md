# Resume Runbook

## Purpose

This file is the required re-entry point for any AI or human resuming work after an interruption.

## Resume Order

1. Read `docs/PROGRAM-STATUS.md`
2. Read `docs/REQUIREMENT-BACKLOG.md`
3. Read `docs/HANDOFF-CHECKLIST.md`
4. Read the active task in `docs/TASK-BOARD.md`
5. Read the latest entry in `docs/ITERATION-LOG.md`
6. Run the standard verification commands
7. Continue the single next recommended action from `docs/PROGRAM-STATUS.md`
8. If remote verification is relevant, read `docs/REMOTE-VERIFICATION-NODE.md` and run `py -3.12 ops/remote/probe_server.py`

## Standard Verification Commands

```powershell
./gradlew.bat build
./gradlew.bat :boot-app:test
./gradlew.bat :application:test
./gradlew.bat :testkit:test
./gradlew.bat :interfaces-rest:test
./gradlew.bat :interfaces-cli:test
py -3.12 ops/remote/probe_server.py
```

Run the Gradle commands serially, not in parallel, on this Windows machine.

## How To Identify Current Work

- `PROGRAM-STATUS.md` says which stage and task are active
- `REQUIREMENT-BACKLOG.md` says which long-lived requirement currently owns the work
- `TASK-BOARD.md` says what done means
- `ITERATION-LOG.md` says what actually happened most recently
- `TEST-STATUS.md` says whether the current baseline is verified

## How To Restore Local Services

- if Docker is available, start Neo4j with `docker compose up -d`
- if Docker is unavailable, do not fake green infrastructure; record the gap in `PROGRAM-STATUS.md` and `TEST-STATUS.md`
- if remote verification is needed, use `ops/remote/probe_server.py` and the local-only `.local/remote-verification.env` credential cache
- if the remote host is being used for Neo4j verification, check `docs/REMOTE-VERIFICATION-NODE.md` first and then use `ops/remote/start_neo4j.py` or `ops/remote/stop_neo4j.py`
- when validating the real persistence path, prefer the live remote Neo4j node over inventing local container success on this workstation

## How To Continue Safely

- create or update a requirement before changing implementation scope
- create or update a task only after the requirement entry exists and is actionable
- write an ADR before any non-trivial architecture change
- update the relevant contract docs before landing schema or API changes
- finish every work block by updating status, logs, and the handoff checklist

## Forbidden Actions

- do not rely on chat memory as the source of truth
- do not leave blockers or assumptions only in code comments
- do not start the next task while the handoff checklist is stale
- do not claim infrastructure verification if the machine lacks the required tools
- do not run overlapping Gradle build and test tasks against the same module outputs in parallel
- do not commit `.local/` or copy its contents into repo docs
