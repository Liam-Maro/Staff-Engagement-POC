package com.staffengagement.portfolio.github;

/**
 * Thrown when a submitted URL does not match the expected GitHub profile URL pattern.
 */
public class InvalidGitHubUrlException extends IllegalArgumentException {

    public InvalidGitHubUrlException(String message) {
        super(message);
    }
}
