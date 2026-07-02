package com.staffengagement.portfolio.github;

import com.staffengagement.portfolio.github.LanguageAggregator.AggregatedLanguage;
import net.jqwik.api.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for resilient fetching behaviour of {@link GitHubApiClientImpl}.
 * Uses MockWebServer to simulate partial failures and verifies that successful data
 * is preserved while failed repos are correctly excluded from aggregation.
 */
class GitHubApiClientProperties {

    // ========================================================================
    // Feature: github-profile-import, Property 13: Resilient Fetching Preserves Successful Data
    // ========================================================================

    /**
     * For any set of repositories where some language fetches fail (non-rate-limit errors),
     * the aggregation SHALL include language data from all successfully fetched repositories
     * and exclude failed ones. The skipped repository list SHALL contain exactly the failed
     * repository names.
     *
     * **Validates: Requirements 3.3, 4.5**
     */
    @Property(tries = 50)
    void resilientFetchingPreservesSuccessfulData(
            @ForAll("repoScenarios") RepoScenario scenario
    ) throws IOException {
        try (MockWebServer server = new MockWebServer()) {
            server.start();

            // Enqueue responses for each repo in order
            for (RepoSetup setup : scenario.repos()) {
                if (setup.shouldFail()) {
                    server.enqueue(new MockResponse()
                            .setResponseCode(500)
                            .setBody("{\"message\":\"Internal Server Error\"}"));
                } else {
                    String jsonBody = toJsonMap(setup.languages());
                    server.enqueue(new MockResponse()
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json")
                            .setBody(jsonBody));
                }
            }

            // Create client pointing to MockWebServer
            String baseUrl = server.url("/").toString();
            // Remove trailing slash to match expected format
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            GitHubImportProperties properties = new GitHubImportProperties(
                    new GitHubImportProperties.Api(baseUrl, "test-token"),
                    Map.of()
            );
            GitHubApiClientImpl client = new GitHubApiClientImpl(properties);

            // Call fetchLanguages for each repo and collect results
            List<Map<String, Long>> successfulLanguageMaps = new ArrayList<>();
            List<String> skippedRepos = new ArrayList<>();

            for (RepoSetup setup : scenario.repos()) {
                Map<String, Long> languages = client.fetchLanguages("owner", setup.repoName());

                if (languages.isEmpty()) {
                    skippedRepos.add(setup.repoName());
                } else {
                    successfulLanguageMaps.add(languages);
                }
            }

            // Assert: skipped repos match exactly the repos designated to fail
            List<String> expectedSkipped = scenario.repos().stream()
                    .filter(RepoSetup::shouldFail)
                    .map(RepoSetup::repoName)
                    .toList();
            assertThat(skippedRepos)
                    .as("Skipped repos should contain exactly the failed repo names")
                    .containsExactlyInAnyOrderElementsOf(expectedSkipped);

            // Assert: successful language maps match expected data
            List<Map<String, Long>> expectedSuccessful = scenario.repos().stream()
                    .filter(r -> !r.shouldFail())
                    .map(RepoSetup::languages)
                    .toList();
            assertThat(successfulLanguageMaps)
                    .as("Successful language data should include only non-failed repos")
                    .hasSize(expectedSuccessful.size());

            // Assert: aggregation includes only successfully fetched data
            List<AggregatedLanguage> aggregated = LanguageAggregator.aggregate(successfulLanguageMaps);

            // Compute expected aggregation from successful repos only
            Map<String, Long> expectedTotalBytes = new HashMap<>();
            for (Map<String, Long> langMap : expectedSuccessful) {
                for (Map.Entry<String, Long> entry : langMap.entrySet()) {
                    expectedTotalBytes.merge(entry.getKey(), entry.getValue(), Long::sum);
                }
            }

            // Verify aggregation matches expected (successful data only, no failed data)
            for (AggregatedLanguage lang : aggregated) {
                assertThat(expectedTotalBytes)
                        .as("Aggregated language '%s' should come from successful repos only", lang.name())
                        .containsKey(lang.name());
                assertThat(lang.totalBytes())
                        .as("Total bytes for '%s' should match sum from successful repos", lang.name())
                        .isEqualTo(expectedTotalBytes.get(lang.name()));
            }

            // Verify no extra languages in aggregation
            assertThat(aggregated.stream().map(AggregatedLanguage::name).toList())
                    .as("Aggregated languages should match exactly the languages from successful repos")
                    .containsExactlyInAnyOrderElementsOf(expectedTotalBytes.keySet());
        }
    }

    // ========================================================================
    // Generators
    // ========================================================================

    @Provide
    Arbitrary<RepoScenario> repoScenarios() {
        return repoSetup()
                .list()
                .ofMinSize(2)
                .ofMaxSize(6)
                .filter(repos -> repos.stream().anyMatch(RepoSetup::shouldFail))
                .filter(repos -> repos.stream().anyMatch(r -> !r.shouldFail()))
                .map(RepoScenario::new);
    }

    private Arbitrary<RepoSetup> repoSetup() {
        Arbitrary<String> repoNames = Arbitraries.of(
                "repo-alpha", "repo-beta", "repo-gamma",
                "repo-delta", "repo-epsilon", "repo-zeta"
        );
        Arbitrary<Boolean> shouldFail = Arbitraries.of(true, false);
        Arbitrary<Map<String, Long>> languages = languageMap();

        return Combinators.combine(repoNames, shouldFail, languages)
                .as(RepoSetup::new);
    }

    private Arbitrary<Map<String, Long>> languageMap() {
        Arbitrary<String> languageNames = Arbitraries.of(
                "Java", "Python", "Go", "Rust", "TypeScript",
                "JavaScript", "C", "Ruby", "Kotlin", "Swift"
        );
        Arbitrary<Long> byteCounts = Arbitraries.longs().between(100L, 500_000L);

        return Combinators.combine(languageNames, byteCounts)
                .as(Map::entry)
                .list()
                .ofMinSize(1)
                .ofMaxSize(4)
                .map(entries -> {
                    Map<String, Long> map = new LinkedHashMap<>();
                    for (Map.Entry<String, Long> entry : entries) {
                        map.put(entry.getKey(), entry.getValue());
                    }
                    return map;
                });
    }

    // ========================================================================
    // Helper types
    // ========================================================================

    record RepoScenario(List<RepoSetup> repos) {}

    record RepoSetup(String repoName, boolean shouldFail, Map<String, Long> languages) {}

    // ========================================================================
    // Utility
    // ========================================================================

    private String toJsonMap(Map<String, Long> languages) {
        StringBuilder sb = new StringBuilder("{");
        Iterator<Map.Entry<String, Long>> it = languages.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            if (it.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
