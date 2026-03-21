package com.openclaw.digitalbeings.legacy.importer;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record LegacySourceDiscovery(
        String label,
        Path relativePath,
        boolean present,
        List<LegacyImportCategory> expectedCategories,
        long fileCount,
        long directoryCount,
        List<String> sampleEntries
) {

    public LegacySourceDiscovery {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(relativePath, "relativePath");
        Objects.requireNonNull(expectedCategories, "expectedCategories");
        Objects.requireNonNull(sampleEntries, "sampleEntries");
    }
}
