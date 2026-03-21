package com.openclaw.digitalbeings.application.governance;

import com.openclaw.digitalbeings.domain.being.Being;

public record GovernanceSummaryView(
        String beingId,
        String displayName,
        int identityFacetCount,
        int relationshipCount,
        int runtimeSessionCount,
        int activeLeaseCount,
        int reviewItemCount,
        int acceptedReviewItemCount,
        Long canonicalProjectionVersion,
        int ownerProfileFactCount,
        int managedAgentSpecCount
) {

    public static GovernanceSummaryView from(Being being) {
        return new GovernanceSummaryView(
                being.beingId().value(),
                being.displayName(),
                being.identityFacets().size(),
                being.relationships().size(),
                being.runtimeSessions().size(),
                (int) being.authorityLeases().stream().filter(lease -> lease.isActive()).count(),
                being.reviewItems().size(),
                (int) being.reviewItems().stream().filter(reviewItem -> reviewItem.isAccepted()).count(),
                being.canonicalProjection().map(projection -> projection.version()).orElse(null),
                being.ownerProfileFacts().size(),
                being.managedAgentSpecs().size()
        );
    }
}
