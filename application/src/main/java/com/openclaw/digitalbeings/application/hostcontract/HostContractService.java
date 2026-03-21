package com.openclaw.digitalbeings.application.hostcontract;

import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.runtime.HostContract;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class HostContractService {

    private final BeingStore beingStore;
    private final Clock clock;

    public HostContractService(BeingStore beingStore, Clock clock) {
        this.beingStore = Objects.requireNonNull(beingStore, "beingStore");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public HostContractView registerHostContract(RegisterHostContractCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        HostContract hostContract = being.registerHostContract(command.hostType(), command.actor(), clock.instant());
        beingStore.save(being);
        return HostContractView.from(command.beingId(), hostContract);
    }

    public List<HostContractView> listHostContracts(String beingId) {
        Being being = beingStore.requireById(requireText(beingId, "beingId"));
        return being.hostContracts().stream()
                .map(hostContract -> HostContractView.from(beingId, hostContract))
                .toList();
    }

    public BeingView getBeing(String beingId) {
        return BeingView.from(beingStore.requireById(requireText(beingId, "beingId")));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
