package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity;

import java.time.Instant;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("RelationshipEntity")
public class RelationshipEntityNode {

    @Id
    private String entityId;

    @Property("kind")
    private String kind;

    @Property("displayName")
    private String displayName;

    @Property("recordedAt")
    private Instant recordedAt;

    public RelationshipEntityNode() {
    }

    public RelationshipEntityNode(String entityId, String kind, String displayName, Instant recordedAt) {
        this.entityId = entityId;
        this.kind = kind;
        this.displayName = displayName;
        this.recordedAt = recordedAt;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
