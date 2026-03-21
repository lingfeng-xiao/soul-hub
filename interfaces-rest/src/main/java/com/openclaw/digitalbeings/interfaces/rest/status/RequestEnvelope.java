package com.openclaw.digitalbeings.interfaces.rest.status;

import java.time.Instant;

public record RequestEnvelope<T>(
        String requestId,
        Instant timestamp,
        boolean success,
        T data,
        Object error
) {
}
