package com.openclaw.digitalbeings.domain.review;

import com.openclaw.digitalbeings.domain.core.DomainRuleViolation;
import com.openclaw.digitalbeings.domain.core.UlidFactory;
import java.time.Instant;
import java.util.List;

public record CanonicalProjection(
        String projectionId,
        long version,
        Instant generatedAt,
        List<String> acceptedReviewItemIds,
        String contentSummary
) {

    public CanonicalProjection {
        projectionId = requireText(projectionId, "projectionId");
        if (version <= 0) {
            throw new IllegalArgumentException("version must be positive.");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("generatedAt must not be null.");
        }
        acceptedReviewItemIds = List.copyOf(acceptedReviewItemIds);
        contentSummary = requireText(contentSummary, "contentSummary");
    }

    public static CanonicalProjection rebuild(
            CanonicalProjection current,
            List<ReviewItem> acceptedReviews,
            Instant generatedAt
    ) {
        if (generatedAt == null) {
            throw new IllegalArgumentException("generatedAt must not be null.");
        }
        if (acceptedReviews.stream().anyMatch(reviewItem -> !reviewItem.isAccepted())) {
            throw new DomainRuleViolation("Only accepted review items may enter the canonical projection.");
        }
        long nextVersion = current == null ? 1 : current.version() + 1;
        List<String> reviewIds = acceptedReviews.stream()
                .map(ReviewItem::reviewItemId)
                .sorted()
                .toList();
        String summary = reviewIds.isEmpty() ? "no-accepted-reviews" : String.join(",", reviewIds);
        return new CanonicalProjection(UlidFactory.newUlid(), nextVersion, generatedAt, reviewIds, summary);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
