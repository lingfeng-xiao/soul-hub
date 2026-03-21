package com.openclaw.digitalbeings.legacy.importer;

import java.util.List;

public record LegacyImportPlan(
        String sourceRoot,
        List<String> plannedImports,
        List<String> deferredImports
) {

    public static LegacyImportPlan stageZeroDefault(String sourceRoot) {
        return new LegacyImportPlan(
                sourceRoot,
                List.of(
                        "beings",
                        "relationships",
                        "accepted-review-items",
                        "owner-profile-facts",
                        "sessions-and-leases-metadata",
                        "snapshots"
                ),
                List.of(
                        "openclaw-runtime-patches",
                        "live-log-scanning",
                        "generated-markdown-mirrors"
                )
        );
    }

    public List<LegacyImportCategory> supportedImportCategories() {
        return plannedImports.stream()
                .map(LegacyImportCategory::fromPlanKey)
                .toList();
    }

    public List<LegacyImportCategory> deferredImportCategories() {
        return deferredImports.stream()
                .map(LegacyImportCategory::fromPlanKey)
                .toList();
    }
}
