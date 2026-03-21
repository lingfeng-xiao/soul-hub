package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity;

import java.time.Instant;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("HostContract")
public class HostContractNode {

    @Id
    private String contractId;

    @Property("hostType")
    private String hostType;

    @Property("status")
    private String status;

    @Property("registeredAt")
    private Instant registeredAt;

    public HostContractNode() {
    }

    public HostContractNode(String contractId, String hostType, String status, Instant registeredAt) {
        this.contractId = contractId;
        this.hostType = hostType;
        this.status = status;
        this.registeredAt = registeredAt;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getHostType() {
        return hostType;
    }

    public void setHostType(String hostType) {
        this.hostType = hostType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
}
