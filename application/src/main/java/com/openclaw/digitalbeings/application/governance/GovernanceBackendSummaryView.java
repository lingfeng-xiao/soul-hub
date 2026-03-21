package com.openclaw.digitalbeings.application.governance;

import com.openclaw.digitalbeings.application.review.CanonicalProjectionView;

public record GovernanceBackendSummaryView(
        String beingId,
        String displayName,
        ReviewCockpitSummaryView reviewCockpit,
        OwnerProfileCompilationView ownerProfileCompilation,
        CanonicalProjectionView canonicalProjection
) {
}
