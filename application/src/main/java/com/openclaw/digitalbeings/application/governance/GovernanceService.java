package com.openclaw.digitalbeings.application.governance;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.governance.ManagedAgentSpec;
import com.openclaw.digitalbeings.domain.governance.OwnerProfileFact;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class GovernanceService {

    private final BeingStore beingStore;
    private final Clock clock;

    public GovernanceService(BeingStore beingStore, Clock clock) {
        this.beingStore = Objects.requireNonNull(beingStore, "beingStore");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public OwnerProfileFactView recordOwnerProfileFact(RecordOwnerProfileFactCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        OwnerProfileFact ownerProfileFact = being.recordOwnerProfileFact(
                command.section(),
                command.key(),
                command.summary(),
                command.actor(),
                clock.instant()
        );
        beingStore.save(being);
        return OwnerProfileFactView.from(command.beingId(), ownerProfileFact);
    }

    public List<OwnerProfileFactView> listOwnerProfileFacts(String beingId) {
        Being being = beingStore.requireById(requireText(beingId, "beingId"));
        return being.ownerProfileFacts().stream()
                .map(fact -> OwnerProfileFactView.from(beingId, fact))
                .toList();
    }

    public ManagedAgentSpecView registerManagedAgentSpec(RegisterManagedAgentSpecCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        ManagedAgentSpec managedAgentSpec = being.registerManagedAgentSpec(
                command.role(),
                command.status(),
                command.actor(),
                clock.instant()
        );
        beingStore.save(being);
        return ManagedAgentSpecView.from(command.beingId(), managedAgentSpec);
    }

    public List<ManagedAgentSpecView> listManagedAgentSpecs(String beingId) {
        Being being = beingStore.requireById(requireText(beingId, "beingId"));
        return being.managedAgentSpecs().stream()
                .map(spec -> ManagedAgentSpecView.from(beingId, spec))
                .toList();
    }

    public GovernanceSummaryView getGovernanceSummary(String beingId) {
        Being being = beingStore.requireById(requireText(beingId, "beingId"));
        return GovernanceSummaryView.from(being);
    }

    public OwnerProfileCompilationView getOwnerProfile(String beingId) {
        Being being = beingStore.requireById(requireText(beingId, "beingId"));
        List<OwnerProfileFactView> facts = being.ownerProfileFacts().stream()
                .map(fact -> OwnerProfileFactView.from(beingId, fact))
                .toList();
        return OwnerProfileCompilationView.from(beingId, facts);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
