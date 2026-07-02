package com.staffengagement.portfolio.github;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for the GitHub profile import endpoint.
 * Contains the GitHub profile URL to import skills from.
 */
public record GitHubImportRequest(
        @NotBlank(message = "githubProfileUrl is required")
        String githubProfileUrl
) {}
