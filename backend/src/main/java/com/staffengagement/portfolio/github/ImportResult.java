package com.staffengagement.portfolio.github;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for the GitHub profile import operation.
 * Contains the list of created/updated skills, the stored GitHub profile URL,
 * the number of repositories analysed, and any repositories that were skipped due to errors.
 */
public record ImportResult(
        List<ImportedSkill> skills,
        String githubProfileUrl,
        int repositoriesAnalysed,
        List<String> skippedRepositories
) {
    /**
     * Represents a single skill entry created or updated during the import.
     */
    public record ImportedSkill(
            UUID id,
            String name,
            int projectCount,
            String proficiency,
            String source
    ) {}
}
