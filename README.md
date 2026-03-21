# Digital Beings Java

`digital-beings-java` is the new engineering-grade Java rewrite of the Digital Beings platform.

## Goals

- replace the current Python script mesh with a structured multi-module Java codebase
- treat Neo4j as the authoritative state store
- make every work block resumable by any AI through repo-local tracking docs
- keep legacy Python assets as a reference and one-time import source only

## Stage 0 Scope

- Gradle multi-module project with wrapper
- Spring Boot application baseline
- JDK 21 toolchain auto-provisioning
- Neo4j docker-compose baseline
- Testcontainers baseline module
- mandatory execution-trace documentation system under `docs/`

## Quick Start

1. Run `gradlew.bat build`
2. When Docker is available, run `docker compose up -d`
3. Start the app with `gradlew.bat :boot-app:bootRun --args="--spring.profiles.active=local"`
4. Read `docs/RESUME-RUNBOOK.md` before continuing implementation work

## Modules

- `boot-app`: Spring Boot entrypoint and runtime assembly
- `domain-core`: shared domain model and invariants
- `application`: application services and use case orchestration
- `infra-neo4j`: Neo4j persistence and schema support
- `interfaces-rest`: REST-facing controllers and DTOs
- `interfaces-cli`: CLI-facing integration points
- `jobs`: scheduled work and operational jobs
- `legacy-importer`: one-time import from the Python repository
- `testkit`: reusable testing helpers, especially Neo4j/Testcontainers
