package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity;

import com.openclaw.digitalbeings.domain.core.SnapshotType;
import java.time.Instant;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("ContinuitySnapshot")
public class ContinuitySnapshotNode {

    @Id
    private String snapshotId;

    @Property("type")
    private SnapshotType type;

    @Property("summary")
    private String summary;

    @Property("createdAt")
    private Instant createdAt;

    public ContinuitySnapshotNode() {
    }

    public ContinuitySnapshotNode(String snapshotId, SnapshotType type, String summary, Instant createdAt) {
        this.snapshotId = snapshotId;
        this.type = type;
        this.summary = summary;
        this.createdAt = createdAt;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public SnapshotType getType() {
        return type;
    }

    public void setType(SnapshotType type) {
        this.type = type;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
