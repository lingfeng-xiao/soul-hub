package com.openclaw.digitalbeings.legacy.importer;

import java.util.Map;
import java.util.Objects;

public record LegacyImportCountSummary(
        long discoveredFiles,
        long discoveredDirectories,
        long pythonFiles,
        long jsonFiles,
        long jsonlFiles,
        long yamlFiles,
        long markdownFiles,
        long otherFiles,
        Map<LegacyImportCategory, Long> filesByCategory
) {

    public LegacyImportCountSummary {
        Objects.requireNonNull(filesByCategory, "filesByCategory");
    }
}
