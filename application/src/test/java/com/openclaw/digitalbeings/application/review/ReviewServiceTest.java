package com.openclaw.digitalbeings.application.review;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.being.CreateBeingCommand;
import com.openclaw.digitalbeings.application.support.InMemoryBeingStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReviewServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-21T08:20:00Z"), ZoneOffset.UTC);

    @Test
    void reviewLifecycleProducesCanonicalProjection() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        ReviewService reviewService = new ReviewService(store, CLOCK);
        BeingView beingView = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        ReviewItemView draft = reviewService.draftReview(
                new DraftReviewCommand(beingView.beingId(), "canonical", "profile-update", "normalize profile", "codex")
        );
        reviewService.submitReview(new SubmitReviewCommand(beingView.beingId(), draft.reviewItemId(), "codex"));
        ReviewItemView accepted = reviewService.decideReview(
                new DecideReviewCommand(beingView.beingId(), draft.reviewItemId(), ReviewDecision.ACCEPTED, "codex")
        );
        CanonicalProjectionView projection = reviewService.rebuildCanonicalProjection(
                new RebuildCanonicalProjectionCommand(beingView.beingId(), "codex")
        );

        assertEquals("ACCEPTED", accepted.status());
        assertEquals(1L, projection.version());
        assertEquals(1, projection.acceptedReviewItemIds().size());
    }

    @Test
    void getCanonicalProjectionReturnsNullUntilRebuilt() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        ReviewService reviewService = new ReviewService(store, CLOCK);
        BeingView beingView = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        assertNull(reviewService.getCanonicalProjection(beingView.beingId()));
    }
}
