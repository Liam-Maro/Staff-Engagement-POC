package com.staffengagement.portfolio.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Configuration properties for GitHub profile import integration.
 * Binds to the "github" prefix in application properties.
 */
@ConfigurationProperties(prefix = "github")
public record GitHubImportProperties(
    Api api,
    Map<String, String> languageMapping
) {
    /**
     * GitHub API connection settings.
     *
     * @param baseUrl the base URL for GitHub REST API (e.g., https://api.github.com)
     * @param pat     the GitHub Personal Access Token for authenticated requests
     */
    public record Api(String baseUrl, String pat) {}
}
