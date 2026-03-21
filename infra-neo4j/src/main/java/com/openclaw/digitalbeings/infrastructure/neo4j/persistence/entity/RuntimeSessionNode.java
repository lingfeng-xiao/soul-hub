package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity;

import java.time.Instant;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("RuntimeSession")
public class RuntimeSessionNode {

    @Id
    private String sessionId;

    @Property("hostType")
    private String hostType;

    @Property("startedAt")
    private Instant startedAt;

    @Property("endedAt")
    private Instant endedAt;

    public RuntimeSessionNode() {
    }

    public RuntimeSessionNode(String sessionId, String hostType, Instant startedAt, Instant endedAt) {
        this.sessionId = sessionId;
        this.hostType = hostType;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getHostType() {
        return hostType;
    }

    public void setHostType(String hostType) {
        this.hostType = hostType;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}
