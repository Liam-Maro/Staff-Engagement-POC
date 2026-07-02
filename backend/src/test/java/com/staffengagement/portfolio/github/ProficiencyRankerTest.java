package com.staffengagement.portfolio.github;

import com.staffengagement.portfolio.github.LanguageSkillMapper.MappedSkill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProficiencyRankerTest {

    @Test
    void rank_nullInput_returnsEmptyMap() {
        Map<String, Proficiency> result = ProficiencyRanker.rank(null);
        assertThat(result).isEmpty();
    }

    @Test
    void rank_emptyList_returnsEmptyMap() {
        Map<String, Proficiency> result = ProficiencyRanker.rank(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void rank_singleSkill_getsExpert() {
        List<MappedSkill> skills = List.of(new MappedSkill("Java", 50000L, 5));

        Map<String, Proficiency> result = ProficiencyRanker.rank(skills);

        assertThat(result).hasSize(1);
        assertThat(result.get("Java")).isEqualTo(Proficiency.EXPERT);
    }

    @Test
    void rank_twoSkills_firstExpert_secondAdvanced() {
        List<MappedSkill> skills = List.of(
                new MappedSkill("Java", 50000L, 5),
                new MappedSkill("Python", 30000L, 3)
        );

        Map<String, Proficiency> result = ProficiencyRanker.rank(skills);

        assertThat(result.get("Java")).isEqualTo(Proficiency.EXPERT);
        assertThat(result.get("Python")).isEqualTo(Proficiency.ADVANCED);
    }

    @Test
    void rank_threeSkills_firstExpert_secondAndThirdAdvanced() {
        List<MappedSkill> skills = List.of(
                new MappedSkill("Java", 50000L, 5),
                new MappedSkill("Python", 30000L, 3),
                new MappedSkill("Go", 10000L, 2)
        );

        Map<String, Proficiency> result = ProficiencyRanker.rank(skills);

        assertThat(result.get("Java")).isEqualTo(Proficiency.EXPERT);
        assertThat(result.get("Python")).isEqualTo(Proficiency.ADVANCED);
        assertThat(result.get("Go")).isEqualTo(Proficiency.ADVANCED);
    }

    @Test
    void rank_fourSkills_fourthGetsIntermediate() {
        List<MappedSkill> skills = List.of(
                new MappedSkill("Java", 50000L, 5),
                new MappedSkill("Python", 30000L, 3),
                new MappedSkill("Go", 10000L, 2),
                new MappedSkill("Rust", 5000L, 1)
        );

        Map<String, Proficiency> result = ProficiencyRanker.rank(skills);

        assertThat(result.get("Java")).isEqualTo(Proficiency.EXPERT);
        assertThat(result.get("Python")).isEqualTo(Proficiency.ADVANCED);
        assertThat(result.get("Go")).isEqualTo(Proficiency.ADVANCED);
        assertThat(result.get("Rust")).isEqualTo(Proficiency.INTERMEDIATE);
    }

    @Test
    void rank_tiesAtRank1Boundary_allGetExpert() {
        // Two skills with same bytes as top → both get EXPERT
        List<MappedSkill> skills = List.of(
                new MappedSkill("Java", 50000L, 5),
                new MappedSkill("Python", 50000L, 3),
                new MappedSkill("Go", 10000L, 2)
        );

        Map<String, Proficiency> result = ProficiencyRanker.rank(skills);

        assertThat(result.get("Java")).isEqualTo(Proficiency.EXPERT);
        assertThat(result.get("Python")).isEqualTo(Proficiency.EXPERT);
        assertThat(result.get("Go")).isEqualTo(Proficiency.ADVANCED);
    }

    @Test
    void rank_tiesAtRank3Boundary_allGetAdvanced() {
        // Ranks 2 and 3 tied with same bytes → both ADVANCED, rank 4 tied with them also gets ADVANCED
        List<MappedSkill> skills = List.of(
                new MappedSkill("Java", 50000L, 5),
                new MappedSkill("Python", 30000L, 3),
                new MappedSkill("Go", 30000L, 2),
                new MappedSkill("Rust", 5000L, 1)
        );

        Map<String, Proficiency> result = ProficiencyRanker.rank(skills);

        assertThat(result.get("Java")).isEqualTo(Proficiency.EXPERT);
        assertThat(result.get("Python")).isEqualTo(Proficiency.ADVANCED);
        assertThat(result.get("Go")).isEqualTo(Proficiency.ADVANCED);
        assertThat(result.get("Rust")).isEqualTo(Proficiency.INTERMEDIATE);
    }

    @Test
    void rank_tiesAtRank3And4Boundary_tiedSkillsGetAdvanced() {
        // 3rd and 4th have same bytes → tie at rank 3 boundary → 4th gets ADVANCED (higher)
        List<MappedSkill> skills = List.of(
                new MappedSkill("Java", 50000L, 5),
                new MappedSkill("Python", 30000L, 3),
                new MappedSkill("Go", 10000L, 2),
                new MappedSkill("Rust", 10000L, 1),
                new MappedSkill("C", 5000L, 1)
        );

        Map<String, Proficiency> result = ProficiencyRanker.rank(skills);

        assertThat(result.get("Java")).isEqualTo(Proficiency.EXPERT);
        assertThat(result.get("Python")).isEqualTo(Proficiency.ADVANCED);
        assertThat(result.get("Go")).isEqualTo(Proficiency.ADVANCED);
        assertThat(result.get("Rust")).isEqualTo(Proficiency.ADVANCED);
        assertThat(result.get("C")).isEqualTo(Proficiency.INTERMEDIATE);
    }

    @Test
    void rank_unsortedInput_sortsCorrectlyBeforeRanking() {
        // Input not pre-sorted — ranker should sort internally
        List<MappedSkill> skills = List.of(
                new MappedSkill("Go", 10000L, 2),
                new MappedSkill("Java", 50000L, 5),
                new MappedSkill("Python", 30000L, 3)
        );

        Map<String, Proficiency> result = ProficiencyRanker.rank(skills);

        assertThat(result.get("Java")).isEqualTo(Proficiency.EXPERT);
        assertThat(result.get("Python")).isEqualTo(Proficiency.ADVANCED);
        assertThat(result.get("Go")).isEqualTo(Proficiency.ADVANCED);
    }

    @Test
    void rank_allTied_allGetExpert() {
        // All skills have same bytes → all tied at rank 1 → all EXPERT
        List<MappedSkill> skills = List.of(
                new MappedSkill("Java", 1000L, 1),
                new MappedSkill("Python", 1000L, 1),
                new MappedSkill("Go", 1000L, 1)
        );

        Map<String, Proficiency> result = ProficiencyRanker.rank(skills);

        assertThat(result.get("Java")).isEqualTo(Proficiency.EXPERT);
        assertThat(result.get("Python")).isEqualTo(Proficiency.EXPERT);
        assertThat(result.get("Go")).isEqualTo(Proficiency.EXPERT);
    }

    @Test
    void rank_manySkills_correctDistribution() {
        List<MappedSkill> skills = List.of(
                new MappedSkill("Java", 100000L, 10),
                new MappedSkill("Python", 80000L, 8),
                new MappedSkill("Go", 60000L, 6),
                new MappedSkill("Rust", 40000L, 4),
                new MappedSkill("C", 20000L, 2),
                new MappedSkill("Ruby", 10000L, 1)
        );

        Map<String, Proficiency> result = ProficiencyRanker.rank(skills);

        assertThat(result.get("Java")).isEqualTo(Proficiency.EXPERT);
        assertThat(result.get("Python")).isEqualTo(Proficiency.ADVANCED);
        assertThat(result.get("Go")).isEqualTo(Proficiency.ADVANCED);
        assertThat(result.get("Rust")).isEqualTo(Proficiency.INTERMEDIATE);
        assertThat(result.get("C")).isEqualTo(Proficiency.INTERMEDIATE);
        assertThat(result.get("Ruby")).isEqualTo(Proficiency.INTERMEDIATE);
    }
}
