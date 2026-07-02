package com.staffengagement.portfolio.github;

/**
 * Thrown when GitHub returns HTTP 404 for a user lookup.
 */
public class GitHubUserNotFoundException extends RuntimeException {

    private final String username;

    public GitHubUserNotFoundException(String username) {
        super("GitHub user not found: " + username);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
