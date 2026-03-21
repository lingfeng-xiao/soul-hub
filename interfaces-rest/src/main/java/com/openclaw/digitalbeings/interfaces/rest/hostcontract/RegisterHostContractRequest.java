package com.openclaw.digitalbeings.interfaces.rest.hostcontract;

public record RegisterHostContractRequest(
        String beingId,
        String hostType,
        String actor
) {
}
