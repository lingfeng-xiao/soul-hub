package com.openclaw.digitalbeings.application.being;

import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.core.ReviewItemStatus;
import java.time.Instant;

public record BeingView(
        String beingId,
        String displayName,
        long revision,
        Instant createdAt,
        int identityFacetCount,
        int relationshipCount,
        int runtimeSessionCount,
        int activeLeaseCount,
        int reviewItemCount,
        int acceptedReviewItemCount,
        Long canonicalProjectionVersion
) {

    public static BeingView from(Being being) {
        return new BeingView(
                being.beingId().value(),
                being.displayName(),
                being.revision(),
                being.createdAt(),
                being.identityFacets().size(),
                being.relationships().size(),
                being.runtimeSessions().size(),
                (int) being.authorityLeases().stream().filter(lease -> lease.isActive()).count(),
                being.reviewItems().size(),
                (int) being.reviewItems().stream().filter(reviewItem -> reviewItem.status() == ReviewItemStatus.ACCEPTED).count(),
                being.canonicalProjection().map(projection -> projection.version()).orElse(null)
        );
    }
}
