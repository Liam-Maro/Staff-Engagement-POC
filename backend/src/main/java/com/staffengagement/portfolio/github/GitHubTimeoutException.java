package com.staffengagement.portfolio.github;

/**
 * Thrown when a GitHub API request exceeds the configured timeout (30 seconds).
 */
public class GitHubTimeoutException extends RuntimeException {

    public GitHubTimeoutException() {
        super("GitHub API request timed out");
    }

    public GitHubTimeoutException(String message) {
        super(message);
    }
}
