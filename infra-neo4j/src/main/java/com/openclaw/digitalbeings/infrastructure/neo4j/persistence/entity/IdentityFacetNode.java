package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity;

import java.time.Instant;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("IdentityFacet")
public class IdentityFacetNode {

    @Id
    private String facetId;

    @Property("kind")
    private String kind;

    @Property("summary")
    private String summary;

    @Property("recordedAt")
    private Instant recordedAt;

    public IdentityFacetNode() {
    }

    public IdentityFacetNode(String facetId, String kind, String summary, Instant recordedAt) {
        this.facetId = facetId;
        this.kind = kind;
        this.summary = summary;
        this.recordedAt = recordedAt;
    }

    public String getFacetId() {
        return facetId;
    }

    public void setFacetId(String facetId) {
        this.facetId = facetId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
