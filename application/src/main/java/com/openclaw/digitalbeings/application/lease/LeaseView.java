package com.openclaw.digitalbeings.application.lease;

import com.openclaw.digitalbeings.domain.runtime.AuthorityLease;
import com.openclaw.digitalbeings.domain.runtime.RuntimeSession;
import java.time.Instant;

public record LeaseView(
        String beingId,
        String leaseId,
        String sessionId,
        String status,
        Instant requestedAt,
        Instant grantedAt,
        Instant releasedAt,
        String lastActor
) {

    public static LeaseView from(String beingId, AuthorityLease lease, RuntimeSession session) {
        return new LeaseView(
                beingId,
                lease.leaseId(),
                session.sessionId(),
                lease.status().name(),
                lease.requestedAt(),
                lease.grantedAt(),
                lease.releasedAt(),
                lease.lastActor()
        );
    }
}
