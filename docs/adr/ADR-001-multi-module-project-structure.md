# ADR-001 Multi-Module Spring Boot Project Structure

## Background

The rewrite needs long-lived bounded contexts, independent testing seams, and a single deployable runtime.

## Decision

Use a Gradle multi-module build with one deployable `boot-app` and dedicated modules for domain, application, infrastructure, interfaces, jobs, importer, and test support.

## Impact

- keeps boundaries visible from day one
- allows future host adapters or persistence variants without destabilizing core modules
- makes stage-based execution easier to track in docs and builds

## Rejected Alternatives

- single flat Spring Boot module: too easy to collapse boundaries
- Maven bootstrap: acceptable, but Gradle wrapper plus toolchains is better for this machine state

## Rollback Conditions

Revisit only if the module graph becomes too fragmented for delivery speed or the build becomes materially harder to maintain than a smaller module set.
