package com.staffengagement.portfolio.github;

import com.staffengagement.portfolio.github.LanguageSkillMapper.MappedSkill;

import java.util.*;

/**
 * Pure function that assigns proficiency levels based on rank position
 * determined by total byte count.
 *
 * Ranking rules:
 * - Rank 1 (top by bytes): EXPERT
 * - Ranks 2-3: ADVANCED
 * - Rank 4+: INTERMEDIATE
 * - Ties at rank boundaries get the higher proficiency.
 */
public final class ProficiencyRanker {

    private ProficiencyRanker() {
        // Utility class — prevent instantiation
    }

    /**
     * Assigns proficiency based on rank position within the skills list.
     * Skills are ranked by totalBytes descending. Ties at rank boundaries
     * receive the higher proficiency level.
     *
     * @param skills list of mapped skills (need not be pre-sorted)
     * @return map of skill name to assigned proficiency level; empty map if input is null or empty
     */
    public static Map<String, Proficiency> rank(List<MappedSkill> skills) {
        if (skills == null || skills.isEmpty()) {
            return Map.of();
        }

        // Sort by totalBytes descending
        List<MappedSkill> sorted = skills.stream()
                .sorted(Comparator.comparingLong(MappedSkill::totalBytes).reversed())
                .toList();

        Map<String, Proficiency> result = new LinkedHashMap<>();

        // Assign ranks with tie handling.
        // "Rank" is dense rank: ties share the same rank, next distinct value gets rank = position of first in that group.
        int rank = 1;
        for (int i = 0; i < sorted.size(); i++) {
            // Determine the rank for position i.
            // If this skill has same bytes as previous, it shares the same rank.
            if (i > 0 && sorted.get(i).totalBytes() == sorted.get(i - 1).totalBytes()) {
                // Same rank as previous — inherits same proficiency
            } else {
                // New rank = current position + 1
                rank = i + 1;
            }

            result.put(sorted.get(i).skillName(), proficiencyForRank(rank));
        }

        return result;
    }

    private static Proficiency proficiencyForRank(int rank) {
        if (rank == 1) {
            return Proficiency.EXPERT;
        } else if (rank <= 3) {
            return Proficiency.ADVANCED;
        } else {
            return Proficiency.INTERMEDIATE;
        }
    }
}
