package com.openclaw.digitalbeings.application.hostcontract;

import com.openclaw.digitalbeings.domain.runtime.HostContract;
import java.time.Instant;

public record HostContractView(
        String beingId,
        String contractId,
        String hostType,
        String status,
        Instant registeredAt
) {

    public static HostContractView from(String beingId, HostContract hostContract) {
        return new HostContractView(
                beingId,
                hostContract.contractId(),
                hostContract.hostType(),
                hostContract.status(),
                hostContract.registeredAt()
        );
    }
}
