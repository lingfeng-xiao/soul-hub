package com.openclaw.digitalbeings.application.hostcontract;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.being.CreateBeingCommand;
import com.openclaw.digitalbeings.application.support.InMemoryBeingStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HostContractServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-21T08:45:00Z"), ZoneOffset.UTC);

    @Test
    void registerHostContractUpdatesTheSameAggregateAndCanBeReadBack() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        HostContractService hostContractService = new HostContractService(store, CLOCK);
        BeingView beingView = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        HostContractView hostContractView = hostContractService.registerHostContract(
                new RegisterHostContractCommand(beingView.beingId(), "codex", "codex")
        );

        assertEquals(beingView.beingId(), hostContractView.beingId());
        assertEquals("codex", hostContractView.hostType());
        assertEquals("ACTIVE", hostContractView.status());
        assertEquals(1, hostContractService.listHostContracts(beingView.beingId()).size());
    }
}
