package com.staffengagement.portfolio.github;

import com.staffengagement.portfolio.github.LanguageAggregator.AggregatedLanguage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure function that applies configurable language-to-skill mapping.
 * <p>
 * Features:
 * <ul>
 *   <li>Case-insensitive lookup in mapping config</li>
 *   <li>Many-to-one mapping support (combines byte counts and repo counts)</li>
 *   <li>Unmapped languages pass through with original casing</li>
 *   <li>Null/empty mapping config treated as no-op (all pass through)</li>
 * </ul>
 */
public final class LanguageSkillMapper {

    /**
     * Represents a language mapped to a skill name with combined metrics.
     *
     * @param skillName  the resolved skill name (from mapping or original language name)
     * @param totalBytes combined total byte count for all source languages mapping to this skill
     * @param repoCount  combined repo count for all source languages mapping to this skill
     */
    public record MappedSkill(String skillName, long totalBytes, int repoCount) {}

    private LanguageSkillMapper() {
        // Utility class — prevent instantiation
    }

    /**
     * Applies configurable language-to-skill mapping on aggregated languages.
     * <p>
     * If mappingConfig is null or empty, all languages pass through as-is.
     * Mapping lookup is case-insensitive against the language name.
     * Multiple source languages mapping to the same skill name have their
     * byte counts and repo counts summed.
     *
     * @param languages     aggregated languages from {@link LanguageAggregator#aggregate}
     * @param mappingConfig map of language name (key, case-insensitive) to skill name (value)
     * @return list of mapped skills with combined metrics
     */
    public static List<MappedSkill> map(List<AggregatedLanguage> languages, Map<String, String> mappingConfig) {
        if (languages == null || languages.isEmpty()) {
            return List.of();
        }

        // Build case-insensitive lookup from mapping config
        Map<String, String> normalizedConfig = buildCaseInsensitiveLookup(mappingConfig);

        // Accumulate by resolved skill name (preserving insertion order for determinism)
        Map<String, long[]> accumulator = new LinkedHashMap<>();
        // value: [totalBytes, repoCount]

        for (AggregatedLanguage lang : languages) {
            String skillName = resolveSkillName(lang.name(), normalizedConfig);
            accumulator.computeIfAbsent(skillName, k -> new long[]{0L, 0});
            long[] stats = accumulator.get(skillName);
            stats[0] += lang.totalBytes();
            stats[1] += lang.repoCount();
        }

        return accumulator.entrySet().stream()
                .map(e -> new MappedSkill(e.getKey(), e.getValue()[0], (int) e.getValue()[1]))
                .collect(Collectors.toList());
    }

    /**
     * Builds a case-insensitive lookup map (lowercase key → mapped skill name).
     */
    private static Map<String, String> buildCaseInsensitiveLookup(Map<String, String> mappingConfig) {
        if (mappingConfig == null || mappingConfig.isEmpty()) {
            return Map.of();
        }

        Map<String, String> normalized = new HashMap<>();
        for (Map.Entry<String, String> entry : mappingConfig.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                normalized.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
            }
        }
        return normalized;
    }

    /**
     * Resolves the skill name for a given language name.
     * Uses case-insensitive lookup in the normalized config.
     * Falls back to original language name if no mapping exists.
     */
    private static String resolveSkillName(String languageName, Map<String, String> normalizedConfig) {
        if (normalizedConfig.isEmpty() || languageName == null) {
            return languageName;
        }

        String mapped = normalizedConfig.get(languageName.toLowerCase(Locale.ROOT));
        return mapped != null ? mapped : languageName;
    }
}
