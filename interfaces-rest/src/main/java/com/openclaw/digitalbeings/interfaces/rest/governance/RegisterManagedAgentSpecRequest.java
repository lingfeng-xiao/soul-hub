package com.openclaw.digitalbeings.interfaces.rest.governance;

public record RegisterManagedAgentSpecRequest(
        String beingId,
        String role,
        String status,
        String actor
) {
}
