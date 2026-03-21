# Remote Verification Node

## Purpose

This document records the status of the provided remote server intended to compensate for the lack of local Docker support.

## Host Summary

- Host: `114.67.156.250`
- User: `lingfeng`
- Role: remote Neo4j verification node candidate
- Credential Handling:
  - the host credential cache is stored locally in `.local/remote-verification.env`
  - `.local/` is ignored by Git and must never be committed

## Verified Findings

- Reachable over SSH from the current workstation
- Operating system:
  - Ubuntu Linux kernel `6.8.0-53-generic`
- Docker binary:
  - present
- Docker group:
  - `lingfeng` is now a member of `docker`
- `docker compose` plugin:
  - available
- Java:
  - OpenJDK 21 available
- Account permissions:
  - password-backed `sudo -S` works
- Image provisioning:
  - mirror fallback is supported by `start_neo4j.py`
  - a usable Neo4j image is now present on the host
- Current remote container state:
  - `digital-beings-neo4j-dev` is running
  - Bolt endpoint: `bolt://114.67.156.250:17687`
  - HTTP endpoint: `http://114.67.156.250:17474`
  - last verified on 2026-03-21 via `start_neo4j.py`, `probe_server.py`, `run_neo4j_smoke.py`, and the remote `infra-neo4j` test suite

## Current Conclusion

The server is now an active Neo4j verification node for this project. Local Docker is still unavailable on this workstation, so this remote node is the authoritative container-backed verification target.

## Unblock Conditions

Any one of the following is sufficient:

- rerun `py -3.12 ops/remote/start_neo4j.py` if the container disappears or the host is recreated
- use `py -3.12 ops/remote/stop_neo4j.py` to tear the container down cleanly
- update `.local/remote-verification.env` if the host, ports, or credentials change

## Revalidation Command

```powershell
py -3.12 ops/remote/probe_server.py
py -3.12 ops/remote/start_neo4j.py
py -3.12 ops/remote/run_neo4j_smoke.py
```
