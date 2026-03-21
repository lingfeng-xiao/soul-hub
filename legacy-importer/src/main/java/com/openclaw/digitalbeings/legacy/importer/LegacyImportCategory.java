package com.openclaw.digitalbeings.legacy.importer;

import java.util.Locale;

public enum LegacyImportCategory {
    BEINGS("beings"),
    RELATIONSHIPS("relationships"),
    ACCEPTED_REVIEW_ITEMS("accepted-review-items"),
    OWNER_PROFILE_FACTS("owner-profile-facts"),
    SESSIONS_AND_LEASES_METADATA("sessions-and-leases-metadata"),
    SNAPSHOTS("snapshots"),
    OPENCLAW_RUNTIME_PATCHES("openclaw-runtime-patches"),
    LIVE_LOG_SCANNING("live-log-scanning"),
    GENERATED_MARKDOWN_MIRRORS("generated-markdown-mirrors");

    private final String planKey;

    LegacyImportCategory(String planKey) {
        this.planKey = planKey;
    }

    public String planKey() {
        return planKey;
    }

    public static LegacyImportCategory fromPlanKey(String planKey) {
        String normalized = planKey.toLowerCase(Locale.ROOT);
        for (LegacyImportCategory value : values()) {
            if (value.planKey.equals(normalized)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown legacy import category: " + planKey);
    }
}
