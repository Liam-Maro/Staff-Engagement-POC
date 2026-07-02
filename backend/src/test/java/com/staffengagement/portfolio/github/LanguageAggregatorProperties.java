package com.staffengagement.portfolio.github;

import com.staffengagement.portfolio.github.LanguageAggregator.AggregatedLanguage;
import net.jqwik.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for {@link LanguageAggregator}.
 */
class LanguageAggregatorProperties {

    // ========================================================================
    // Feature: github-profile-import, Property 4: Language Aggregation Is Commutative
    // ========================================================================

    /**
     * For any list of repository language maps, aggregating in any permutation
     * of the list SHALL produce identical results (same languages with same
     * byte counts and repo counts).
     *
     * **Validates: Requirements 4.4**
     */
    @Property(tries = 100)
    void aggregationIsCommutative(@ForAll("repoLanguageMaps") List<Map<String, Long>> input) {
        List<AggregatedLanguage> original = LanguageAggregator.aggregate(input);

        // Shuffle to get a different permutation
        List<Map<String, Long>> shuffled = new ArrayList<>(input);
        Collections.shuffle(shuffled);

        List<AggregatedLanguage> shuffledResult = LanguageAggregator.aggregate(shuffled);

        assertThat(shuffledResult).isEqualTo(original);
    }

    /**
     * Additional permutation check: reverse order produces same result.
     *
     * **Validates: Requirements 4.4**
     */
    @Property(tries = 100)
    void aggregationReversedOrderProducesSameResult(@ForAll("repoLanguageMaps") List<Map<String, Long>> input) {
        List<AggregatedLanguage> original = LanguageAggregator.aggregate(input);

        List<Map<String, Long>> reversed = new ArrayList<>(input);
        Collections.reverse(reversed);

        List<AggregatedLanguage> reversedResult = LanguageAggregator.aggregate(reversed);

        assertThat(reversedResult).isEqualTo(original);
    }

    // ========================================================================
    // Feature: github-profile-import, Property 5: Aggregation Sums Are Correct
    // ========================================================================

    /**
     * For any list of repository language maps, the total byte count for each
     * language in the aggregated result SHALL equal the sum of that language's
     * byte counts across all input maps, and the repo count SHALL equal the
     * number of input maps containing that language.
     *
     * **Validates: Requirements 4.1**
     */
    @Property(tries = 100)
    void aggregationSumsAreCorrect(@ForAll("repoLanguageMaps") List<Map<String, Long>> input) {
        List<AggregatedLanguage> result = LanguageAggregator.aggregate(input);

        // Compute expected sums manually
        Map<String, Long> expectedBytes = new HashMap<>();
        Map<String, Integer> expectedRepoCount = new HashMap<>();

        for (Map<String, Long> repoMap : input) {
            for (Map.Entry<String, Long> entry : repoMap.entrySet()) {
                expectedBytes.merge(entry.getKey(), entry.getValue(), Long::sum);
                expectedRepoCount.merge(entry.getKey(), 1, Integer::sum);
            }
        }

        // Every language in expected should appear in result
        assertThat(result).hasSize(expectedBytes.size());

        for (AggregatedLanguage lang : result) {
            assertThat(lang.totalBytes())
                    .as("totalBytes for %s", lang.name())
                    .isEqualTo(expectedBytes.get(lang.name()));
            assertThat(lang.repoCount())
                    .as("repoCount for %s", lang.name())
                    .isEqualTo(expectedRepoCount.get(lang.name()));
        }
    }

    // ========================================================================
    // Feature: github-profile-import, Property 6: Aggregation Sort Order
    // ========================================================================

    /**
     * For any aggregated language list, languages SHALL be ordered by total byte
     * count descending; where byte counts are equal, languages SHALL be ordered
     * alphabetically ascending by name.
     *
     * **Validates: Requirements 4.2**
     */
    @Property(tries = 100)
    void aggregationResultIsSortedByBytesDescThenNameAsc(@ForAll("repoLanguageMaps") List<Map<String, Long>> input) {
        List<AggregatedLanguage> result = LanguageAggregator.aggregate(input);

        for (int i = 0; i < result.size() - 1; i++) {
            AggregatedLanguage current = result.get(i);
            AggregatedLanguage next = result.get(i + 1);

            // Primary: descending by totalBytes
            assertThat(current.totalBytes())
                    .as("Element at index %d (%s) should have >= bytes than index %d (%s)",
                            i, current.name(), i + 1, next.name())
                    .isGreaterThanOrEqualTo(next.totalBytes());

            // Secondary: if same bytes, ascending alphabetical by name
            if (current.totalBytes() == next.totalBytes()) {
                assertThat(current.name().compareTo(next.name()))
                        .as("When bytes are tied, %s should come before %s alphabetically",
                                current.name(), next.name())
                        .isLessThanOrEqualTo(0);
            }
        }
    }

    // ========================================================================
    // Generators
    // ========================================================================

    @Provide
    Arbitrary<List<Map<String, Long>>> repoLanguageMaps() {
        Arbitrary<Map<String, Long>> singleRepoMap = languageMap();
        return singleRepoMap.list().ofMinSize(1).ofMaxSize(10);
    }

    private Arbitrary<Map<String, Long>> languageMap() {
        Arbitrary<String> languageNames = Arbitraries.of(
                "Java", "Python", "Go", "Rust", "TypeScript",
                "JavaScript", "C", "C++", "Ruby", "Kotlin",
                "Swift", "Scala", "Haskell", "Elixir", "Dart"
        );
        Arbitrary<Long> byteCounts = Arbitraries.longs().between(1L, 1_000_000L);

        return Combinators.combine(languageNames, byteCounts)
                .as(Map::entry)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5)
                .map(entries -> {
                    Map<String, Long> map = new HashMap<>();
                    for (Map.Entry<String, Long> entry : entries) {
                        map.merge(entry.getKey(), entry.getValue(), Long::sum);
                    }
                    return map;
                });
    }
}
