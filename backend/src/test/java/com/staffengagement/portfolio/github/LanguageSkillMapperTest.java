package com.staffengagement.portfolio.github;

import com.staffengagement.portfolio.github.LanguageAggregator.AggregatedLanguage;
import com.staffengagement.portfolio.github.LanguageSkillMapper.MappedSkill;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageSkillMapperTest {

    @Test
    void map_nullLanguages_returnsEmptyList() {
        List<MappedSkill> result = LanguageSkillMapper.map(null, Map.of("Java", "Java"));
        assertThat(result).isEmpty();
    }

    @Test
    void map_emptyLanguages_returnsEmptyList() {
        List<MappedSkill> result = LanguageSkillMapper.map(List.of(), Map.of("Java", "Java"));
        assertThat(result).isEmpty();
    }

    @Test
    void map_nullMappingConfig_allLanguagesPassThrough() {
        List<AggregatedLanguage> languages = List.of(
                new AggregatedLanguage("Java", 50000L, 3),
                new AggregatedLanguage("Python", 20000L, 2)
        );

        List<MappedSkill> result = LanguageSkillMapper.map(languages, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(new MappedSkill("Java", 50000L, 3));
        assertThat(result.get(1)).isEqualTo(new MappedSkill("Python", 20000L, 2));
    }

    @Test
    void map_emptyMappingConfig_allLanguagesPassThrough() {
        List<AggregatedLanguage> languages = List.of(
                new AggregatedLanguage("Java", 50000L, 3)
        );

        List<MappedSkill> result = LanguageSkillMapper.map(languages, Collections.emptyMap());

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(new MappedSkill("Java", 50000L, 3));
    }

    @Test
    void map_caseInsensitiveLookup_mapsRegardlessOfCase() {
        List<AggregatedLanguage> languages = List.of(
                new AggregatedLanguage("javascript", 30000L, 2)
        );
        Map<String, String> config = Map.of("JavaScript", "JS");

        List<MappedSkill> result = LanguageSkillMapper.map(languages, config);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).skillName()).isEqualTo("JS");
    }

    @Test
    void map_unmappedLanguage_preservesOriginalCasing() {
        List<AggregatedLanguage> languages = List.of(
                new AggregatedLanguage("Haskell", 15000L, 1)
        );
        Map<String, String> config = Map.of("Java", "Java");

        List<MappedSkill> result = LanguageSkillMapper.map(languages, config);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).skillName()).isEqualTo("Haskell");
        assertThat(result.get(0).totalBytes()).isEqualTo(15000L);
        assertThat(result.get(0).repoCount()).isEqualTo(1);
    }

    @Test
    void map_manyToOne_combinesBytesAndRepoCounts() {
        List<AggregatedLanguage> languages = List.of(
                new AggregatedLanguage("Python", 40000L, 5),
                new AggregatedLanguage("Jupyter Notebook", 10000L, 2)
        );
        Map<String, String> config = Map.of(
                "Python", "Python",
                "Jupyter Notebook", "Python"
        );

        List<MappedSkill> result = LanguageSkillMapper.map(languages, config);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(new MappedSkill("Python", 50000L, 7));
    }

    @Test
    void map_manyToOneCaseInsensitive_combinesCorrectly() {
        List<AggregatedLanguage> languages = List.of(
                new AggregatedLanguage("PYTHON", 20000L, 3),
                new AggregatedLanguage("jupyter notebook", 5000L, 1)
        );
        Map<String, String> config = Map.of(
                "Python", "Python",
                "Jupyter Notebook", "Python"
        );

        List<MappedSkill> result = LanguageSkillMapper.map(languages, config);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).skillName()).isEqualTo("Python");
        assertThat(result.get(0).totalBytes()).isEqualTo(25000L);
        assertThat(result.get(0).repoCount()).isEqualTo(4);
    }

    @Test
    void map_mixedMappedAndUnmapped_handledCorrectly() {
        List<AggregatedLanguage> languages = List.of(
                new AggregatedLanguage("Java", 50000L, 4),
                new AggregatedLanguage("Shell", 10000L, 2),
                new AggregatedLanguage("Go", 30000L, 3)
        );
        Map<String, String> config = Map.of("Shell", "Bash");

        List<MappedSkill> result = LanguageSkillMapper.map(languages, config);

        assertThat(result).hasSize(3);
        assertThat(result).contains(
                new MappedSkill("Java", 50000L, 4),
                new MappedSkill("Bash", 10000L, 2),
                new MappedSkill("Go", 30000L, 3)
        );
    }

    @Test
    void map_multipleMappingsToSameSkill_sumsAll() {
        List<AggregatedLanguage> languages = List.of(
                new AggregatedLanguage("TypeScript", 30000L, 4),
                new AggregatedLanguage("JavaScript", 25000L, 5),
                new AggregatedLanguage("CoffeeScript", 5000L, 1)
        );
        Map<String, String> config = new HashMap<>();
        config.put("TypeScript", "JavaScript");
        config.put("JavaScript", "JavaScript");
        config.put("CoffeeScript", "JavaScript");

        List<MappedSkill> result = LanguageSkillMapper.map(languages, config);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(new MappedSkill("JavaScript", 60000L, 10));
    }
}
