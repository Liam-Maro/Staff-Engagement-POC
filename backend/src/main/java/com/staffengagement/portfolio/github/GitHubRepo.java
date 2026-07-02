package com.staffengagement.portfolio.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Record DTO for deserializing GitHub repository data from the REST API.
 *
 * @param name     the repository name (e.g., "my-project")
 * @param fullName the full repository name including owner (e.g., "octocat/my-project")
 * @param owner    the repository owner information
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepo(
    String name,
    @JsonProperty("full_name") String fullName,
    Owner owner
) {
    /**
     * Represents the owner of a GitHub repository.
     *
     * @param login the owner's GitHub username
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(String login) {}
}
