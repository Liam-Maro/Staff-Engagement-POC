package com.staffengagement.portfolio.github;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link GitHubApiClient} using Spring's {@link RestClient}.
 * Configured with a 30-second timeout, Bearer token authentication, and
 * pagination support via the Link header (up to 10 pages).
 */
@Component
public class GitHubApiClientImpl implements GitHubApiClient {

    private static final int MAX_PAGES = 10;
    private static final int PER_PAGE = 100;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Pattern LINK_NEXT_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
    private static final String RATE_LIMIT_REMAINING_HEADER = "x-ratelimit-remaining";
    private static final String RATE_LIMIT_RESET_HEADER = "x-ratelimit-reset";

    private final RestClient restClient;

    public GitHubApiClientImpl(GitHubImportProperties properties) {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(REQUEST_TIMEOUT)
                        .withReadTimeout(REQUEST_TIMEOUT)
        );

        this.restClient = RestClient.builder()
                .baseUrl(properties.api().baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.api().pat())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<GitHubRepo> fetchPublicRepos(String username) {
        try {
            List<GitHubRepo> allRepos = new ArrayList<>();
            String url = "/users/{username}/repos?type=public&per_page={perPage}";

            ResponseEntity<List<GitHubRepo>> response = restClient.get()
                    .uri(url, username, PER_PAGE)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<List<GitHubRepo>>() {});

            addReposFromResponse(response, allRepos);

            int pagesFollowed = 1;
            String nextUrl = extractNextLink(response.getHeaders());

            while (nextUrl != null && pagesFollowed < MAX_PAGES) {
                ResponseEntity<List<GitHubRepo>> nextResponse = restClient.get()
                        .uri(nextUrl)
                        .retrieve()
                        .toEntity(new ParameterizedTypeReference<List<GitHubRepo>>() {});

                addReposFromResponse(nextResponse, allRepos);
                nextUrl = extractNextLink(nextResponse.getHeaders());
                pagesFollowed++;
            }

            return allRepos;
        } catch (RestClientResponseException ex) {
            throw mapResponseException(ex, username);
        } catch (ResourceAccessException ex) {
            throw new GitHubTimeoutException();
        }
    }

    @Override
    public Map<String, Long> fetchLanguages(String owner, String repo) {
        try {
            Map<String, Long> languages = restClient.get()
                    .uri("/repos/{owner}/{repo}/languages", owner, repo)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Long>>() {});

            return languages != null ? languages : Map.of();
        } catch (RestClientResponseException ex) {
            if (isRateLimitExceeded(ex)) {
                throw buildRateLimitException(ex);
            }
            // Non-rate-limit errors for language endpoint: return empty map (skip signal)
            return Collections.emptyMap();
        } catch (ResourceAccessException ex) {
            // Timeout on language fetch: return empty map (skip signal per repo)
            return Collections.emptyMap();
        }
    }

    /**
     * Maps a RestClientResponseException to the appropriate custom exception
     * for the repos endpoint.
     */
    private RuntimeException mapResponseException(RestClientResponseException ex, String username) {
        int statusCode = ex.getStatusCode().value();

        if (statusCode == 404) {
            return new GitHubUserNotFoundException(username);
        }

        if (statusCode == 403 && isRateLimitExceeded(ex)) {
            return buildRateLimitException(ex);
        }

        // 5xx or unexpected 4xx
        return new GitHubApiUnavailableException(statusCode);
    }

    /**
     * Checks if a 403 response indicates rate-limit exhaustion by inspecting
     * the x-ratelimit-remaining header.
     */
    private boolean isRateLimitExceeded(RestClientResponseException ex) {
        if (ex.getStatusCode().value() != 403) {
            return false;
        }
        HttpHeaders headers = ex.getResponseHeaders();
        if (headers == null) {
            return false;
        }
        String remaining = headers.getFirst(RATE_LIMIT_REMAINING_HEADER);
        return remaining != null && "0".equals(remaining.trim());
    }

    /**
     * Builds a GitHubRateLimitException extracting the reset time from headers.
     */
    private GitHubRateLimitException buildRateLimitException(RestClientResponseException ex) {
        HttpHeaders headers = ex.getResponseHeaders();
        Instant resetTime = Instant.now();
        if (headers != null) {
            String resetHeader = headers.getFirst(RATE_LIMIT_RESET_HEADER);
            if (resetHeader != null) {
                try {
                    long epochSeconds = Long.parseLong(resetHeader.trim());
                    resetTime = Instant.ofEpochSecond(epochSeconds);
                } catch (NumberFormatException ignored) {
                    // Fall back to current time if header is malformed
                }
            }
        }
        return new GitHubRateLimitException(resetTime);
    }

    private void addReposFromResponse(ResponseEntity<List<GitHubRepo>> response, List<GitHubRepo> accumulator) {
        List<GitHubRepo> body = response.getBody();
        if (body != null) {
            accumulator.addAll(body);
        }
    }

    /**
     * Parses the Link header to extract the URL for the "next" page.
     *
     * @param headers the HTTP response headers
     * @return the next page URL, or null if no next link exists
     */
    private String extractNextLink(HttpHeaders headers) {
        List<String> linkHeaders = headers.get("Link");
        if (linkHeaders == null || linkHeaders.isEmpty()) {
            return null;
        }

        for (String linkHeader : linkHeaders) {
            Matcher matcher = LINK_NEXT_PATTERN.matcher(linkHeader);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }
}
