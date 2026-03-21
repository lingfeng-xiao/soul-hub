package com.openclaw.digitalbeings.application.governance;

import com.openclaw.digitalbeings.domain.governance.OwnerProfileFact;
import java.time.Instant;

public record OwnerProfileFactView(
        String beingId,
        String factId,
        String section,
        String key,
        String summary,
        Instant acceptedAt
) {

    public static OwnerProfileFactView from(String beingId, OwnerProfileFact fact) {
        return new OwnerProfileFactView(
                beingId,
                fact.factId(),
                fact.section(),
                fact.key(),
                fact.summary(),
                fact.acceptedAt()
        );
    }
}
