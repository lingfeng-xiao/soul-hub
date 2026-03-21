package com.openclaw.digitalbeings.application.governance;

public record RegisterManagedAgentSpecCommand(
        String beingId,
        String role,
        String status,
        String actor
) {
}
