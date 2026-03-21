package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity;

import java.time.Instant;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("ManagedAgentSpec")
public class ManagedAgentSpecNode {

    @Id
    private String managedAgentId;

    @Property("role")
    private String role;

    @Property("status")
    private String status;

    @Property("createdAt")
    private Instant createdAt;

    public ManagedAgentSpecNode() {
    }

    public ManagedAgentSpecNode(String managedAgentId, String role, String status, Instant createdAt) {
        this.managedAgentId = managedAgentId;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getManagedAgentId() {
        return managedAgentId;
    }

    public void setManagedAgentId(String managedAgentId) {
        this.managedAgentId = managedAgentId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
