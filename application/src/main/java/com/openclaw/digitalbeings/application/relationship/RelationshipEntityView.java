package com.openclaw.digitalbeings.application.relationship;

import com.openclaw.digitalbeings.domain.identity.RelationshipEntity;
import java.time.Instant;

public record RelationshipEntityView(
        String beingId,
        String relationshipEntityId,
        String kind,
        String displayName,
        Instant recordedAt
) {

    public static RelationshipEntityView from(String beingId, RelationshipEntity relationshipEntity) {
        return new RelationshipEntityView(
                beingId,
                relationshipEntity.entityId(),
                relationshipEntity.kind(),
                relationshipEntity.displayName(),
                relationshipEntity.recordedAt()
        );
    }
}
