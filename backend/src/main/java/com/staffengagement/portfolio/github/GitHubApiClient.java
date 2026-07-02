package com.staffengagement.portfolio.github;

import java.util.List;
import java.util.Map;

/**
 * Client interface for communicating with the GitHub REST API.
 * Handles fetching public repositories and language data for a given user.
 */
public interface GitHubApiClient {

    /**
     * Fetches all public repositories for the given GitHub username.
     * Follows pagination (Link header) up to a maximum of 10 pages (1000 repos).
     *
     * @param username the GitHub username
     * @return list of public repositories
     */
    List<GitHubRepo> fetchPublicRepos(String username);

    /**
     * Fetches the language byte counts for a specific repository.
     *
     * @param owner the repository owner's login
     * @param repo  the repository name
     * @return map of language name to byte count
     */
    Map<String, Long> fetchLanguages(String owner, String repo);
}
