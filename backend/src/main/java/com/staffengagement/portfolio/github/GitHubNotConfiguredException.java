package com.staffengagement.portfolio.github;

/**
 * Thrown when GitHub integration is not properly configured
 * (missing PAT or base URL). Maps to HTTP 503.
 */
public class GitHubNotConfiguredException extends RuntimeException {

    public GitHubNotConfiguredException() {
        super("GitHub integration is not configured (missing PAT or base URL)");
    }
}
