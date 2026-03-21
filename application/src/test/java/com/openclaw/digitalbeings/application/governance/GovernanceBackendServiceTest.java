package com.openclaw.digitalbeings.application.governance;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.being.CreateBeingCommand;
import com.openclaw.digitalbeings.application.review.CanonicalProjectionView;
import com.openclaw.digitalbeings.application.review.DecideReviewCommand;
import com.openclaw.digitalbeings.application.review.DraftReviewCommand;
import com.openclaw.digitalbeings.application.review.RebuildCanonicalProjectionCommand;
import com.openclaw.digitalbeings.application.review.ReviewDecision;
import com.openclaw.digitalbeings.application.review.ReviewItemView;
import com.openclaw.digitalbeings.application.review.ReviewService;
import com.openclaw.digitalbeings.application.review.SubmitReviewCommand;
import com.openclaw.digitalbeings.application.support.InMemoryBeingStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GovernanceBackendServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-21T08:55:00Z"), ZoneOffset.UTC);

    @Test
    void buildSummaryCombinesReviewCockpitOwnerProfileCompilationAndProjection() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        ReviewService reviewService = new ReviewService(store, CLOCK);
        GovernanceService governanceService = new GovernanceService(store, CLOCK);
        GovernanceBackendService backendService = new GovernanceBackendService(store, reviewService, governanceService);
        BeingView beingView = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        governanceService.recordOwnerProfileFact(
                new RecordOwnerProfileFactCommand(beingView.beingId(), "preferences", "tone", "prefers warm collaboration", "codex")
        );
        governanceService.recordOwnerProfileFact(
                new RecordOwnerProfileFactCommand(beingView.beingId(), "identity", "voice", "speaks with steady clarity", "codex")
        );
        ReviewItemView draftReview = reviewService.draftReview(
                new DraftReviewCommand(beingView.beingId(), "canonical", "profile-update", "normalize profile", "codex")
        );
        reviewService.submitReview(new SubmitReviewCommand(beingView.beingId(), draftReview.reviewItemId(), "codex"));
        reviewService.decideReview(
                new DecideReviewCommand(beingView.beingId(), draftReview.reviewItemId(), ReviewDecision.ACCEPTED, "codex")
        );

        CanonicalProjectionView rebuilt = backendService.rebuildCanonicalProjection(
                new RebuildCanonicalProjectionCommand(beingView.beingId(), "codex")
        );
        GovernanceBackendSummaryView summary = backendService.buildSummary(beingView.beingId());

        assertEquals(beingView.beingId(), summary.beingId());
        assertEquals("guan-guan", summary.displayName());
        assertEquals(1L, summary.reviewCockpit().reviewItemCount());
        assertEquals(1L, summary.reviewCockpit().acceptedCount());
        assertEquals(0, summary.reviewCockpit().openReviewItemIds().size());
        assertEquals(draftReview.reviewItemId(), summary.reviewCockpit().acceptedReviewItemIds().getFirst());
        assertEquals(rebuilt.version(), summary.canonicalProjection().version());
        assertEquals(2, summary.ownerProfileCompilation().factCount());
        assertEquals("identity", summary.ownerProfileCompilation().sections().getFirst());
        assertEquals("identity.voice = speaks with steady clarity", summary.ownerProfileCompilation().lines().getFirst());
    }

    @Test
    void compileOwnerProfileReadModelSortsFactsDeterministically() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        ReviewService reviewService = new ReviewService(store, CLOCK);
        GovernanceService governanceService = new GovernanceService(store, CLOCK);
        GovernanceBackendService backendService = new GovernanceBackendService(store, reviewService, governanceService);
        BeingView beingView = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        governanceService.recordOwnerProfileFact(
                new RecordOwnerProfileFactCommand(beingView.beingId(), "preferences", "tone", "prefers warm collaboration", "codex")
        );
        governanceService.recordOwnerProfileFact(
                new RecordOwnerProfileFactCommand(beingView.beingId(), "identity", "voice", "speaks with steady clarity", "codex")
        );

        OwnerProfileCompilationView compiled = backendService.compileOwnerProfileReadModel(beingView.beingId());

        assertEquals(2, compiled.factCount());
        assertEquals(List.of("identity", "preferences"), compiled.sections());
        assertEquals(
                List.of(
                        "identity.voice = speaks with steady clarity",
                        "preferences.tone = prefers warm collaboration"
                ),
                compiled.lines()
        );
        assertEquals(
                "identity.voice = speaks with steady clarity\npreferences.tone = prefers warm collaboration",
                compiled.compiledText()
        );
    }

    @Test
    void buildReviewCockpitSummaryReportsOpenAndAcceptedItems() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        ReviewService reviewService = new ReviewService(store, CLOCK);
        GovernanceService governanceService = new GovernanceService(store, CLOCK);
        GovernanceBackendService backendService = new GovernanceBackendService(store, reviewService, governanceService);
        BeingView beingView = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        ReviewItemView draftReview = reviewService.draftReview(
                new DraftReviewCommand(beingView.beingId(), "canonical", "profile-update", "normalize profile", "codex")
        );
        ReviewItemView secondDraft = reviewService.draftReview(
                new DraftReviewCommand(beingView.beingId(), "ops", "lease-policy", "tighten lease policy", "codex")
        );
        reviewService.submitReview(new SubmitReviewCommand(beingView.beingId(), draftReview.reviewItemId(), "codex"));
        reviewService.submitReview(new SubmitReviewCommand(beingView.beingId(), secondDraft.reviewItemId(), "codex"));
        reviewService.decideReview(
                new DecideReviewCommand(beingView.beingId(), draftReview.reviewItemId(), ReviewDecision.ACCEPTED, "codex")
        );

        ReviewCockpitSummaryView cockpit = backendService.buildReviewCockpitSummary(beingView.beingId());

        assertEquals(2L, cockpit.reviewItemCount());
        assertEquals(0L, cockpit.draftCount());
        assertEquals(1L, cockpit.submittedCount());
        assertEquals(1L, cockpit.acceptedCount());
        assertEquals(List.of(secondDraft.reviewItemId()), cockpit.openReviewItemIds());
        assertEquals(List.of(draftReview.reviewItemId()), cockpit.acceptedReviewItemIds());
        assertNull(cockpit.canonicalProjectionVersion());
    }
}
