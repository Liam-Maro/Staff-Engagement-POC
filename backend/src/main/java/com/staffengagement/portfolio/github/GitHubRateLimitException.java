package com.staffengagement.portfolio.github;

import java.time.Instant;

/**
 * Thrown when GitHub returns HTTP 403 with rate-limit headers indicating
 * the limit has been exceeded (x-ratelimit-remaining = 0).
 */
public class GitHubRateLimitException extends RuntimeException {

    private final Instant resetTime;

    public GitHubRateLimitException(Instant resetTime) {
        super("GitHub API rate limit exceeded. Resets at: " + resetTime);
        this.resetTime = resetTime;
    }

    public Instant getResetTime() {
        return resetTime;
    }
}
