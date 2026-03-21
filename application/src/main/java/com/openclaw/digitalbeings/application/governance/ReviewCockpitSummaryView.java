package com.openclaw.digitalbeings.application.governance;

import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.core.ReviewItemStatus;
import com.openclaw.digitalbeings.domain.review.ReviewItem;
import java.util.List;

public record ReviewCockpitSummaryView(
        String beingId,
        long reviewItemCount,
        long draftCount,
        long submittedCount,
        long acceptedCount,
        long rejectedCount,
        long deferredCount,
        long cancelledCount,
        List<String> openReviewItemIds,
        List<String> acceptedReviewItemIds,
        Long canonicalProjectionVersion
) {

    public static ReviewCockpitSummaryView from(Being being) {
        List<ReviewItem> reviewItems = being.reviewItems();
        return new ReviewCockpitSummaryView(
                being.beingId().value(),
                reviewItems.size(),
                count(reviewItems, ReviewItemStatus.DRAFT),
                count(reviewItems, ReviewItemStatus.SUBMITTED),
                count(reviewItems, ReviewItemStatus.ACCEPTED),
                count(reviewItems, ReviewItemStatus.REJECTED),
                count(reviewItems, ReviewItemStatus.DEFERRED),
                count(reviewItems, ReviewItemStatus.CANCELLED),
                reviewItems.stream()
                        .filter(reviewItem -> reviewItem.status() == ReviewItemStatus.DRAFT
                                || reviewItem.status() == ReviewItemStatus.SUBMITTED
                                || reviewItem.status() == ReviewItemStatus.DEFERRED)
                        .map(ReviewItem::reviewItemId)
                        .sorted()
                        .toList(),
                reviewItems.stream()
                        .filter(ReviewItem::isAccepted)
                        .map(ReviewItem::reviewItemId)
                        .sorted()
                        .toList(),
                being.canonicalProjection().map(projection -> projection.version()).orElse(null)
        );
    }

    private static long count(List<ReviewItem> reviewItems, ReviewItemStatus targetStatus) {
        return reviewItems.stream().filter(reviewItem -> reviewItem.status() == targetStatus).count();
    }
}
