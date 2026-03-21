package com.openclaw.digitalbeings.application.relationship;

import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.identity.RelationshipEntity;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class RelationshipService {

    private final BeingStore beingStore;
    private final Clock clock;

    public RelationshipService(BeingStore beingStore, Clock clock) {
        this.beingStore = Objects.requireNonNull(beingStore, "beingStore");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public RelationshipEntityView createRelationshipEntity(CreateRelationshipEntityCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        RelationshipEntity relationshipEntity = being.addRelationshipEntity(
                command.kind(),
                command.displayName(),
                command.actor(),
                clock.instant()
        );
        beingStore.save(being);
        return RelationshipEntityView.from(command.beingId(), relationshipEntity);
    }

    public List<RelationshipEntityView> listRelationshipEntities(String beingId) {
        Being being = beingStore.requireById(requireText(beingId, "beingId"));
        return being.relationships().stream()
                .map(relationshipEntity -> RelationshipEntityView.from(beingId, relationshipEntity))
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
