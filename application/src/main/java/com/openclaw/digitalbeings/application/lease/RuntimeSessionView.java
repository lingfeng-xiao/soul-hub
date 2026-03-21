package com.openclaw.digitalbeings.application.lease;

import com.openclaw.digitalbeings.domain.runtime.RuntimeSession;
import java.time.Instant;

public record RuntimeSessionView(
        String beingId,
        String sessionId,
        String hostType,
        Instant startedAt,
        Instant endedAt
) {

    public static RuntimeSessionView from(String beingId, RuntimeSession runtimeSession) {
        return new RuntimeSessionView(
                beingId,
                runtimeSession.sessionId(),
                runtimeSession.hostType(),
                runtimeSession.startedAt(),
                runtimeSession.endedAt()
        );
    }
}
