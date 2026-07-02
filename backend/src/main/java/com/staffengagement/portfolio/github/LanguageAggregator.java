package com.staffengagement.portfolio.github;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure function that aggregates language byte counts across multiple repositories.
 * Produces a sorted list of aggregated languages (descending by total bytes,
 * alphabetical ascending by name for ties).
 */
public final class LanguageAggregator {

    /**
     * Represents an aggregated language with total byte count across all repositories
     * and the number of repositories where the language was detected.
     *
     * @param name       the language name as returned by GitHub API
     * @param totalBytes total byte count summed across all repositories
     * @param repoCount  number of repositories containing this language
     */
    public record AggregatedLanguage(String name, long totalBytes, int repoCount) {}

    private LanguageAggregator() {
        // Utility class — prevent instantiation
    }

    /**
     * Sums byte counts per language across all repository language maps.
     * Order-independent (commutative, associative).
     *
     * @param repoLanguageMaps list of maps, each mapping language name to byte count for one repo
     * @return sorted list of aggregated languages (descending by totalBytes, alphabetical by name for ties)
     */
    public static List<AggregatedLanguage> aggregate(List<Map<String, Long>> repoLanguageMaps) {
        if (repoLanguageMaps == null || repoLanguageMaps.isEmpty()) {
            return List.of();
        }

        Map<String, long[]> accumulator = new LinkedHashMap<>();
        // accumulator value: [totalBytes, repoCount]

        for (Map<String, Long> repoMap : repoLanguageMaps) {
            if (repoMap == null) {
                continue;
            }
            for (Map.Entry<String, Long> entry : repoMap.entrySet()) {
                String lang = entry.getKey();
                long bytes = entry.getValue() != null ? entry.getValue() : 0L;
                accumulator.computeIfAbsent(lang, k -> new long[]{0L, 0L});
                accumulator.get(lang)[0] += bytes;
                accumulator.get(lang)[1] += 1;
            }
        }

        return accumulator.entrySet().stream()
                .map(e -> new AggregatedLanguage(e.getKey(), e.getValue()[0], (int) e.getValue()[1]))
                .sorted(Comparator
                        .comparingLong(AggregatedLanguage::totalBytes).reversed()
                        .thenComparing(AggregatedLanguage::name))
                .collect(Collectors.toList());
    }
}
