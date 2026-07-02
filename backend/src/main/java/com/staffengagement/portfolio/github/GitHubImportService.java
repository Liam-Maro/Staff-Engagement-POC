package com.staffengagement.portfolio.github;

import java.util.UUID;

/**
 * Orchestrates the GitHub profile import workflow:
 * validate config → validate employee → parse URL → fetch repos →
 * fetch languages → aggregate → map → rank → upsert skills →
 * store PortfolioLink → return result.
 */
public interface GitHubImportService {

    /**
     * Imports programming language skills from a GitHub profile for the given employee.
     *
     * @param employeeId       the employee to import skills for
     * @param githubProfileUrl the GitHub profile URL to import from
     * @return the import result containing created/updated skills and metadata
     * @throws GitHubNotConfiguredException if PAT or base URL is not configured
     * @throws EmployeeNotActiveException   if the employee is archived/deactivated
     */
    ImportResult importFromGitHub(UUID employeeId, String githubProfileUrl);
}
