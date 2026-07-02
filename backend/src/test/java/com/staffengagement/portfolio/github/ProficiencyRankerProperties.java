package com.staffengagement.portfolio.github;

import com.staffengagement.portfolio.github.LanguageSkillMapper.MappedSkill;
import net.jqwik.api.*;

import java.util.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link ProficiencyRanker}.
 */
class ProficiencyRankerProperties {

    // ========================================================================
    // Feature: github-profile-import, Property 10: Proficiency Ranking Invariant
    // ========================================================================

    /**
     * Property 10: Proficiency Ranking Invariant
     *
     * For any non-empty list of mapped skills sorted by bytes descending,
     * the top skill SHALL receive EXPERT, the 2nd and 3rd SHALL receive ADVANCED,
     * and all remaining SHALL receive INTERMEDIATE. Skills tied at a rank boundary
     * SHALL receive the higher proficiency.
     *
     * **Validates: Requirements 6.4**
     */
    @Property(tries = 100)
    void rankingInvariantDistinctBytes(@ForAll("distinctByteSkills") List<MappedSkill> skills) {
        Map<String, Proficiency> result = ProficiencyRanker.rank(skills);

        assertThat(result).hasSize(skills.size());

        // Sort by totalBytes descending to determine expected ranks
        List<MappedSkill> sorted = skills.stream()
                .sorted(Comparator.comparingLong(MappedSkill::totalBytes).reversed())
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            String skillName = sorted.get(i).skillName();
            Proficiency actual = result.get(skillName);
            int rank = i + 1; // All distinct, so rank = position

            if (rank == 1) {
                assertThat(actual)
                        .as("Rank 1 skill '%s' should be EXPERT", skillName)
                        .isEqualTo(Proficiency.EXPERT);
            } else if (rank <= 3) {
                assertThat(actual)
                        .as("Rank %d skill '%s' should be ADVANCED", rank, skillName)
                        .isEqualTo(Proficiency.ADVANCED);
            } else {
                assertThat(actual)
                        .as("Rank %d skill '%s' should be INTERMEDIATE", rank, skillName)
                        .isEqualTo(Proficiency.INTERMEDIATE);
            }
        }
    }

    /**
     * Property 10 (tie at rank 1 boundary): When multiple skills share the highest
     * byte count, ALL tied skills receive EXPERT.
     *
     * **Validates: Requirements 6.4**
     */
    @Property(tries = 100)
    void tiesAtRank1BoundaryAllGetExpert(@ForAll("skillsWithTiedTop") List<MappedSkill> skills) {
        Map<String, Proficiency> result = ProficiencyRanker.rank(skills);

        // Find max bytes
        long maxBytes = skills.stream().mapToLong(MappedSkill::totalBytes).max().orElse(0);

        // All skills with max bytes should be EXPERT
        skills.stream()
                .filter(s -> s.totalBytes() == maxBytes)
                .forEach(s -> assertThat(result.get(s.skillName()))
                        .as("Skill '%s' tied at rank 1 should be EXPERT", s.skillName())
                        .isEqualTo(Proficiency.EXPERT));
    }

    /**
     * Property 10 (tie at rank 3 boundary): When skills at position 3 share
     * bytes with other skills at the boundary, all tied skills get ADVANCED.
     *
     * **Validates: Requirements 6.4**
     */
    @Property(tries = 100)
    void tiesAtRank3BoundaryAllGetAdvanced(@ForAll("skillsWithTiedAtRank3") List<MappedSkill> skills) {
        Map<String, Proficiency> result = ProficiencyRanker.rank(skills);

        // Sort descending by bytes
        List<MappedSkill> sorted = skills.stream()
                .sorted(Comparator.comparingLong(MappedSkill::totalBytes).reversed())
                .toList();

        // The byte count at position index 2 (3rd skill) is the rank-3 boundary value
        long rank3Bytes = sorted.get(2).totalBytes();

        // All skills with that byte count that are not already rank 1
        // (i.e., they share the rank-3 boundary) should get ADVANCED
        long rank1Bytes = sorted.get(0).totalBytes();
        for (MappedSkill skill : skills) {
            if (skill.totalBytes() == rank3Bytes && rank3Bytes != rank1Bytes) {
                assertThat(result.get(skill.skillName()))
                        .as("Skill '%s' tied at rank 3 boundary should be ADVANCED", skill.skillName())
                        .isEqualTo(Proficiency.ADVANCED);
            }
        }
    }

    // ========================================================================
    // Providers
    // ========================================================================

    /**
     * Generates non-empty lists of MappedSkill with distinct totalBytes values
     * and distinct skill names.
     */
    @Provide
    Arbitrary<List<MappedSkill>> distinctByteSkills() {
        return Arbitraries.integers().between(1, 10)
                .flatMap(size -> {
                    // Generate 'size' distinct byte values and distinct names
                    Arbitrary<List<Long>> bytesArb = Arbitraries.longs()
                            .between(1L, 1_000_000_000L)
                            .list().ofSize(size)
                            .filter(list -> list.stream().distinct().count() == size);

                    Arbitrary<List<String>> namesArb = Arbitraries.strings()
                            .withCharRange('a', 'z')
                            .ofMinLength(3).ofMaxLength(12)
                            .list().ofSize(size)
                            .filter(list -> list.stream().distinct().count() == size);

                    return Combinators.combine(bytesArb, namesArb).as((bytes, names) ->
                            IntStream.range(0, size)
                                    .mapToObj(i -> new MappedSkill(names.get(i), bytes.get(i), i + 1))
                                    .toList()
                    );
                });
    }

    /**
     * Generates skill lists where at least 2 skills share the highest byte count (tied at rank 1).
     * Remaining skills have lower distinct byte counts.
     */
    @Provide
    Arbitrary<List<MappedSkill>> skillsWithTiedTop() {
        return Arbitraries.integers().between(2, 8)
                .flatMap(size -> {
                    // At least 2 skills tied at top
                    int tiedCount = Math.min(size, Math.max(2, size / 2));

                    Arbitrary<Long> topBytesArb = Arbitraries.longs().between(500_000L, 1_000_000_000L);
                    Arbitrary<List<Long>> lowerBytesArb = Arbitraries.longs()
                            .between(1L, 499_999L)
                            .list().ofSize(size - tiedCount)
                            .filter(list -> list.stream().distinct().count() == (size - tiedCount));

                    Arbitrary<List<String>> namesArb = Arbitraries.strings()
                            .withCharRange('a', 'z')
                            .ofMinLength(3).ofMaxLength(12)
                            .list().ofSize(size)
                            .filter(list -> list.stream().distinct().count() == size);

                    return Combinators.combine(topBytesArb, lowerBytesArb, namesArb)
                            .as((topBytes, lowerBytes, names) -> {
                                List<MappedSkill> skills = new ArrayList<>();
                                for (int i = 0; i < tiedCount; i++) {
                                    skills.add(new MappedSkill(names.get(i), topBytes, i + 1));
                                }
                                for (int i = 0; i < lowerBytes.size(); i++) {
                                    skills.add(new MappedSkill(names.get(tiedCount + i), lowerBytes.get(i), i + 1));
                                }
                                return skills;
                            });
                });
    }

    /**
     * Generates skill lists where at least 2 skills share the byte count at position 3
     * (tied at rank 3 boundary), with distinct values for rank 1.
     * Ensures at least 4 skills so there's a meaningful boundary.
     */
    @Provide
    Arbitrary<List<MappedSkill>> skillsWithTiedAtRank3() {
        return Arbitraries.integers().between(4, 8)
                .flatMap(size -> {
                    // rank1 has unique top bytes, rank2-3 and beyond share bytes at position 3
                    Arbitrary<Long> rank1BytesArb = Arbitraries.longs().between(900_000L, 1_000_000_000L);
                    Arbitrary<Long> rank3BytesArb = Arbitraries.longs().between(100_000L, 899_999L);
                    Arbitrary<List<Long>> lowerBytesArb = Arbitraries.longs()
                            .between(1L, 99_999L)
                            .list().ofSize(size - 3)
                            .filter(list -> list.stream().distinct().count() == (size - 3));

                    Arbitrary<List<String>> namesArb = Arbitraries.strings()
                            .withCharRange('a', 'z')
                            .ofMinLength(3).ofMaxLength(12)
                            .list().ofSize(size)
                            .filter(list -> list.stream().distinct().count() == size);

                    return Combinators.combine(rank1BytesArb, rank3BytesArb, lowerBytesArb, namesArb)
                            .as((rank1Bytes, rank3Bytes, lowerBytes, names) -> {
                                List<MappedSkill> skills = new ArrayList<>();
                                // Rank 1: unique top
                                skills.add(new MappedSkill(names.get(0), rank1Bytes, 3));
                                // Rank 2 and 3: tied at rank3Bytes
                                skills.add(new MappedSkill(names.get(1), rank3Bytes, 2));
                                skills.add(new MappedSkill(names.get(2), rank3Bytes, 1));
                                // Remaining: lower bytes
                                for (int i = 0; i < lowerBytes.size(); i++) {
                                    skills.add(new MappedSkill(names.get(3 + i), lowerBytes.get(i), i + 1));
                                }
                                return skills;
                            });
                });
    }
}
