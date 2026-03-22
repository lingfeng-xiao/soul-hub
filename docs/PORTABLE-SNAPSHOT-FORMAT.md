# Portable Snapshot Format

> **Version**: 1.0
> **Status**: Implemented (JAVA-C2a)
> **Schema ID**: `urn:openclaw:digital-beings:portable-snapshot:v1.0`

A Portable Snapshot is a self-contained JSON serialization of a complete `Being` aggregate. It is used to migrate Beings between Neo4j instances, backup full runtime state, and support the cutover from the Python runtime to the Java runtime.

---

## Overview

A portable snapshot captures all domain entities that form the Being aggregate:

| Entity | Purpose |
|---|---|
| `being` | Core identity: beingId, displayName, continuityEpoch, createdAt, revision |
| `identityFacets` | Identity facets (KINS, SOUL, MASK, etc.) |
| `relationships` | Relationship entities (PARENT_OF, WORKS_WITH, etc.) |
| `sessions` | Runtime sessions (host type, start/end timestamps) |
| `leases` | Authority leases (status, actor, timestamps) |
| `reviewItems` | Review items (lane, kind, proposal, status) |
| `ownerProfileFacts` | Owner profile facts (section, key, summary) |
| `managedAgentSpecs` | Managed agent specs (role, status) |
| `canonicalProjection` | Current canonical projection (version, accepted items) |
| `domainEvents` | Domain event log (used for continuity audit) |

---

## JSON Schema (RFC 8259)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "urn:openclaw:digital-beings:portable-snapshot:v1.0",
  "title": "PortableSnapshot",
  "description": "Self-contained serialization of a Being aggregate for migration and backup.",
  "version": "1.0",
  "type": "object",
  "required": ["version", "being", "exportedAt"],
  "properties": {
    "version": {
      "type": "string",
      "const": "1.0",
      "description": "Format version. Must be '1.0'."
    },
    "being": { "$ref": "#/definitions/BeingSnapshot" },
    "identityFacets": {
      "type": "array",
      "items": { "$ref": "#/definitions/IdentityFacetSnapshot" }
    },
    "relationships": {
      "type": "array",
      "items": { "$ref": "#/definitions/RelationshipSnapshot" }
    },
    "sessions": {
      "type": "array",
      "items": { "$ref": "#/definitions/SessionSnapshot" }
    },
    "leases": {
      "type": "array",
      "items": { "$ref": "#/definitions/LeaseSnapshot" }
    },
    "reviewItems": {
      "type": "array",
      "items": { "$ref": "#/definitions/ReviewItemSnapshot" }
    },
    "ownerProfileFacts": {
      "type": "array",
      "items": { "$ref": "#/definitions/OwnerProfileSnapshot" }
    },
    "managedAgentSpecs": {
      "type": "array",
      "items": { "$ref": "#/definitions/ManagedAgentSpecSnapshot" }
    },
    "canonicalProjection": {
      "$ref": "#/definitions/CanonicalProjectionSnapshot"
    },
    "domainEvents": {
      "type": "array",
      "items": { "$ref": "#/definitions/DomainEventSnapshot" }
    },
    "exportedAt": {
      "type": "string",
      "format": "date-time",
      "description": "ISO-8601 UTC timestamp of export."
    }
  },
  "definitions": {
    "BeingSnapshot": {
      "type": "object",
      "required": ["beingId", "displayName", "continuityEpoch", "createdAt", "revision"],
      "properties": {
        "beingId": {
          "type": "string",
          "pattern": "^[0-9A-Z]{24,26}$",
          "description": "ULID-based being identifier."
        },
        "displayName": {
          "type": "string",
          "minLength": 1,
          "maxLength": 255,
          "description": "Human-readable display name."
        },
        "continuityEpoch": {
          "type": "integer",
          "minimum": 1,
          "description": "Monotonically increasing epoch counter."
        },
        "createdAt": {
          "type": "string",
          "format": "date-time",
          "description": "Original creation timestamp."
        },
        "revision": {
          "type": "integer",
          "minimum": 0,
          "description": "Optimistic-lock revision counter."
        }
      }
    },

    "IdentityFacetSnapshot": {
      "type": "object",
      "required": ["facetId", "kind", "summary", "createdAt"],
      "properties": {
        "facetId": {
          "type": "string",
          "pattern": "^[0-9A-Z]{24,26}$",
          "description": "ULID of this identity facet."
        },
        "kind": {
          "type": "string",
          "enum": ["KINS", "SOUL", "MASK", "SHADOW", "MASKED", "PERSONA", "ARCHETYPE"],
          "description": "Facet kind."
        },
        "summary": {
          "type": "string",
          "maxLength": 500,
          "description": "Human-readable summary of this facet."
        },
        "createdAt": {
          "type": "string",
          "format": "date-time",
          "description": "When this facet was recorded."
        }
      }
    },

    "RelationshipSnapshot": {
      "type": "object",
      "required": ["entityId", "kind", "displayName", "createdAt"],
      "properties": {
        "entityId": {
          "type": "string",
          "pattern": "^[0-9A-Z]{24,26}$",
          "description": "ULID of the related entity."
        },
        "kind": {
          "type": "string",
          "enum": ["PARENT_OF", "CHILD_OF", "WORKS_WITH", "REPORTS_TO", "MENTIONS", "OWNS"],
          "description": "Relationship kind."
        },
        "displayName": {
          "type": "string",
          "maxLength": 255,
          "description": "Human-readable name of the related entity."
        },
        "createdAt": {
          "type": "string",
          "format": "date-time",
          "description": "When this relationship was established."
        }
      }
    },

    "SessionSnapshot": {
      "type": "object",
      "required": ["sessionId", "hostType", "startedAt"],
      "properties": {
        "sessionId": {
          "type": "string",
          "pattern": "^[0-9A-Z]{24,26}$",
          "description": "ULID of the runtime session."
        },
        "hostType": {
          "type": "string",
          "enum": ["OPENCLAW", "CODEX", "REMOTE"],
          "description": "Host adapter type."
        },
        "startedAt": {
          "type": "string",
          "format": "date-time",
          "description": "Session start timestamp."
        },
        "endedAt": {
          "type": ["string", "null"],
          "format": "date-time",
          "description": "Session end timestamp. Null if session is active."
        }
      }
    },

    "LeaseSnapshot": {
      "type": "object",
      "required": ["leaseId", "sessionId", "status", "requestedAt", "lastActor"],
      "properties": {
        "leaseId": {
          "type": "string",
          "pattern": "^[0-9A-Z]{24,26}$",
          "description": "ULID of this authority lease."
        },
        "sessionId": {
          "type": "string",
          "pattern": "^[0-9A-Z]{24,26}$",
          "description": "ULID of the session that holds this lease."
        },
        "status": {
          "type": "string",
          "enum": ["PENDING", "ACTIVE", "RELEASED", "EXPIRED", "REVOKED"],
          "description": "Lease status. Active leases are set to RELEASED on import."
        },
        "requestedAt": {
          "type": "string",
          "format": "date-time",
          "description": "When the lease was requested."
        },
        "grantedAt": {
          "type": ["string", "null"],
          "format": "date-time",
          "description": "When the lease was granted."
        },
        "releasedAt": {
          "type": ["string", "null"],
          "format": "date-time",
          "description": "When the lease was released."
        },
        "lastActor": {
          "type": "string",
          "minLength": 1,
          "description": "Actor who last acted on this lease."
        }
      }
    },

    "ReviewItemSnapshot": {
      "type": "object",
      "required": ["reviewItemId", "lane", "kind", "proposal", "status", "createdAt", "actor"],
      "properties": {
        "reviewItemId": {
          "type": "string",
          "pattern": "^[0-9A-Z]{24,26}$",
          "description": "ULID of this review item."
        },
        "lane": {
          "type": "string",
          "enum": ["GOVERNANCE", "EVOLUTION", "REVIEW"],
          "description": "Review lane."
        },
        "kind": {
          "type": "string",
          "enum": ["EVOLUTION_SIGNAL", "GOVERNANCE_RULE", "PROFILE_UPDATE", "RELATIONSHIP_CHANGE"],
          "description": "Review item kind."
        },
        "proposal": {
          "type": "string",
          "maxLength": 2000,
          "description": "Proposal text."
        },
        "status": {
          "type": "string",
          "enum": ["DRAFT", "SUBMITTED", "ACCEPTED", "REJECTED", "DEFERRED", "CANCELLED"],
          "description": "Review item status."
        },
        "createdAt": {
          "type": "string",
          "format": "date-time",
          "description": "Creation timestamp."
        },
        "submittedAt": {
          "type": ["string", "null"],
          "format": "date-time",
          "description": "Submission timestamp. Null if not submitted."
        },
        "decidedAt": {
          "type": ["string", "null"],
          "format": "date-time",
          "description": "Decision timestamp. Null if not decided."
        },
        "actor": {
          "type": "string",
          "minLength": 1,
          "description": "Last actor who modified this item."
        }
      }
    },

    "OwnerProfileSnapshot": {
      "type": "object",
      "required": ["factId", "section", "key", "summary", "recordedAt"],
      "properties": {
        "factId": {
          "type": "string",
          "pattern": "^[0-9A-Z]{24,26}$",
          "description": "ULID of this owner profile fact."
        },
        "section": {
          "type": "string",
          "enum": ["IDENTITY", "GOVERNANCE", "PREFERENCES", "NOTES"],
          "description": "Profile section."
        },
        "key": {
          "type": "string",
          "minLength": 1,
          "maxLength": 255,
          "description": "Fact key within the section."
        },
        "summary": {
          "type": "string",
          "maxLength": 1000,
          "description": "Human-readable summary."
        },
        "recordedAt": {
          "type": "string",
          "format": "date-time",
          "description": "When this fact was recorded."
        }
      }
    },

    "ManagedAgentSpecSnapshot": {
      "type": "object",
      "required": ["specId", "role", "status", "registeredAt"],
      "properties": {
        "specId": {
          "type": "string",
          "pattern": "^[0-9A-Z]{24,26}$",
          "description": "ULID of this managed agent spec."
        },
        "role": {
          "type": "string",
          "minLength": 1,
          "maxLength": 100,
          "description": "Agent role (e.g. 'product-manager', 'code-reviewer')."
        },
        "status": {
          "type": "string",
          "enum": ["ACTIVE", "SUSPENDED", "RETIRED"],
          "description": "Agent spec status."
        },
        "registeredAt": {
          "type": "string",
          "format": "date-time",
          "description": "Registration timestamp."
        }
      }
    },

    "CanonicalProjectionSnapshot": {
      "type": "object",
      "required": ["projectionId", "version", "generatedAt", "acceptedReviewItemIds", "contentSummary"],
      "properties": {
        "projectionId": {
          "type": "string",
          "pattern": "^[0-9A-Z]{24,26}$",
          "description": "ULID of the canonical projection."
        },
        "version": {
          "type": "integer",
          "minimum": 0,
          "description": "Projection version number."
        },
        "generatedAt": {
          "type": "string",
          "format": "date-time",
          "description": "When this projection was generated."
        },
        "acceptedReviewItemIds": {
          "type": "array",
          "items": { "type": "string" },
          "description": "List of accepted review item ULIDs."
        },
        "contentSummary": {
          "type": "string",
          "maxLength": 2000,
          "description": "Human-readable content summary."
        }
      }
    },

    "DomainEventSnapshot": {
      "type": "object",
      "required": ["eventId", "eventType", "actor", "occurredAt", "summary"],
      "properties": {
        "eventId": {
          "type": "string",
          "pattern": "^[0-9A-Z]{24,26}$",
          "description": "ULID of this domain event."
        },
        "eventType": {
          "type": "string",
          "enum": [
            "BEING_CREATED", "BEING_IMPORTED", "INJECTION_CONTEXT_UPDATED",
            "IDENTITY_FACET_ADDED", "IDENTITY_FACET_UPDATED", "IDENTITY_FACET_REMOVED",
            "RELATIONSHIP_ADDED", "RELATIONSHIP_REMOVED",
            "SESSION_STARTED", "SESSION_ENDED",
            "LEASE_REQUESTED", "LEASE_GRANTED", "LEASE_RELEASED", "LEASE_EXPIRED", "LEASE_REVOKED",
            "EVOLUTION_SIGNAL_SUBMITTED", "EVOLUTION_SIGNAL_ACCEPTED", "EVOLUTION_SIGNAL_REJECTED",
            "GOVERNANCE_RULE_SUBMITTED", "GOVERNANCE_RULE_ACCEPTED", "GOVERNANCE_RULE_REJECTED",
            "CANONICAL_PROJECTION_REBUILT",
            "OWNER_PROFILE_FACT_RECORDED",
            "MANAGED_AGENT_REGISTERED", "MANAGED_AGENT_SUSPENDED", "MANAGED_AGENT_RETIRED"
          ],
          "description": "Domain event type."
        },
        "actor": {
          "type": "string",
          "minLength": 1,
          "description": "Actor who triggered this event."
        },
        "occurredAt": {
          "type": "string",
          "format": "date-time",
          "description": "When this event occurred."
        },
        "summary": {
          "type": "string",
          "maxLength": 500,
          "description": "Human-readable event summary."
        }
      }
    }
  }
}
```

---

## Example

```json
{
  "version": "1.0",
  "being": {
    "beingId": "01HZX0000000000000000000000",
    "displayName": "Guan Guan",
    "continuityEpoch": 1,
    "createdAt": "2026-01-01T00:00:00Z",
    "revision": 42
  },
  "identityFacets": [
    {
      "facetId": "01HZX0000000000000000000001",
      "kind": "SOUL",
      "summary": "Primary identity core",
      "createdAt": "2026-01-01T00:00:00Z"
    }
  ],
  "relationships": [
    {
      "entityId": "01HZX0000000000000000000002",
      "kind": "PARENT_OF",
      "displayName": "Xiao Yi",
      "createdAt": "2026-01-15T00:00:00Z"
    }
  ],
  "sessions": [
    {
      "sessionId": "01HZX0000000000000000000003",
      "hostType": "CODEX",
      "startedAt": "2026-03-01T00:00:00Z",
      "endedAt": null
    }
  ],
  "leases": [
    {
      "leaseId": "01HZX0000000000000000000004",
      "sessionId": "01HZX0000000000000000000003",
      "status": "ACTIVE",
      "requestedAt": "2026-03-01T00:00:00Z",
      "grantedAt": "2026-03-01T00:00:01Z",
      "releasedAt": null,
      "lastActor": "codex"
    }
  ],
  "reviewItems": [],
  "ownerProfileFacts": [
    {
      "factId": "01HZX0000000000000000000005",
      "section": "IDENTITY",
      "key": "species",
      "summary": "AI Agent",
      "recordedAt": "2026-01-01T00:00:00Z"
    }
  ],
  "managedAgentSpecs": [],
  "canonicalProjection": {
    "projectionId": "01HZX0000000000000000000006",
    "version": 3,
    "generatedAt": "2026-03-20T00:00:00Z",
    "acceptedReviewItemIds": [],
    "contentSummary": "Guan Guan - 3 evolution signals processed"
  },
  "domainEvents": [
    {
      "eventId": "01HZX0000000000000000000007",
      "eventType": "BEING_CREATED",
      "actor": "system",
      "occurredAt": "2026-01-01T00:00:00Z",
      "summary": "Being created"
    },
    {
      "eventId": "01HZX0000000000000000000008",
      "eventType": "BEING_IMPORTED",
      "actor": "codex",
      "occurredAt": "2026-03-22T00:00:00Z",
      "summary": "Being imported from portable snapshot"
    }
  ],
  "exportedAt": "2026-03-22T12:00:00Z"
}
```

---

## Import Behaviour

| Rule | Detail |
|---|---|
| **beingId** | Preserved from snapshot JSON |
| **Active leases** | All set to `RELEASED` — no active lease survives migration |
| **Sessions** | Preserved with original session IDs |
| **Review items** | Preserved with original review item IDs |
| **BEING_IMPORTED** | Appended to domain events with actor from import command |
| **continuityEpoch** | Increments by 1 from the imported being's epoch |

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/snapshots/beings/{beingId}/export` | Export being to portable snapshot JSON |
| `POST` | `/snapshots/beings/{beingId}/import` | Import being from portable snapshot JSON |

### Export Request

```json
{ "actor": "codex" }
```

### Export/Import Response Envelope

```json
{
  "success": true,
  "data": {
    "beingId": "01HZX0000000000000000000000",
    "exportedAt": "2026-03-22T12:00:00Z",
    "identityFacetCount": 1,
    "relationshipCount": 1,
    "sessionCount": 1,
    "leaseCount": 1,
    "reviewItemCount": 0,
    "ownerProfileFactCount": 1,
    "managedAgentSpecCount": 0,
    "snapshot": { ... }
  },
  "requestId": "01HZX0000000000000000000000"
}
```

### Import Request

```json
{
  "snapshot": { ... }
}
```

---

## Migration Path

1. **Export** — Call `POST /snapshots/beings/{id}/export` on the source runtime
2. **Stop source** — Shut down the source runtime for that being
3. **Import** — Call `POST /snapshots/beings/{id}/import` on the target runtime
4. **Verify** — All GET endpoints return identical data (verified by `JAVA-C3b`)
5. **Cutover** — Being is now served exclusively by the Java runtime

See `docs/MIGRATION-LEDGER.md` for the cutover registry.
