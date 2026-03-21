package com.openclaw.digitalbeings.legacy.importer;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record LegacyImportDryRunReport(
        Path sourceRoot,
        LegacyImportPlan plan,
        List<LegacySourceDiscovery> plannedSourceDiscovery,
        LegacyImportCountSummary countSummary,
        List<String> warnings,
        List<String> anomalies,
        List<LegacyImportCategory> supportedImportCategories
) {

    public LegacyImportDryRunReport {
        Objects.requireNonNull(sourceRoot, "sourceRoot");
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(plannedSourceDiscovery, "plannedSourceDiscovery");
        Objects.requireNonNull(countSummary, "countSummary");
        Objects.requireNonNull(warnings, "warnings");
        Objects.requireNonNull(anomalies, "anomalies");
        Objects.requireNonNull(supportedImportCategories, "supportedImportCategories");
    }

    public String render() {
        StringBuilder builder = new StringBuilder();
        builder.append("sourceRoot=").append(sourceRoot).append(System.lineSeparator());
        builder.append("supportedImportCategories=").append(supportedImportCategories).append(System.lineSeparator());
        builder.append("plannedSourceDiscovery=").append(System.lineSeparator());
        for (LegacySourceDiscovery discovery : plannedSourceDiscovery) {
            builder.append("  - ")
                    .append(discovery.label())
                    .append(" path=")
                    .append(discovery.relativePath())
                    .append(" present=")
                    .append(discovery.present())
                    .append(" fileCount=")
                    .append(discovery.fileCount())
                    .append(" directoryCount=")
                    .append(discovery.directoryCount())
                    .append(" expectedCategories=")
                    .append(discovery.expectedCategories())
                    .append(System.lineSeparator());
        }
        builder.append("countSummary=").append(System.lineSeparator());
        builder.append("  discoveredFiles=").append(countSummary.discoveredFiles()).append(System.lineSeparator());
        builder.append("  discoveredDirectories=").append(countSummary.discoveredDirectories()).append(System.lineSeparator());
        builder.append("  pythonFiles=").append(countSummary.pythonFiles()).append(System.lineSeparator());
        builder.append("  jsonFiles=").append(countSummary.jsonFiles()).append(System.lineSeparator());
        builder.append("  jsonlFiles=").append(countSummary.jsonlFiles()).append(System.lineSeparator());
        builder.append("  yamlFiles=").append(countSummary.yamlFiles()).append(System.lineSeparator());
        builder.append("  markdownFiles=").append(countSummary.markdownFiles()).append(System.lineSeparator());
        builder.append("  otherFiles=").append(countSummary.otherFiles()).append(System.lineSeparator());
        builder.append("  filesByCategory=").append(countSummary.filesByCategory()).append(System.lineSeparator());
        builder.append("warnings=").append(warnings).append(System.lineSeparator());
        builder.append("anomalies=").append(anomalies).append(System.lineSeparator());
        return builder.toString();
    }
}
