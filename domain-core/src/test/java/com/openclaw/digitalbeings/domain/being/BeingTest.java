package com.openclaw.digitalbeings.domain.being;

import com.openclaw.digitalbeings.domain.core.DomainRuleViolation;
import com.openclaw.digitalbeings.domain.core.ReviewItemStatus;
import com.openclaw.digitalbeings.domain.core.SnapshotType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BeingTest {

    @Test
    void onlyOneActiveAuthoritativeLeaseMayExist() {
        Instant now = Instant.parse("2026-03-21T07:00:00Z");
        Being being = Being.create("guan-guan", "codex", now);
        String firstSessionId = being.registerRuntimeSession("codex", "codex", now.plusSeconds(1)).sessionId();
        String secondSessionId = being.registerRuntimeSession("codex", "codex", now.plusSeconds(2)).sessionId();

        being.acquireAuthorityLease(firstSessionId, "codex", now.plusSeconds(3));

        assertThrows(
                DomainRuleViolation.class,
                () -> being.acquireAuthorityLease(secondSessionId, "codex", now.plusSeconds(4))
        );
    }

    @Test
    void reviewStateMachineRejectsAcceptingDraftReview() {
        Instant now = Instant.parse("2026-03-21T07:10:00Z");
        Being being = Being.create("guan-guan", "codex", now);
        String reviewItemId = being.draftReview("canonical", "profile-update", "normalize owner profile", "codex", now.plusSeconds(1))
                .reviewItemId();

        assertThrows(
                DomainRuleViolation.class,
                () -> being.decideReview(reviewItemId, ReviewItemStatus.ACCEPTED, "codex", now.plusSeconds(2))
        );
    }

    @Test
    void canonicalProjectionIncludesOnlyAcceptedReviewItems() {
        Instant now = Instant.parse("2026-03-21T07:20:00Z");
        Being being = Being.create("guan-guan", "codex", now);

        String acceptedReviewId = being.draftReview("canonical", "identity", "accept identity facet", "codex", now.plusSeconds(1))
                .reviewItemId();
        being.submitReview(acceptedReviewId, "codex", now.plusSeconds(2));
        being.decideReview(acceptedReviewId, ReviewItemStatus.ACCEPTED, "codex", now.plusSeconds(3));

        String rejectedReviewId = being.draftReview("canonical", "identity", "reject identity facet", "codex", now.plusSeconds(4))
                .reviewItemId();
        being.submitReview(rejectedReviewId, "codex", now.plusSeconds(5));
        being.decideReview(rejectedReviewId, ReviewItemStatus.REJECTED, "codex", now.plusSeconds(6));

        var projection = being.rebuildCanonicalProjection("codex", now.plusSeconds(7));

        assertEquals(List.of(acceptedReviewId), projection.acceptedReviewItemIds());
    }

    @Test
    void postRestoreSnapshotCannotBeCreatedWhileAnActiveLeaseExists() {
        Instant now = Instant.parse("2026-03-21T07:30:00Z");
        Being being = Being.create("guan-guan", "codex", now);
        String sessionId = being.registerRuntimeSession("codex", "codex", now.plusSeconds(1)).sessionId();
        being.acquireAuthorityLease(sessionId, "codex", now.plusSeconds(2));

        assertThrows(
                DomainRuleViolation.class,
                () -> being.createSnapshot(SnapshotType.POST_RESTORE, "post restore validation", "codex", now.plusSeconds(3))
        );
    }
}
