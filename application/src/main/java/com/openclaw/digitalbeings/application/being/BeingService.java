package com.openclaw.digitalbeings.application.being;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class BeingService {

    private final BeingStore beingStore;
    private final Clock clock;

    public BeingService(BeingStore beingStore, Clock clock) {
        this.beingStore = Objects.requireNonNull(beingStore, "beingStore");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public BeingView createBeing(CreateBeingCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = Being.create(command.displayName(), command.actor(), clock.instant());
        beingStore.save(being);
        return BeingView.from(being);
    }

    public BeingView getBeing(String beingId) {
        return BeingView.from(beingStore.requireById(requireText(beingId, "beingId")));
    }

    public List<BeingView> listBeings() {
        return beingStore.findAll().stream().map(BeingView::from).toList();
    }

    public IdentityFacetView addIdentityFacet(String beingId, String kind, String summary, String actor, Instant now) {
        Being being = beingStore.requireById(requireText(beingId, "beingId"));
        being.addIdentityFacet(requireText(kind, "kind"), requireText(summary, "summary"), requireText(actor, "actor"), Objects.requireNonNull(now, "now"));
        beingStore.save(being);
        return IdentityFacetView.from(being.identityFacets().get(being.identityFacets().size() - 1));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
