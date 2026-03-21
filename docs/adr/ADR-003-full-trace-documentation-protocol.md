# ADR-003 Mandatory Full-Trace Documentation Protocol

## Background

The project must survive AI interruptions without relying on thread memory or human reconstruction.

## Decision

Adopt a mandatory repository-local documentation protocol. Every work block must update status, task board, iteration log, test status, and handoff checklist before stopping.

## Impact

- makes interruption recovery deterministic
- lowers onboarding cost for any future AI or human contributor
- turns documentation drift into a process failure, not a nice-to-have

## Rejected Alternatives

- lightweight notes only
- conversation history as the main source of truth
- update docs only at milestone boundaries

## Rollback Conditions

Do not roll back unless a stronger machine-readable execution tracking system fully replaces the same guarantees.
