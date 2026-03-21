package com.openclaw.digitalbeings.application.review;

import com.openclaw.digitalbeings.domain.review.ReviewItem;
import java.time.Instant;

public record ReviewItemView(
        String beingId,
        String reviewItemId,
        String lane,
        String kind,
        String proposal,
        String status,
        Instant createdAt,
        Instant updatedAt,
        String lastActor
) {

    public static ReviewItemView from(String beingId, ReviewItem reviewItem) {
        return new ReviewItemView(
                beingId,
                reviewItem.reviewItemId(),
                reviewItem.lane(),
                reviewItem.kind(),
                reviewItem.proposal(),
                reviewItem.status().name(),
                reviewItem.createdAt(),
                reviewItem.updatedAt(),
                reviewItem.lastActor()
        );
    }
}
