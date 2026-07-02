package com.staffengagement.portfolio.github;

import java.util.regex.Pattern;

/**
 * Validates and parses a GitHub profile URL, extracting the username.
 *
 * <p>Accepted format: {@code https://github.com/{username}} (case-insensitive scheme and host).
 * Handles leading/trailing whitespace and trailing slashes.
 *
 * <p>Username rules: 1–39 characters, alphanumeric and hyphens only,
 * no leading/trailing/consecutive hyphens.
 */
public final class GitHubUrlParser {

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$");

    private static final int MAX_USERNAME_LENGTH = 39;

    private GitHubUrlParser() {
        // Utility class — no instantiation
    }

    public record ParseResult(String username) {}

    /**
     * Validates and parses a GitHub profile URL.
     * Trims whitespace, normalises trailing slashes, validates pattern.
     *
     * @param url raw input URL string (nullable)
     * @return ParseResult with extracted username
     * @throws InvalidGitHubUrlException if URL is null, blank, wrong domain,
     *         has extra path segments, query params, fragments, or username is invalid
     */
    public static ParseResult parse(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidGitHubUrlException(
                    "Invalid GitHub profile URL format. Expected: https://github.com/{username}");
        }

        String trimmed = url.strip();

        // Reject query params and fragments before any further processing
        if (trimmed.contains("?") || trimmed.contains("#")) {
            throw new InvalidGitHubUrlException(
                    "Only GitHub profile URLs are accepted (no repository or tab paths)");
        }

        // Strip trailing slash(es)
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        // Validate scheme and host (case-insensitive)
        String lowerUrl = trimmed.toLowerCase();
        if (!lowerUrl.startsWith("https://github.com/") && !lowerUrl.startsWith("http://github.com/")) {
            throw new InvalidGitHubUrlException(
                    "Invalid GitHub profile URL format. Expected: https://github.com/{username}");
        }

        // Extract path after "github.com/"
        int hostEnd = lowerUrl.indexOf("github.com/") + "github.com/".length();
        String path = trimmed.substring(hostEnd);

        // Reject extra path segments
        if (path.contains("/")) {
            throw new InvalidGitHubUrlException(
                    "Only GitHub profile URLs are accepted (no repository or tab paths)");
        }

        // Reject empty username (URL was just "https://github.com/" after trimming)
        if (path.isEmpty()) {
            throw new InvalidGitHubUrlException(
                    "Invalid GitHub profile URL format. Expected: https://github.com/{username}");
        }

        String username = path;

        // Validate username length
        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new InvalidGitHubUrlException(
                    "Invalid GitHub username: must be between 1 and 39 characters");
        }

        // Validate username pattern
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new InvalidGitHubUrlException(
                    "Invalid GitHub username: must contain only alphanumeric characters and hyphens, " +
                            "and cannot start or end with a hyphen");
        }

        // Reject consecutive hyphens
        if (username.contains("--")) {
            throw new InvalidGitHubUrlException(
                    "Invalid GitHub username: cannot contain consecutive hyphens");
        }

        return new ParseResult(username);
    }
}
