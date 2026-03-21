package com.openclaw.digitalbeings.application.governance;

import com.openclaw.digitalbeings.application.review.CanonicalProjectionView;
import com.openclaw.digitalbeings.application.review.RebuildCanonicalProjectionCommand;
import com.openclaw.digitalbeings.application.review.ReviewService;
import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import java.util.List;
import java.util.Objects;

public final class GovernanceBackendService {

    private final BeingStore beingStore;
    private final ReviewService reviewService;
    private final GovernanceService governanceService;

    public GovernanceBackendService(BeingStore beingStore, ReviewService reviewService, GovernanceService governanceService) {
        this.beingStore = Objects.requireNonNull(beingStore, "beingStore");
        this.reviewService = Objects.requireNonNull(reviewService, "reviewService");
        this.governanceService = Objects.requireNonNull(governanceService, "governanceService");
    }

    public GovernanceBackendSummaryView buildSummary(String beingId) {
        Being being = requireBeing(beingId);
        return new GovernanceBackendSummaryView(
                being.beingId().value(),
                being.displayName(),
                ReviewCockpitSummaryView.from(being),
                compileOwnerProfileReadModel(beingId),
                reviewService.getCanonicalProjection(beingId)
        );
    }

    public ReviewCockpitSummaryView buildReviewCockpitSummary(String beingId) {
        return ReviewCockpitSummaryView.from(requireBeing(beingId));
    }

    public OwnerProfileCompilationView compileOwnerProfileReadModel(String beingId) {
        List<OwnerProfileFactView> facts = governanceService.listOwnerProfileFacts(beingId);
        return OwnerProfileCompilationView.from(beingId, facts);
    }

    public CanonicalProjectionView rebuildCanonicalProjection(RebuildCanonicalProjectionCommand command) {
        Objects.requireNonNull(command, "command");
        return reviewService.rebuildCanonicalProjection(command);
    }

    private Being requireBeing(String beingId) {
        return beingStore.requireById(requireText(beingId, "beingId"));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
