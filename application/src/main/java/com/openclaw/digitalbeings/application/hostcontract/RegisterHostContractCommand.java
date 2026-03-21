package com.openclaw.digitalbeings.application.hostcontract;

public record RegisterHostContractCommand(
        String beingId,
        String hostType,
        String actor
) {
}
