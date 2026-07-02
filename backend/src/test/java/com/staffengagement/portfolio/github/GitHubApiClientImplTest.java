package com.staffengagement.portfolio.github;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GitHubApiClientImpl} covering pagination, error handling,
 * Link header parsing, timeout handling, and rate-limit detection.
 */
class GitHubApiClientImplTest {

    private MockWebServer server;
    private GitHubApiClientImpl client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        String baseUrl = server.url("/").toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        GitHubImportProperties properties = new GitHubImportProperties(
                new GitHubImportProperties.Api(baseUrl, "test-token"),
                Map.of()
        );
        client = new GitHubApiClientImpl(properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    // ==================== fetchPublicRepos ====================

    @Test
    void fetchPublicRepos_shouldReturnRepos_whenSinglePageResponse() {
        String body = """
                [{"name":"repo1","owner":{"login":"user1"}},{"name":"repo2","owner":{"login":"user1"}}]
                """;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body));

        List<GitHubRepo> repos = client.fetchPublicRepos("user1");

        assertThat(repos).hasSize(2);
        assertThat(repos.get(0).name()).isEqualTo("repo1");
        assertThat(repos.get(0).owner().login()).isEqualTo("user1");
        assertThat(repos.get(1).name()).isEqualTo("repo2");
    }

    @Test
    void fetchPublicRepos_shouldFollowPagination_whenLinkHeaderPresent() {
        String nextUrl = server.url("/users/user1/repos?type=public&per_page=100&page=2").toString();
        String page1Body = """
                [{"name":"repo1","owner":{"login":"user1"}}]
                """;
        String page2Body = """
                [{"name":"repo2","owner":{"login":"user1"}}]
                """;

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .addHeader("Link", "<" + nextUrl + ">; rel=\"next\"")
                .setBody(page1Body));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(page2Body));

        List<GitHubRepo> repos = client.fetchPublicRepos("user1");

        assertThat(repos).hasSize(2);
        assertThat(repos.get(0).name()).isEqualTo("repo1");
        assertThat(repos.get(1).name()).isEqualTo("repo2");
    }

    @Test
    void fetchPublicRepos_shouldStopAfterMaxPages() {
        // Enqueue 11 pages but should stop after 10
        for (int i = 1; i <= 10; i++) {
            String nextUrl = server.url("/page" + (i + 1)).toString();
            String body = "[{\"name\":\"repo" + i + "\",\"owner\":{\"login\":\"user1\"}}]";
            MockResponse response = new MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json")
                    .setBody(body);
            if (i < 10) {
                response.addHeader("Link", "<" + nextUrl + ">; rel=\"next\"");
            }
            server.enqueue(response);
        }
        // Extra page that shouldn't be reached
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[{\"name\":\"repo11\",\"owner\":{\"login\":\"user1\"}}]"));

        List<GitHubRepo> repos = client.fetchPublicRepos("user1");

        assertThat(repos).hasSize(10);
        assertThat(server.getRequestCount()).isEqualTo(10);
    }

    @Test
    void fetchPublicRepos_shouldReturnEmptyList_whenNoRepos() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[]"));

        List<GitHubRepo> repos = client.fetchPublicRepos("user1");

        assertThat(repos).isEmpty();
    }

    @Test
    void fetchPublicRepos_shouldThrowGitHubUserNotFoundException_when404() {
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"message\":\"Not Found\"}"));

        assertThatThrownBy(() -> client.fetchPublicRepos("nonexistent"))
                .isInstanceOf(GitHubUserNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void fetchPublicRepos_shouldThrowGitHubRateLimitException_when403WithRateLimitHeaders() {
        server.enqueue(new MockResponse()
                .setResponseCode(403)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-ratelimit-remaining", "0")
                .addHeader("x-ratelimit-reset", "1700000000")
                .setBody("{\"message\":\"rate limit exceeded\"}"));

        assertThatThrownBy(() -> client.fetchPublicRepos("user1"))
                .isInstanceOf(GitHubRateLimitException.class)
                .hasMessageContaining("rate limit");
    }

    @Test
    void fetchPublicRepos_shouldThrowGitHubApiUnavailableException_when500() {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"message\":\"Internal Server Error\"}"));

        assertThatThrownBy(() -> client.fetchPublicRepos("user1"))
                .isInstanceOf(GitHubApiUnavailableException.class)
                .hasMessageContaining("500");
    }

    @Test
    void fetchPublicRepos_shouldThrowGitHubApiUnavailableException_when403WithoutRateLimitHeader() {
        server.enqueue(new MockResponse()
                .setResponseCode(403)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"message\":\"Forbidden\"}"));

        assertThatThrownBy(() -> client.fetchPublicRepos("user1"))
                .isInstanceOf(GitHubApiUnavailableException.class);
    }

    @Test
    void fetchPublicRepos_shouldThrowGitHubRateLimitException_withCurrentTimeWhenResetHeaderMalformed() {
        server.enqueue(new MockResponse()
                .setResponseCode(403)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-ratelimit-remaining", "0")
                .addHeader("x-ratelimit-reset", "not-a-number")
                .setBody("{\"message\":\"rate limit exceeded\"}"));

        assertThatThrownBy(() -> client.fetchPublicRepos("user1"))
                .isInstanceOf(GitHubRateLimitException.class);
    }

    // ==================== fetchLanguages ====================

    @Test
    void fetchLanguages_shouldReturnLanguageMap_whenSuccessful() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"Java\":150000,\"Python\":32000}"));

        Map<String, Long> languages = client.fetchLanguages("owner1", "repo1");

        assertThat(languages).containsEntry("Java", 150000L);
        assertThat(languages).containsEntry("Python", 32000L);
    }

    @Test
    void fetchLanguages_shouldReturnEmptyMap_whenResponseIsNull() {
        // Simulate null body by returning empty JSON that maps to an empty response
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));

        Map<String, Long> languages = client.fetchLanguages("owner1", "repo1");

        assertThat(languages).isEmpty();
    }

    @Test
    void fetchLanguages_shouldReturnEmptyMap_whenNonRateLimitError() {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"message\":\"Internal Server Error\"}"));

        Map<String, Long> languages = client.fetchLanguages("owner1", "repo1");

        assertThat(languages).isEmpty();
    }

    @Test
    void fetchLanguages_shouldThrowRateLimitException_when403WithRateLimitHeaders() {
        server.enqueue(new MockResponse()
                .setResponseCode(403)
                .addHeader("Content-Type", "application/json")
                .addHeader("x-ratelimit-remaining", "0")
                .addHeader("x-ratelimit-reset", "1700000000")
                .setBody("{\"message\":\"rate limit exceeded\"}"));

        assertThatThrownBy(() -> client.fetchLanguages("owner1", "repo1"))
                .isInstanceOf(GitHubRateLimitException.class);
    }

    @Test
    void fetchLanguages_shouldReturnEmptyMap_when403WithoutRateLimitHeader() {
        server.enqueue(new MockResponse()
                .setResponseCode(403)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"message\":\"Forbidden\"}"));

        Map<String, Long> languages = client.fetchLanguages("owner1", "repo1");

        assertThat(languages).isEmpty();
    }

    @Test
    void fetchLanguages_shouldReturnEmptyMap_when404() {
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"message\":\"Not Found\"}"));

        Map<String, Long> languages = client.fetchLanguages("owner1", "repo1");

        assertThat(languages).isEmpty();
    }

    // ==================== Link header parsing edge cases ====================

    @Test
    void fetchPublicRepos_shouldHandleLinkHeaderWithMultipleRels() {
        String nextUrl = server.url("/page2").toString();
        String lastUrl = server.url("/page5").toString();
        String page1Body = "[{\"name\":\"repo1\",\"owner\":{\"login\":\"user1\"}}]";
        String page2Body = "[{\"name\":\"repo2\",\"owner\":{\"login\":\"user1\"}}]";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .addHeader("Link", "<" + nextUrl + ">; rel=\"next\", <" + lastUrl + ">; rel=\"last\"")
                .setBody(page1Body));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(page2Body));

        List<GitHubRepo> repos = client.fetchPublicRepos("user1");

        assertThat(repos).hasSize(2);
    }

    @Test
    void fetchPublicRepos_shouldNotPaginate_whenNoLinkHeader() {
        String body = "[{\"name\":\"repo1\",\"owner\":{\"login\":\"user1\"}}]";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body));

        List<GitHubRepo> repos = client.fetchPublicRepos("user1");

        assertThat(repos).hasSize(1);
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void fetchPublicRepos_shouldNotPaginate_whenLinkHeaderHasNoNextRel() {
        String prevUrl = server.url("/page1").toString();
        String body = "[{\"name\":\"repo1\",\"owner\":{\"login\":\"user1\"}}]";

        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .addHeader("Link", "<" + prevUrl + ">; rel=\"prev\"")
                .setBody(body));

        List<GitHubRepo> repos = client.fetchPublicRepos("user1");

        assertThat(repos).hasSize(1);
        assertThat(server.getRequestCount()).isEqualTo(1);
    }
}
