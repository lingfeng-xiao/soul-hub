package com.openclaw.digitalbeings.application.review;

public record DraftReviewCommand(
        String beingId,
        String lane,
        String kind,
        String proposal,
        String actor
) {

    public DraftReviewCommand {
        beingId = requireText(beingId, "beingId");
        lane = requireText(lane, "lane");
        kind = requireText(kind, "kind");
        proposal = requireText(proposal, "proposal");
        actor = requireText(actor, "actor");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
