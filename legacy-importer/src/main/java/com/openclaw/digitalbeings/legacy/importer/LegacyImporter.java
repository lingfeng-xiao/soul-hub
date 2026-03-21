package com.openclaw.digitalbeings.legacy.importer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class LegacyImporter {

    private static final List<PlannedSource> PLANNED_SOURCES = List.of(
            new PlannedSource(
                    "repository-root",
                    Path.of(""),
                    List.of(
                            LegacyImportCategory.BEINGS,
                            LegacyImportCategory.RELATIONSHIPS,
                            LegacyImportCategory.ACCEPTED_REVIEW_ITEMS,
                            LegacyImportCategory.OWNER_PROFILE_FACTS,
                            LegacyImportCategory.SESSIONS_AND_LEASES_METADATA,
                            LegacyImportCategory.SNAPSHOTS
                    )
            ),
            new PlannedSource(
                    "agents",
                    Path.of("agents"),
                    List.of(
                            LegacyImportCategory.OWNER_PROFILE_FACTS,
                            LegacyImportCategory.SESSIONS_AND_LEASES_METADATA,
                            LegacyImportCategory.ACCEPTED_REVIEW_ITEMS
                    )
            ),
            new PlannedSource(
                    "bridge",
                    Path.of("bridge"),
                    List.of(
                            LegacyImportCategory.RELATIONSHIPS,
                            LegacyImportCategory.ACCEPTED_REVIEW_ITEMS,
                            LegacyImportCategory.SESSIONS_AND_LEASES_METADATA,
                            LegacyImportCategory.SNAPSHOTS
                    )
            ),
            new PlannedSource(
                    "memory",
                    Path.of("memory"),
                    List.of(LegacyImportCategory.OWNER_PROFILE_FACTS)
            ),
            new PlannedSource(
                    "browser",
                    Path.of("browser"),
                    List.of(LegacyImportCategory.ACCEPTED_REVIEW_ITEMS)
            )
    );

    public LegacyImportDryRunReport dryRun(String sourceRoot) {
        return dryRun(Paths.get(sourceRoot));
    }

    public LegacyImportDryRunReport dryRun(Path sourceRoot) {
        Objects.requireNonNull(sourceRoot, "sourceRoot");
        Path normalizedRoot = sourceRoot.toAbsolutePath().normalize();
        LegacyImportPlan plan = LegacyImportPlan.stageZeroDefault(normalizedRoot.toString());

        List<String> warnings = new ArrayList<>();
        List<String> anomalies = new ArrayList<>();
        if (!Files.exists(normalizedRoot)) {
            anomalies.add("Source root does not exist: " + normalizedRoot);
            return new LegacyImportDryRunReport(
                    normalizedRoot,
                    plan,
                    buildDiscoveries(normalizedRoot, warnings, anomalies),
                    emptySummary(),
                    List.copyOf(warnings),
                    List.copyOf(anomalies),
                    plan.supportedImportCategories()
            );
        }
        if (!Files.isDirectory(normalizedRoot)) {
            anomalies.add("Source root is not a directory: " + normalizedRoot);
            return new LegacyImportDryRunReport(
                    normalizedRoot,
                    plan,
                    buildDiscoveries(normalizedRoot, warnings, anomalies),
                    emptySummary(),
                    List.copyOf(warnings),
                    List.copyOf(anomalies),
                    plan.supportedImportCategories()
            );
        }

        List<LegacySourceDiscovery> discoveries = buildDiscoveries(normalizedRoot, warnings, anomalies);
        ScanTally tally = scan(normalizedRoot, plan, warnings, anomalies);

        if (tally.discoveredFiles == 0L) {
            warnings.add("No importable artifacts were discovered under " + normalizedRoot);
        }

        return new LegacyImportDryRunReport(
                normalizedRoot,
                plan,
                discoveries,
                new LegacyImportCountSummary(
                        tally.discoveredFiles,
                        tally.discoveredDirectories,
                        tally.pythonFiles,
                        tally.jsonFiles,
                        tally.jsonlFiles,
                        tally.yamlFiles,
                        tally.markdownFiles,
                        tally.otherFiles,
                        Map.copyOf(tally.filesByCategory)
                ),
                List.copyOf(warnings),
                List.copyOf(anomalies),
                plan.supportedImportCategories()
        );
    }

    private static List<LegacySourceDiscovery> buildDiscoveries(Path normalizedRoot, List<String> warnings, List<String> anomalies) {
        List<LegacySourceDiscovery> discoveries = new ArrayList<>();
        for (PlannedSource source : PLANNED_SOURCES) {
            Path absolutePath = source.path().getNameCount() == 0 ? normalizedRoot : normalizedRoot.resolve(source.path());
            boolean present = Files.exists(absolutePath);
            long fileCount = present ? countFiles(absolutePath) : 0L;
            long directoryCount = present ? countDirectories(absolutePath) : 0L;
            List<String> sampleEntries = present ? sampleEntries(absolutePath) : List.of();
            if (!present && source.path().getNameCount() > 0) {
                warnings.add("Planned source path missing: " + normalizedRoot.relativize(absolutePath));
            }
            if (present && fileCount == 0L && source.path().getNameCount() > 0) {
                warnings.add("Planned source path is empty: " + normalizedRoot.relativize(absolutePath));
            }
            discoveries.add(new LegacySourceDiscovery(
                    source.label(),
                    source.path(),
                    present,
                    source.expectedCategories(),
                    fileCount,
                    directoryCount,
                    sampleEntries
            ));
        }
        if (discoveries.stream().noneMatch(LegacySourceDiscovery::present)) {
            anomalies.add("No planned source paths were present under " + normalizedRoot);
        }
        return discoveries;
    }

    private static ScanTally scan(Path normalizedRoot, LegacyImportPlan plan, List<String> warnings, List<String> anomalies) {
        ScanTally tally = new ScanTally();
        Map<LegacyImportCategory, Long> filesByCategory = tally.filesByCategory;
        for (LegacyImportCategory category : plan.supportedImportCategories()) {
            filesByCategory.put(category, 0L);
        }

        try (Stream<Path> stream = Files.walk(normalizedRoot)) {
            stream.filter(path -> !path.equals(normalizedRoot))
                    .filter(path -> !shouldSkip(path))
                    .sorted(Comparator.naturalOrder())
                    .forEach(path -> {
                        if (Files.isDirectory(path)) {
                            tally.discoveredDirectories++;
                            return;
                        }
                        tally.discoveredFiles++;
                        String fileName = path.getFileName().toString().toLowerCase();
                        if (fileName.contains(".deleted.")) {
                            anomalies.add("Deleted artifact marker found: " + normalizedRoot.relativize(path));
                        }
                        LegacyImportCategory category = inferCategory(normalizedRoot, path, plan);
                        if (category != null) {
                            filesByCategory.merge(category, 1L, Long::sum);
                        }
                        if (fileName.endsWith(".py")) {
                            tally.pythonFiles++;
                        } else if (fileName.endsWith(".jsonl")) {
                            tally.jsonlFiles++;
                        } else if (fileName.endsWith(".json")) {
                            tally.jsonFiles++;
                        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                            tally.yamlFiles++;
                        } else if (fileName.endsWith(".md")) {
                            tally.markdownFiles++;
                        } else {
                            tally.otherFiles++;
                        }
                    });
        } catch (Exception exception) {
            anomalies.add("Failed to scan source root: " + exception.getMessage());
        }
        return tally;
    }

    private static boolean shouldSkip(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        return fileName.equals(".git")
                || fileName.equals(".gradle")
                || fileName.equals("build")
                || fileName.equals("out")
                || fileName.equals("target")
                || fileName.equals("dist")
                || fileName.equals("node_modules")
                || fileName.equals("__pycache__")
                || fileName.equals(".pytest_cache");
    }

    private static LegacyImportCategory inferCategory(Path normalizedRoot, Path path, LegacyImportPlan plan) {
        Path relative = normalizedRoot.relativize(path);
        String normalized = relative.toString().replace('\\', '/').toLowerCase();
        if (normalized.contains("/agents/") || normalized.startsWith("agents/")) {
            return LegacyImportCategory.OWNER_PROFILE_FACTS;
        }
        if (normalized.contains("/bridge/") || normalized.startsWith("bridge/")) {
            return LegacyImportCategory.RELATIONSHIPS;
        }
        if (normalized.contains("/memory/") || normalized.startsWith("memory/")) {
            return LegacyImportCategory.OWNER_PROFILE_FACTS;
        }
        if (normalized.contains("/browser/") || normalized.startsWith("browser/")) {
            return LegacyImportCategory.ACCEPTED_REVIEW_ITEMS;
        }
        if (plan.sourceRoot() != null && normalized.isEmpty()) {
            return LegacyImportCategory.BEINGS;
        }
        if (normalized.endsWith(".jsonl")) {
            return LegacyImportCategory.SESSIONS_AND_LEASES_METADATA;
        }
        if (normalized.endsWith(".json")) {
            return LegacyImportCategory.ACCEPTED_REVIEW_ITEMS;
        }
        if (normalized.endsWith(".yaml") || normalized.endsWith(".yml")) {
            return LegacyImportCategory.SNAPSHOTS;
        }
        return null;
    }

    private static long countFiles(Path path) {
        try (Stream<Path> stream = Files.walk(path)) {
            return stream.filter(Files::isRegularFile)
                    .filter(candidate -> !shouldSkip(candidate))
                    .count();
        } catch (Exception exception) {
            return 0L;
        }
    }

    private static long countDirectories(Path path) {
        try (Stream<Path> stream = Files.walk(path)) {
            return stream.filter(Files::isDirectory)
                    .filter(candidate -> !candidate.equals(path))
                    .filter(candidate -> !shouldSkip(candidate))
                    .count();
        } catch (Exception exception) {
            return 0L;
        }
    }

    private static List<String> sampleEntries(Path path) {
        try (Stream<Path> stream = Files.walk(path)) {
            return stream.filter(Files::isRegularFile)
                    .filter(candidate -> !shouldSkip(candidate))
                    .map(path::relativize)
                    .map(Path::toString)
                    .sorted()
                    .limit(5)
                    .toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private static LegacyImportCountSummary emptySummary() {
        return new LegacyImportCountSummary(
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                Map.of()
        );
    }

    private record PlannedSource(String label, Path path, List<LegacyImportCategory> expectedCategories) {
    }

    private static final class ScanTally {
        long discoveredFiles;
        long discoveredDirectories;
        long pythonFiles;
        long jsonFiles;
        long jsonlFiles;
        long yamlFiles;
        long markdownFiles;
        long otherFiles;
        final Map<LegacyImportCategory, Long> filesByCategory = new EnumMap<>(LegacyImportCategory.class);
    }
}
