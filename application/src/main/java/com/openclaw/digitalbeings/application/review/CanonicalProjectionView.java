package com.openclaw.digitalbeings.application.review;

import com.openclaw.digitalbeings.domain.review.CanonicalProjection;
import java.time.Instant;
import java.util.List;

public record CanonicalProjectionView(
        String beingId,
        String projectionId,
        long version,
        Instant generatedAt,
        List<String> acceptedReviewItemIds,
        String contentSummary
) {

    public static CanonicalProjectionView from(String beingId, CanonicalProjection projection) {
        return new CanonicalProjectionView(
                beingId,
                projection.projectionId(),
                projection.version(),
                projection.generatedAt(),
                projection.acceptedReviewItemIds(),
                projection.contentSummary()
        );
    }
}
