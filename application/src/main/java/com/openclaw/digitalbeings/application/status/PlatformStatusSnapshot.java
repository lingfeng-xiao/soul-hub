package com.openclaw.digitalbeings.application.status;

public record PlatformStatusSnapshot(
        String currentPhase,
        String currentFocus,
        String statusDocument,
        String nextRecommendedAction
) {
}
