# Risks

## Active Risks

### RISK-001 Toolchain Auto-Provision Regression

- Impact: high
- Probability: low
- Signal:
  - Gradle can no longer resolve or reuse the auto-provisioned JDK 21 toolchain
- Mitigation:
  - keep wrapper and Foojay resolver configured
  - avoid reintroducing the stale `JAVA_HOME` precedence in wrapper scripts
- Trigger Action:
  - pin the failing step in `PROGRAM-STATUS.md` and fall back to installing a local JDK 21 manually

### RISK-002 Missing Docker On Dev Machine

- Impact: medium
- Probability: high on current machine
- Signal:
  - `docker` command is not available
- Mitigation:
  - keep compose assets ready
  - allow stage 0 build verification to proceed without falsely claiming local infra success
- Trigger Action:
  - mark container-based verification as blocked rather than passed

### RISK-003 Requirement And Documentation Drift

- Impact: high
- Probability: medium
- Signal:
  - requirement backlog, task board, or implementation docs disagree about current scope or completion state
- Mitigation:
  - requirement backlog is the upstream source of truth
  - mandatory end-of-block handoff updates
  - active tasks must always carry a `REQ-*` parent
- Trigger Action:
  - treat the work block as incomplete until `REQUIREMENT-BACKLOG`, `TASK-BOARD`, and the affected docs are reconciled

### RISK-004 Parallel Gradle Verification Collision On Windows

- Impact: medium
- Probability: medium
- Signal:
  - Gradle reports failure to delete `build/test-results` files while overlapping tasks are running
- Mitigation:
  - do not run overlapping Gradle `build` and `test` tasks against the same module outputs in parallel
- Trigger Action:
  - rerun verification serially and record that the failure was environmental rather than a product defect

### RISK-005 Remote Verification Node Drift

- Impact: medium
- Probability: medium
- Signal:
  - the remote host is reachable but the `digital-beings-neo4j-dev` container is missing, stale, or no longer reachable on the expected ports
- Mitigation:
  - keep the remote probe script and status doc current
  - keep the remote bring-up scripts flexible about image names and tags
  - use `start_neo4j.py` as the standard recovery path
- Trigger Action:
  - rerun `py -3.12 ops/remote/start_neo4j.py`, confirm the endpoint with `probe_server.py`, and update `REMOTE-VERIFICATION-NODE.md` if the host characteristics changed

### RISK-006 Stage 4 Shared Interface Hotspots

- Impact: medium
- Probability: medium
- Signal:
  - concurrent edits begin to overlap in `interfaces-rest` shared envelope or handler files, `DigitalBeingsCli.java`, or `docs/API-CONTRACT.md`
- Mitigation:
  - split stage 4 by resource family first
  - keep `JAVA-031` serialized behind `JAVA-029` and `JAVA-030`
  - leave final contract and doc normalization to the main thread
- Trigger Action:
  - stop parallel interface edits, merge the current family-specific work, and reopen only one thread on the shared contract files

### RISK-007 Phase 3 Importer And Governance Contract Drift

- Impact: high
- Probability: medium
- Signal:
  - `legacy-importer` dry-run output shape changes without matching updates in `docs/MIGRATION-LEDGER.md` or `application` governance backend assumptions
- Mitigation:
  - keep `JAVA-032` confined to `legacy-importer/`
  - keep `JAVA-034` confined to `application/`
  - freeze the dry-run contract before opening `JAVA-033`
- Trigger Action:
  - stop the governance lane from binding to importer payload details, document the contract gap in `PROGRAM-STATUS.md`, and reconcile `TASK-BOARD.md` plus `MIGRATION-LEDGER.md` before continuing
