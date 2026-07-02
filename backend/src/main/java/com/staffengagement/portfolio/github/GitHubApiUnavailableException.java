package com.staffengagement.portfolio.github;

/**
 * Thrown when GitHub returns a 5xx server error or an unexpected 4xx client error
 * (other than 404 or rate-limit 403).
 */
public class GitHubApiUnavailableException extends RuntimeException {

    private final int statusCode;

    public GitHubApiUnavailableException(int statusCode) {
        super("GitHub API is currently unavailable (HTTP " + statusCode + ")");
        this.statusCode = statusCode;
    }

    public GitHubApiUnavailableException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
