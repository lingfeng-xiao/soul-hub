package com.openclaw.digitalbeings.application.snapshot;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.snapshot.ContinuitySnapshot;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class SnapshotService {

    private final BeingStore beingStore;
    private final Clock clock;

    public SnapshotService(BeingStore beingStore, Clock clock) {
        this.beingStore = Objects.requireNonNull(beingStore, "beingStore");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SnapshotView createSnapshot(CreateSnapshotCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        ContinuitySnapshot snapshot = being.createSnapshot(
                command.type(),
                command.summary(),
                command.actor(),
                clock.instant()
        );
        beingStore.save(being);
        return SnapshotView.from(command.beingId(), snapshot);
    }

    public List<SnapshotView> listSnapshots(String beingId) {
        Being being = beingStore.requireById(requireText(beingId, "beingId"));
        return being.continuitySnapshots().stream()
                .map(snapshot -> SnapshotView.from(beingId, snapshot))
                .toList();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
