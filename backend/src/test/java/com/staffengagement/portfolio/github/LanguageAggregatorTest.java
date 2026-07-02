package com.staffengagement.portfolio.github;

import com.staffengagement.portfolio.github.LanguageAggregator.AggregatedLanguage;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageAggregatorTest {

    @Test
    void aggregate_nullInput_returnsEmptyList() {
        List<AggregatedLanguage> result = LanguageAggregator.aggregate(null);
        assertThat(result).isEmpty();
    }

    @Test
    void aggregate_emptyList_returnsEmptyList() {
        List<AggregatedLanguage> result = LanguageAggregator.aggregate(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void aggregate_singleRepoSingleLanguage_returnsCorrectCounts() {
        List<Map<String, Long>> input = List.of(Map.of("Java", 50000L));

        List<AggregatedLanguage> result = LanguageAggregator.aggregate(input);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(new AggregatedLanguage("Java", 50000L, 1));
    }

    @Test
    void aggregate_multipleReposSameLanguage_sumsBytesAndCountsRepos() {
        List<Map<String, Long>> input = List.of(
                Map.of("Java", 30000L),
                Map.of("Java", 20000L),
                Map.of("Java", 10000L)
        );

        List<AggregatedLanguage> result = LanguageAggregator.aggregate(input);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(new AggregatedLanguage("Java", 60000L, 3));
    }

    @Test
    void aggregate_multipleLanguages_sortedByTotalBytesDescending() {
        List<Map<String, Long>> input = List.of(
                Map.of("Python", 10000L, "Java", 50000L, "Go", 30000L)
        );

        List<AggregatedLanguage> result = LanguageAggregator.aggregate(input);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).name()).isEqualTo("Java");
        assertThat(result.get(1).name()).isEqualTo("Go");
        assertThat(result.get(2).name()).isEqualTo("Python");
    }

    @Test
    void aggregate_tiedBytes_sortedAlphabeticallyAscending() {
        List<Map<String, Long>> input = List.of(
                Map.of("Zebra", 1000L, "Alpha", 1000L, "Mango", 1000L)
        );

        List<AggregatedLanguage> result = LanguageAggregator.aggregate(input);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).name()).isEqualTo("Alpha");
        assertThat(result.get(1).name()).isEqualTo("Mango");
        assertThat(result.get(2).name()).isEqualTo("Zebra");
    }

    @Test
    void aggregate_multipleReposMultipleLanguages_aggregatesCorrectly() {
        List<Map<String, Long>> input = List.of(
                Map.of("Java", 50000L, "Python", 20000L),
                Map.of("Java", 30000L, "Go", 15000L),
                Map.of("Python", 10000L, "Go", 5000L)
        );

        List<AggregatedLanguage> result = LanguageAggregator.aggregate(input);

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(new AggregatedLanguage("Java", 80000L, 2));
        assertThat(result.get(1)).isEqualTo(new AggregatedLanguage("Python", 30000L, 2));
        assertThat(result.get(2)).isEqualTo(new AggregatedLanguage("Go", 20000L, 2));
    }

    @Test
    void aggregate_emptyMapsInList_skippedGracefully() {
        List<Map<String, Long>> input = List.of(
                Map.of("Java", 5000L),
                Collections.emptyMap(),
                Map.of("Java", 3000L)
        );

        List<AggregatedLanguage> result = LanguageAggregator.aggregate(input);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(new AggregatedLanguage("Java", 8000L, 2));
    }

    @Test
    void aggregate_largeByteValues_uses64BitIntegers() {
        long largeValue = Long.MAX_VALUE / 2;
        List<Map<String, Long>> input = List.of(
                Map.of("Java", largeValue),
                Map.of("Java", 1000L)
        );

        List<AggregatedLanguage> result = LanguageAggregator.aggregate(input);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).totalBytes()).isEqualTo(largeValue + 1000L);
    }

    @Test
    void aggregate_repoCountOnlyCountsReposContainingLanguage() {
        List<Map<String, Long>> input = List.of(
                Map.of("Java", 50000L, "Python", 20000L),
                Map.of("Java", 30000L),
                Map.of("Python", 10000L, "Go", 5000L)
        );

        List<AggregatedLanguage> result = LanguageAggregator.aggregate(input);

        // Java in 2 repos, Python in 2 repos, Go in 1 repo
        AggregatedLanguage java = result.stream().filter(l -> l.name().equals("Java")).findFirst().orElseThrow();
        AggregatedLanguage python = result.stream().filter(l -> l.name().equals("Python")).findFirst().orElseThrow();
        AggregatedLanguage go = result.stream().filter(l -> l.name().equals("Go")).findFirst().orElseThrow();

        assertThat(java.repoCount()).isEqualTo(2);
        assertThat(python.repoCount()).isEqualTo(2);
        assertThat(go.repoCount()).isEqualTo(1);
    }
}
