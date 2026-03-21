package com.openclaw.digitalbeings.application.governance;

import com.openclaw.digitalbeings.domain.governance.ManagedAgentSpec;
import java.time.Instant;

public record ManagedAgentSpecView(
        String beingId,
        String managedAgentId,
        String role,
        String status,
        Instant createdAt
) {

    public static ManagedAgentSpecView from(String beingId, ManagedAgentSpec spec) {
        return new ManagedAgentSpecView(
                beingId,
                spec.managedAgentId(),
                spec.role(),
                spec.status(),
                spec.createdAt()
        );
    }
}
