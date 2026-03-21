package com.openclaw.digitalbeings.interfaces.rest.lease;

public record RegisterRuntimeSessionRequest(
        String beingId,
        String hostType,
        String actor
) {
}
