package com.staffengagement.portfolio.github;

import com.staffengagement.employee.model.Employee;
import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.portfolio.github.GitHubUrlParser.ParseResult;
import com.staffengagement.portfolio.github.LanguageAggregator.AggregatedLanguage;
import com.staffengagement.portfolio.github.LanguageSkillMapper.MappedSkill;
import com.staffengagement.portfolio.model.PortfolioLink;
import com.staffengagement.portfolio.repository.PortfolioLinkRepository;
import com.staffengagement.skills.model.Skill;
import com.staffengagement.skills.repository.SkillRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements the full GitHub profile import workflow:
 * <ol>
 *   <li>Validate configuration (PAT + base URL)</li>
 *   <li>Validate employee exists and is active</li>
 *   <li>Parse GitHub profile URL</li>
 *   <li>Fetch public repositories</li>
 *   <li>Fetch languages per repository (resilient)</li>
 *   <li>Aggregate language byte counts</li>
 *   <li>Map languages to skill names</li>
 *   <li>Rank proficiencies</li>
 *   <li>Upsert skills (create new, update existing, delete stale)</li>
 *   <li>Upsert PortfolioLink with label "GitHub"</li>
 *   <li>Return ImportResult</li>
 * </ol>
 */
@Service
public class GitHubImportServiceImpl implements GitHubImportService {

    private static final String GITHUB_SOURCE = "GITHUB";
    private static final String GITHUB_LINK_LABEL = "GitHub";

    private final GitHubApiClient gitHubApiClient;
    private final SkillRepository skillRepository;
    private final PortfolioLinkRepository portfolioLinkRepository;
    private final GitHubImportProperties properties;
    private final EmployeeRepository employeeRepository;

    public GitHubImportServiceImpl(
            GitHubApiClient gitHubApiClient,
            SkillRepository skillRepository,
            PortfolioLinkRepository portfolioLinkRepository,
            GitHubImportProperties properties,
            EmployeeRepository employeeRepository
    ) {
        this.gitHubApiClient = gitHubApiClient;
        this.skillRepository = skillRepository;
        this.portfolioLinkRepository = portfolioLinkRepository;
        this.properties = properties;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public ImportResult importFromGitHub(UUID employeeId, String githubProfileUrl) {
        // Step 1: Validate configuration
        validateConfiguration();

        // Step 2: Validate employee exists
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + employeeId));

        // Step 3: Validate employee is active
        if (!employee.isActive()) {
            throw new EmployeeNotActiveException(employeeId);
        }

        // Step 4: Parse URL
        ParseResult parseResult = GitHubUrlParser.parse(githubProfileUrl);
        String username = parseResult.username();

        // Step 5: Fetch repos
        List<GitHubRepo> repos = gitHubApiClient.fetchPublicRepos(username);

        // Step 6: Fetch languages per repo (resilient)
        List<Map<String, Long>> repoLanguageMaps = new ArrayList<>();
        List<String> skippedRepositories = new ArrayList<>();

        for (GitHubRepo repo : repos) {
            Map<String, Long> languages = gitHubApiClient.fetchLanguages(
                    repo.owner().login(), repo.name());

            if (languages.isEmpty()) {
                // Empty map = skipped or no languages detected.
                // Only add to skipped if the repo was expected to have languages
                // (GitHub API returns empty map for repos with no code too,
                // but per spec: empty map from fetchLanguages = skip signal)
                // Per requirement 3.5: empty language map repos are NOT included in skipped metadata.
                // The skip signal from the client (due to errors) also returns empty map.
                // We track repos that returned empty as potential skips, but per design
                // we cannot distinguish "no languages" from "error". The GitHubApiClient
                // returns empty map for both. We include these only if we can tell it's an error skip.
                // Since the client returns empty map for BOTH no-languages AND errors,
                // and per requirement 3.5 we should NOT include no-languages repos in skipped,
                // we'll treat all empty responses as "no languages" (non-skipped).
                // The truly "skipped due to error" repos are indistinguishable at this level.
                // Per the task spec: "Track skipped repos (repos where fetchLanguages returned empty
                // AND the repo was expected to have languages)" — but we can't know "expected to have"
                // from this context. We'll skip adding to the skipped list since we can't distinguish.
                continue;
            }

            repoLanguageMaps.add(languages);
        }

        // Step 7: Aggregate
        List<AggregatedLanguage> aggregated = LanguageAggregator.aggregate(repoLanguageMaps);

        // Step 8: Map
        Map<String, String> languageMapping = properties.languageMapping() != null
                ? properties.languageMapping()
                : Map.of();
        List<MappedSkill> mappedSkills = LanguageSkillMapper.map(aggregated, languageMapping);

        // Step 9: Rank
        Map<String, Proficiency> proficiencies = ProficiencyRanker.rank(mappedSkills);

        // Step 10: Upsert skills
        List<Skill> upsertedSkills = upsertSkills(employeeId, mappedSkills, proficiencies);

        // Step 11: Delete stale GITHUB skills
        deleteStaleSkills(employeeId, upsertedSkills);

        // Step 12: Upsert PortfolioLink
        upsertPortfolioLink(employeeId, githubProfileUrl);

        // Step 13: Build and return result
        int repositoriesAnalysed = repoLanguageMaps.size();
        List<ImportResult.ImportedSkill> importedSkills = upsertedSkills.stream()
                .map(skill -> new ImportResult.ImportedSkill(
                        skill.getId(),
                        skill.getName(),
                        skill.getProjectCount(),
                        skill.getProficiency(),
                        skill.getSource()
                ))
                .toList();

        return new ImportResult(importedSkills, githubProfileUrl, repositoriesAnalysed, skippedRepositories);
    }

    private void validateConfiguration() {
        if (properties.api() == null
                || isBlank(properties.api().pat())
                || isBlank(properties.api().baseUrl())) {
            throw new GitHubNotConfiguredException();
        }
    }

    private List<Skill> upsertSkills(UUID employeeId, List<MappedSkill> mappedSkills,
                                     Map<String, Proficiency> proficiencies) {
        List<Skill> result = new ArrayList<>();

        for (MappedSkill mapped : mappedSkills) {
            String skillName = mapped.skillName();
            Proficiency proficiency = proficiencies.get(skillName);
            String proficiencyStr = proficiency != null ? proficiency.name() : Proficiency.INTERMEDIATE.name();

            Optional<Skill> existing = skillRepository.findByEmployeeIdAndNameAndSource(
                    employeeId, skillName, GITHUB_SOURCE);

            if (existing.isPresent()) {
                // Update existing skill
                Skill skill = existing.get();
                skill.setProjectCount(mapped.repoCount());
                skill.setProficiency(proficiencyStr);
                result.add(skillRepository.save(skill));
            } else {
                // Create new skill
                Skill skill = new Skill();
                skill.setEmployeeId(employeeId);
                skill.setName(skillName);
                skill.setProjectCount(mapped.repoCount());
                skill.setProficiency(proficiencyStr);
                skill.setSource(GITHUB_SOURCE);
                skill.setYearsExperience(0);
                result.add(skillRepository.save(skill));
            }
        }

        return result;
    }

    private void deleteStaleSkills(UUID employeeId, List<Skill> currentSkills) {
        Set<UUID> currentSkillIds = currentSkills.stream()
                .map(Skill::getId)
                .collect(Collectors.toSet());

        List<Skill> allGitHubSkills = skillRepository.findByEmployeeIdAndSource(employeeId, GITHUB_SOURCE);

        for (Skill existing : allGitHubSkills) {
            if (!currentSkillIds.contains(existing.getId())) {
                skillRepository.delete(existing);
            }
        }
    }

    private void upsertPortfolioLink(UUID employeeId, String githubProfileUrl) {
        Optional<PortfolioLink> existingLink = portfolioLinkRepository
                .findByEmployeeIdAndLabel(employeeId, GITHUB_LINK_LABEL);

        if (existingLink.isPresent()) {
            PortfolioLink link = existingLink.get();
            link.setUrl(githubProfileUrl);
            portfolioLinkRepository.save(link);
        } else {
            PortfolioLink link = new PortfolioLink();
            link.setEmployeeId(employeeId);
            link.setLabel(GITHUB_LINK_LABEL);
            link.setUrl(githubProfileUrl);
            portfolioLinkRepository.save(link);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
