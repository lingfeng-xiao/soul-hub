package com.openclaw.digitalbeings.application.governance;

import java.util.List;
import java.util.stream.Collectors;

public record OwnerProfileCompilationView(
        String beingId,
        int factCount,
        List<String> sections,
        List<String> lines,
        String compiledText
) {

    public static OwnerProfileCompilationView from(String beingId, List<OwnerProfileFactView> facts) {
        List<OwnerProfileFactView> sortedFacts = facts.stream()
                .sorted((left, right) -> {
                    int sectionComparison = left.section().compareTo(right.section());
                    if (sectionComparison != 0) {
                        return sectionComparison;
                    }
                    int keyComparison = left.key().compareTo(right.key());
                    if (keyComparison != 0) {
                        return keyComparison;
                    }
                    return left.factId().compareTo(right.factId());
                })
                .toList();
        List<String> sections = sortedFacts.stream()
                .map(OwnerProfileFactView::section)
                .distinct()
                .toList();
        List<String> lines = sortedFacts.stream()
                .map(fact -> fact.section() + "." + fact.key() + " = " + fact.summary())
                .toList();
        String compiledText = lines.isEmpty() ? "no-owner-profile-facts" : lines.stream().collect(Collectors.joining("\n"));
        return new OwnerProfileCompilationView(beingId, sortedFacts.size(), sections, lines, compiledText);
    }
}
