package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.mapper;

import com.openclaw.digitalbeings.domain.core.ReviewItemStatus;
import com.openclaw.digitalbeings.domain.review.CanonicalProjection;
import com.openclaw.digitalbeings.domain.review.ReviewItem;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.CanonicalProjectionNode;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.ReviewItemNode;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;

final class ReviewSliceMapper {

    private ReviewSliceMapper() {
    }

    static List<ReviewItemNode> toReviewNodes(List<ReviewItem> reviewItems) {
        return reviewItems.stream()
                .map(reviewItem -> new ReviewItemNode(
                        reviewItem.reviewItemId(),
                        reviewItem.lane(),
                        reviewItem.kind(),
                        reviewItem.proposal(),
                        reviewItem.status(),
                        reviewItem.createdAt(),
                        reviewItem.updatedAt(),
                        reviewItem.lastActor()
                ))
                .toList();
    }

    static List<ReviewItem> toReviewItems(List<ReviewItemNode> nodes) {
        return nodes.stream().map(ReviewSliceMapper::toReviewItem).toList();
    }

    static CanonicalProjectionNode toCanonicalProjectionNode(CanonicalProjection projection) {
        if (projection == null) {
            return null;
        }
        return new CanonicalProjectionNode(
                projection.projectionId(),
                projection.version(),
                projection.generatedAt(),
                projection.acceptedReviewItemIds(),
                projection.contentSummary()
        );
    }

    static CanonicalProjection toCanonicalProjection(CanonicalProjectionNode node) {
        if (node == null) {
            return null;
        }
        return new CanonicalProjection(
                node.getProjectionId(),
                node.getVersion(),
                node.getGeneratedAt(),
                node.getAcceptedReviewItemIds(),
                node.getContentSummary()
        );
    }

    private static ReviewItem toReviewItem(ReviewItemNode node) {
        try {
            Constructor<ReviewItem> constructor = ReviewItem.class.getDeclaredConstructor(
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    ReviewItemStatus.class,
                    Instant.class,
                    Instant.class,
                    String.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    node.getReviewItemId(),
                    node.getLane(),
                    node.getKind(),
                    node.getProposal(),
                    node.getStatus(),
                    node.getCreatedAt(),
                    node.getUpdatedAt(),
                    node.getLastActor()
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to rehydrate ReviewItem.", exception);
        }
    }
}
