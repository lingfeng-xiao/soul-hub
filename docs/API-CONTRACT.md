# API Contract

## Envelope

All REST write and read responses must converge on the following shape:

```json
{
  "requestId": "01HX...",
  "timestamp": "2026-03-21T07:00:00Z",
  "success": true,
  "data": {},
  "error": null
}
```

## Bootstrap Endpoint

### `GET /internal/platform/status`

- Status: implemented for bootstrap only
- Purpose:
  - expose current phase and next recommended action while the rest of the platform is still under construction
- Response Data:
  - `currentPhase`
  - `currentFocus`
  - `statusDocument`
  - `nextRecommendedAction`

## Implemented REST Endpoints

### `POST /beings`

- Status: implemented
- Purpose:
  - create a new being
- Request Data:
  - `displayName`
  - `actor`

### `GET /beings/{beingId}`

- Status: implemented
- Purpose:
  - fetch a single being view by id

### `GET /beings`

- Status: implemented
- Purpose:
  - list the current being views

### `POST /sessions`

- Status: implemented
- Purpose:
  - register a runtime session for a being
- Request Data:
  - `beingId`
  - `hostType`
  - `actor`

### `POST /leases`

- Status: implemented
- Purpose:
  - acquire an authority lease for an existing runtime session
- Request Data:
  - `beingId`
  - `sessionId`
  - `actor`

### `POST /leases/{leaseId}/release`

- Status: implemented
- Purpose:
  - release an authority lease
- Request Data:
  - `beingId`
  - `actor`

### `GET /beings/{beingId}/status`

- Status: implemented
- Purpose:
  - fetch the latest being view through the lease/status workflow

### `POST /reviews`

- Status: implemented
- Purpose:
  - draft a review item
- Request Data:
  - `beingId`
  - `lane`
  - `kind`
  - `proposal`
  - `actor`

### `POST /reviews/{reviewItemId}/submit`

- Status: implemented
- Purpose:
  - submit a draft review item
- Request Data:
  - `beingId`
  - `actor`

### `POST /reviews/{reviewItemId}/decision`

- Status: implemented
- Purpose:
  - accept, reject, or defer a submitted review item
- Request Data:
  - `beingId`
  - `decision`
  - `actor`

### `POST /canonical-projections/rebuild`

- Status: implemented
- Purpose:
  - rebuild the canonical projection for a being from accepted reviews
- Request Data:
  - `beingId`
  - `actor`

### `GET /canonical-projections/{beingId}`

- Status: implemented
- Purpose:
  - fetch the latest canonical projection for a being

### `POST /relationships`

- Status: implemented
- Purpose:
  - create a relationship entity for a being
- Request Data:
  - `beingId`
  - `kind`
  - `displayName`
  - `actor`

### `GET /relationships/{beingId}`

- Status: implemented
- Purpose:
  - list relationship entities for a being

### `POST /host-contracts`

- Status: implemented
- Purpose:
  - register a host contract for a being
- Request Data:
  - `beingId`
  - `hostType`
  - `actor`

### `GET /host-contracts/{beingId}`

- Status: implemented
- Purpose:
  - list host contracts for a being

### `POST /snapshots`

- Status: implemented
- Purpose:
  - create a continuity snapshot for a being
- Request Data:
  - `beingId`
  - `type`
  - `summary`
  - `actor`

### `GET /snapshots/{beingId}`

- Status: implemented
- Purpose:
  - list continuity snapshots for a being

### `POST /owner-profile-facts`

- Status: implemented
- Purpose:
  - record an accepted owner profile fact for a being
- Request Data:
  - `beingId`
  - `section`
  - `key`
  - `summary`
  - `actor`

### `GET /owner-profile-facts/{beingId}`

- Status: implemented
- Purpose:
  - list owner profile facts for a being

### `POST /managed-agent-specs`

- Status: implemented
- Purpose:
  - register a managed agent specification for a being
- Request Data:
  - `beingId`
  - `role`
  - `status`
  - `actor`

### `GET /managed-agent-specs/{beingId}`

- Status: implemented
- Purpose:
  - list managed agent specifications for a being

## Implemented CLI Commands

- `being create`
- `being get`
- `being list`
- `lease register-session`
- `lease acquire`
- `lease release`
- `review draft`
- `review submit`
- `review decide`
- `projection read`
- `projection rebuild`
- `relationship create`
- `relationship list`
- `host-contract create`
- `host-contract list`
- `snapshot create`
- `snapshot list`
- `owner-profile record`
- `owner-profile list`
- `managed-agent register`
- `managed-agent list`

## Error Contract

- All REST success responses use the shared `RequestEnvelope` with generated `requestId`, `timestamp`, `success`, `data`, and `error`.
- All REST validation failures use the shared `RequestEnvelope` with `success=false`, `data=null`, and an `error` object carrying `code` and `message`.
- Error-code routing is currently normalized by request family:
  - `/beings` -> `IDENTITY_VALIDATION`
  - `/sessions` and `/leases` -> `LEASE_VALIDATION`
  - `/reviews` and `/canonical-projections` -> `REVIEW_VALIDATION`
  - `/snapshots` -> `SNAPSHOT_VALIDATION`
  - `/relationships`, `/host-contracts`, `/owner-profile-facts`, and `/managed-agent-specs` -> `GRAPH_VALIDATION`

## Implemented Error Code Families

- `IDENTITY_*`
- `LEASE_*`
- `REVIEW_*`
- `SNAPSHOT_*`
- `GRAPH_*`
- `IMPORT_*`

## CLI Output Contract

- Default CLI output mode is `table`.
- All CLI commands also support `--output json`.
- Resource list commands return a JSON array in `json` mode.
- Resource create/get/read commands return a JSON object in `json` mode.
- `projection read` returns `{"beingId":"...","canonicalProjection":"absent"}` when no projection exists and `--output json` is selected.

## Requirement Links

- `REQ-020` owns completion of the missing V1 resources and REST/CLI coverage parity.
- `REQ-021` owns the final normalization of response envelope rules, error-code families, and CLI output contract behavior.
