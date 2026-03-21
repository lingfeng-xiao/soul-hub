package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity;

import java.time.Instant;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("OwnerProfileFact")
public class OwnerProfileFactNode {

    @Id
    private String factId;

    @Property("section")
    private String section;

    @Property("key")
    private String key;

    @Property("summary")
    private String summary;

    @Property("acceptedAt")
    private Instant acceptedAt;

    public OwnerProfileFactNode() {
    }

    public OwnerProfileFactNode(String factId, String section, String key, String summary, Instant acceptedAt) {
        this.factId = factId;
        this.section = section;
        this.key = key;
        this.summary = summary;
        this.acceptedAt = acceptedAt;
    }

    public String getFactId() {
        return factId;
    }

    public void setFactId(String factId) {
        this.factId = factId;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}
