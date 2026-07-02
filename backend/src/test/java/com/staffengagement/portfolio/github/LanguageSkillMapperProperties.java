package com.staffengagement.portfolio.github;

import com.staffengagement.portfolio.github.LanguageAggregator.AggregatedLanguage;
import com.staffengagement.portfolio.github.LanguageSkillMapper.MappedSkill;
import net.jqwik.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link LanguageSkillMapper}.
 */
class LanguageSkillMapperProperties {

    // ========================================================================
    // Feature: github-profile-import, Property 7: Case-Insensitive Mapping Lookup
    // ========================================================================

    /**
     * Property 7: Case-Insensitive Mapping Lookup
     *
     * For any language name and mapping configuration, looking up the language
     * in any casing (upper, lower, mixed) SHALL return the same mapped skill name.
     *
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 100)
    void caseInsensitiveMappingLookup(
            @ForAll("languageNamesWithMapping") LanguageWithMapping input) {

        String skillTarget = "TargetSkill_" + input.languageName();
        Map<String, String> mappingConfig = Map.of(input.languageName(), skillTarget);

        // Create AggregatedLanguage entries with various casings of the same name
        List<AggregatedLanguage> lowerCase = List.of(
                new AggregatedLanguage(input.languageName().toLowerCase(Locale.ROOT), 1000L, 1));
        List<AggregatedLanguage> upperCase = List.of(
                new AggregatedLanguage(input.languageName().toUpperCase(Locale.ROOT), 1000L, 1));
        List<AggregatedLanguage> originalCase = List.of(
                new AggregatedLanguage(input.languageName(), 1000L, 1));
        List<AggregatedLanguage> mixedCase = List.of(
                new AggregatedLanguage(input.mixedCasing(), 1000L, 1));

        List<MappedSkill> resultLower = LanguageSkillMapper.map(lowerCase, mappingConfig);
        List<MappedSkill> resultUpper = LanguageSkillMapper.map(upperCase, mappingConfig);
        List<MappedSkill> resultOriginal = LanguageSkillMapper.map(originalCase, mappingConfig);
        List<MappedSkill> resultMixed = LanguageSkillMapper.map(mixedCase, mappingConfig);

        // All casings should produce the same mapped skill name
        assertThat(resultLower).hasSize(1);
        assertThat(resultUpper).hasSize(1);
        assertThat(resultOriginal).hasSize(1);
        assertThat(resultMixed).hasSize(1);

        assertThat(resultLower.get(0).skillName()).isEqualTo(skillTarget);
        assertThat(resultUpper.get(0).skillName()).isEqualTo(skillTarget);
        assertThat(resultOriginal.get(0).skillName()).isEqualTo(skillTarget);
        assertThat(resultMixed.get(0).skillName()).isEqualTo(skillTarget);
    }

    /**
     * Case-insensitive lookup works with realistic language names from a known set,
     * tested with random casing transformations.
     *
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 100)
    void caseInsensitiveLookupWithRealisticLanguages(
            @ForAll("realisticLanguageWithCasing") RealisticLanguageCase input) {

        Map<String, String> mappingConfig = Map.of(input.originalName(), input.mappedSkill());

        List<AggregatedLanguage> withOriginalCase = List.of(
                new AggregatedLanguage(input.originalName(), 5000L, 3));
        List<AggregatedLanguage> withRandomCase = List.of(
                new AggregatedLanguage(input.randomCasing(), 5000L, 3));

        List<MappedSkill> resultOriginal = LanguageSkillMapper.map(withOriginalCase, mappingConfig);
        List<MappedSkill> resultRandom = LanguageSkillMapper.map(withRandomCase, mappingConfig);

        assertThat(resultOriginal).hasSize(1);
        assertThat(resultRandom).hasSize(1);
        assertThat(resultOriginal.get(0).skillName()).isEqualTo(resultRandom.get(0).skillName());
        assertThat(resultOriginal.get(0).skillName()).isEqualTo(input.mappedSkill());
    }

    // ========================================================================
    // Feature: github-profile-import, Property 8: Unmapped Languages Preserved Exactly
    // ========================================================================

    /**
     * For any language name that has no entry in the mapping configuration,
     * the mapped skill name SHALL be identical to the original GitHub language
     * name (preserving casing).
     *
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 100)
    void unmappedLanguagesPreservedExactly(
            @ForAll("unmappedLanguageInputs") UnmappedTestCase testCase) {

        List<MappedSkill> result = LanguageSkillMapper.map(testCase.languages(), testCase.mappingConfig());

        // Every input language should appear in the output with identical name
        for (AggregatedLanguage lang : testCase.languages()) {
            Optional<MappedSkill> matched = result.stream()
                    .filter(s -> s.skillName().equals(lang.name()))
                    .findFirst();

            assertThat(matched)
                    .as("Language '%s' should pass through with identical name", lang.name())
                    .isPresent();

            assertThat(matched.get().skillName())
                    .as("Skill name must be identical to input language name (exact casing)")
                    .isEqualTo(lang.name());
        }
    }

    /**
     * With an empty mapping config, all languages pass through with exact name preserved.
     *
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 100)
    void emptyMappingPreservesAllLanguageNames(
            @ForAll("unmappedLanguages") List<AggregatedLanguage> languages) {

        List<MappedSkill> result = LanguageSkillMapper.map(languages, Map.of());

        for (AggregatedLanguage lang : languages) {
            Optional<MappedSkill> matched = result.stream()
                    .filter(s -> s.skillName().equals(lang.name()))
                    .findFirst();

            assertThat(matched)
                    .as("Language '%s' should appear in output with exact name when mapping is empty", lang.name())
                    .isPresent();
        }
    }

    // ========================================================================
    // Generators
    // ========================================================================

    record UnmappedTestCase(List<AggregatedLanguage> languages, Map<String, String> mappingConfig) {}

    @Provide
    Arbitrary<UnmappedTestCase> unmappedLanguageInputs() {
        // Generate language names that are guaranteed to NOT be in the mapping config
        // Strategy: mapping config uses a fixed set of keys, language names are drawn
        // from a completely different set
        Arbitrary<Map<String, String>> configs = mappingConfigs();
        Arbitrary<List<AggregatedLanguage>> langs = languagesNotInMapping();

        return Combinators.combine(langs, configs).as(UnmappedTestCase::new);
    }

    @Provide
    Arbitrary<List<AggregatedLanguage>> unmappedLanguages() {
        return languagesNotInMapping();
    }

    /**
     * Generates language names that will never match keys in our mapping configs.
     * The mapping configs use keys from: Java, Python, JavaScript, TypeScript, Shell, C++.
     * These generated names are from a completely disjoint set.
     */
    private Arbitrary<List<AggregatedLanguage>> languagesNotInMapping() {
        // Names that will never appear as keys in mapping config
        Arbitrary<String> unmappedNames = Arbitraries.of(
                "Haskell", "Elixir", "Erlang", "Fortran", "COBOL",
                "Lua", "Nim", "Zig", "OCaml", "F#",
                "Assembly", "Prolog", "Lisp", "Ada", "Pascal"
        );
        Arbitrary<Long> bytes = Arbitraries.longs().between(1L, 500_000L);
        Arbitrary<Integer> repos = Arbitraries.integers().between(1, 20);

        return Combinators.combine(unmappedNames, bytes, repos)
                .as(AggregatedLanguage::new)
                .list()
                .ofMinSize(1)
                .ofMaxSize(8)
                .filter(list -> list.stream().map(AggregatedLanguage::name).distinct().count() == list.size());
    }

    /**
     * Generates mapping configs that use a fixed set of keys disjoint from unmapped names.
     */
    private Arbitrary<Map<String, String>> mappingConfigs() {
        return Arbitraries.of(
                Map.of("Java", "Java", "Python", "Python"),
                Map.of("JavaScript", "JS", "TypeScript", "JS"),
                Map.of("Shell", "Bash", "C++", "CPP"),
                Map.of("Java", "Java", "Python", "Python", "JavaScript", "JS"),
                Map.of() // empty config — still unmapped
        );
    }

    // ========================================================================
    // Feature: github-profile-import, Property 9: Many-to-One Mapping Combines Counts
    // ========================================================================

    /**
     * For any set of languages that map to the same skill name, the resulting
     * mapped skill SHALL have a total byte count equal to the sum of all source
     * languages' byte counts, and a repo count equal to the sum of all source
     * languages' repo counts.
     *
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 100)
    void manyToOneMappingCombinesBytesAndRepoCounts(
            @ForAll("multipleLanguagesMappingToSameSkill") ManyToOneTestCase testCase
    ) {
        List<MappedSkill> result = LanguageSkillMapper.map(testCase.languages(), testCase.mappingConfig());

        // Find the combined skill in the result
        Optional<MappedSkill> combinedSkill = result.stream()
                .filter(s -> s.skillName().equals(testCase.targetSkillName()))
                .findFirst();

        assertThat(combinedSkill)
                .as("A single MappedSkill for target '%s' should exist", testCase.targetSkillName())
                .isPresent();
        assertThat(combinedSkill.get().totalBytes())
                .as("Combined totalBytes should equal sum of all source languages' bytes")
                .isEqualTo(testCase.expectedTotalBytes());
        assertThat(combinedSkill.get().repoCount())
                .as("Combined repoCount should equal sum of all source languages' repo counts")
                .isEqualTo(testCase.expectedTotalRepos());
    }

    /**
     * When N languages all map to the same skill, result contains exactly one
     * MappedSkill entry for that target skill name (no duplicates).
     *
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 100)
    void manyToOneMappingProducesSingleEntry(
            @ForAll("multipleLanguagesMappingToSameSkill") ManyToOneTestCase testCase
    ) {
        List<MappedSkill> result = LanguageSkillMapper.map(testCase.languages(), testCase.mappingConfig());

        long count = result.stream()
                .filter(s -> s.skillName().equals(testCase.targetSkillName()))
                .count();

        assertThat(count)
                .as("Exactly one MappedSkill entry should exist for target '%s'", testCase.targetSkillName())
                .isEqualTo(1);
    }

    // ========================================================================
    // Test Case Records
    // ========================================================================

    record ManyToOneTestCase(
            List<AggregatedLanguage> languages,
            Map<String, String> mappingConfig,
            String targetSkillName,
            long expectedTotalBytes,
            int expectedTotalRepos
    ) {}

    // ========================================================================
    // Generators for Property 9: Many-to-One Mapping Combines Counts
    // ========================================================================

    @Provide
    Arbitrary<ManyToOneTestCase> multipleLanguagesMappingToSameSkill() {
        // Pool of distinct source language names that will all map to same target
        List<String> sourcePool = List.of(
                "Jupyter Notebook", "Python", "Cython",
                "TypeScript", "JavaScript", "CoffeeScript",
                "C", "C++", "Objective-C",
                "Shell", "Bash", "PowerShell",
                "Kotlin", "Java", "Groovy"
        );

        // Target skill names
        Arbitrary<String> targetSkills = Arbitraries.of(
                "Python", "JavaScript", "C-Family", "Shell", "JVM");

        // Number of source languages (2 to 5)
        Arbitrary<Integer> languageCount = Arbitraries.integers().between(2, 5);

        return Combinators.combine(targetSkills, languageCount)
                .flatAs((targetSkill, count) -> {
                    // Pick `count` distinct source language names from pool
                    Arbitrary<List<String>> sourceNames = Arbitraries.shuffle(sourcePool)
                            .map(shuffled -> shuffled.subList(0, Math.min(count, shuffled.size())));

                    // Generate byte counts and repo counts for each source
                    Arbitrary<List<Long>> bytesList = Arbitraries.longs()
                            .between(1L, 500_000L)
                            .list().ofSize(count);
                    Arbitrary<List<Integer>> reposList = Arbitraries.integers()
                            .between(1, 50)
                            .list().ofSize(count);

                    return Combinators.combine(sourceNames, bytesList, reposList)
                            .as((names, bytes, repos) -> {
                                int actualCount = Math.min(names.size(),
                                        Math.min(bytes.size(), repos.size()));

                                // Build languages list and mapping config
                                List<AggregatedLanguage> languages = new ArrayList<>();
                                Map<String, String> mappingConfig = new HashMap<>();
                                long expectedBytes = 0L;
                                int expectedRepos = 0;

                                for (int i = 0; i < actualCount; i++) {
                                    long b = bytes.get(i);
                                    int r = repos.get(i);
                                    languages.add(new AggregatedLanguage(names.get(i), b, r));
                                    mappingConfig.put(names.get(i), targetSkill);
                                    expectedBytes += b;
                                    expectedRepos += r;
                                }

                                return new ManyToOneTestCase(
                                        languages, mappingConfig, targetSkill,
                                        expectedBytes, expectedRepos
                                );
                            });
                });
    }

    // ========================================================================
    // Generators for Property 7: Case-Insensitive Mapping Lookup
    // ========================================================================

    record LanguageWithMapping(String languageName, String mixedCasing) {}

    record RealisticLanguageCase(String originalName, String mappedSkill, String randomCasing) {}

    @Provide
    Arbitrary<LanguageWithMapping> languageNamesWithMapping() {
        // Generate language names (alphabetic, 2-20 chars) and a mixed-case variant
        Arbitrary<String> languageNames = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .ofMinLength(2)
                .ofMaxLength(20)
                .filter(s -> !s.isBlank());

        return languageNames.map(name -> {
            String mixed = applyMixedCase(name);
            return new LanguageWithMapping(name, mixed);
        });
    }

    @Provide
    Arbitrary<RealisticLanguageCase> realisticLanguageWithCasing() {
        record LangMapping(String lang, String skill) {}
        Arbitrary<LangMapping> mappings = Arbitraries.of(
                new LangMapping("JavaScript", "JavaScript"),
                new LangMapping("TypeScript", "TypeScript"),
                new LangMapping("Python", "Python"),
                new LangMapping("Jupyter Notebook", "Python"),
                new LangMapping("Shell", "Bash"),
                new LangMapping("Java", "Java"),
                new LangMapping("Kotlin", "Kotlin"),
                new LangMapping("Ruby", "Ruby"),
                new LangMapping("Go", "Go"),
                new LangMapping("Rust", "Rust"),
                new LangMapping("Haskell", "Haskell")
        );

        return mappings.map(m -> {
            String randomCasing = applyMixedCase(m.lang());
            return new RealisticLanguageCase(m.lang(), m.skill(), randomCasing);
        });
    }

    /**
     * Applies alternating upper/lower casing to produce a mixed-case variant.
     */
    private static String applyMixedCase(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i % 2 == 0) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
