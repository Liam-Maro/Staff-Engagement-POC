package com.staffengagement.portfolio.github;

import com.staffengagement.employee.model.Employee;
import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.portfolio.model.PortfolioLink;
import com.staffengagement.portfolio.repository.PortfolioLinkRepository;
import com.staffengagement.skills.model.Skill;
import com.staffengagement.skills.repository.SkillRepository;
import net.jqwik.api.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for {@link GitHubImportServiceImpl} orchestration logic.
 * Tests are run with mocked dependencies to verify correctness properties
 * of the import workflow.
 */
class GitHubImportServiceProperties {

    // ========================================================================
    // Feature: github-profile-import, Property 11: Import Idempotency (No Duplicates)
    // ========================================================================

    /**
     * Property 11: Import Idempotency (No Duplicates)
     *
     * For any employee and import data, running the import twice with the same data
     * SHALL produce the same set of skills (no duplicates created). The second run
     * SHALL update existing entries rather than creating new ones.
     *
     * **Validates: Requirements 6.3**
     */
    @Property(tries = 50)
    void importIdempotencyNoDuplicates(
            @ForAll("languageMapsForRepos") List<Map<String, Long>> repoLanguages
    ) {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        String username = "testuser";
        String profileUrl = "https://github.com/" + username;

        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        GitHubApiClient gitHubApiClient = mock(GitHubApiClient.class);
        SkillRepository skillRepository = mock(SkillRepository.class);
        PortfolioLinkRepository portfolioLinkRepository = mock(PortfolioLinkRepository.class);

        GitHubImportProperties properties = new GitHubImportProperties(
                new GitHubImportProperties.Api("https://api.github.com", "test-pat"),
                Map.of()
        );

        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setActive(true);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        // Set up repos matching the generated language maps
        List<GitHubRepo> repos = new ArrayList<>();
        for (int i = 0; i < repoLanguages.size(); i++) {
            String repoName = "repo-" + i;
            GitHubRepo repo = new GitHubRepo(repoName, username + "/" + repoName,
                    new GitHubRepo.Owner(username));
            repos.add(repo);
        }
        when(gitHubApiClient.fetchPublicRepos(username)).thenReturn(repos);

        for (int i = 0; i < repos.size(); i++) {
            when(gitHubApiClient.fetchLanguages(username, repos.get(i).name()))
                    .thenReturn(repoLanguages.get(i));
        }

        // Track persisted skills to simulate DB state between runs
        Map<String, Skill> persistedSkills = new LinkedHashMap<>();

        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> {
            Skill input = invocation.getArgument(0);
            if (input.getId() == null) {
                // New skill — assign ID via reflection
                setSkillId(input, UUID.randomUUID());
            }
            persistedSkills.put(input.getName(), input);
            return input;
        });

        // First run: no existing skills
        when(skillRepository.findByEmployeeIdAndNameAndSource(eq(employeeId), anyString(), eq("GITHUB")))
                .thenReturn(Optional.empty());
        when(skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB"))
                .thenReturn(List.of());
        when(portfolioLinkRepository.findByEmployeeIdAndLabel(employeeId, "GitHub"))
                .thenReturn(Optional.empty());
        when(portfolioLinkRepository.save(any(PortfolioLink.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act: First import
        GitHubImportServiceImpl service = new GitHubImportServiceImpl(
                gitHubApiClient, skillRepository, portfolioLinkRepository,
                properties, employeeRepository
        );
        ImportResult firstResult = service.importFromGitHub(employeeId, profileUrl);

        // Set up second run: existing skills are now returned from "DB"
        for (Map.Entry<String, Skill> entry : persistedSkills.entrySet()) {
            when(skillRepository.findByEmployeeIdAndNameAndSource(employeeId, entry.getKey(), "GITHUB"))
                    .thenReturn(Optional.of(entry.getValue()));
        }
        when(skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB"))
                .thenReturn(new ArrayList<>(persistedSkills.values()));

        PortfolioLink existingLink = new PortfolioLink();
        existingLink.setEmployeeId(employeeId);
        existingLink.setLabel("GitHub");
        existingLink.setUrl(profileUrl);
        when(portfolioLinkRepository.findByEmployeeIdAndLabel(employeeId, "GitHub"))
                .thenReturn(Optional.of(existingLink));

        // Act: Second import (same data)
        ImportResult secondResult = service.importFromGitHub(employeeId, profileUrl);

        // Assert: Same set of skills returned (no duplicates)
        Set<String> firstSkillNames = firstResult.skills().stream()
                .map(ImportResult.ImportedSkill::name)
                .collect(Collectors.toSet());
        Set<String> secondSkillNames = secondResult.skills().stream()
                .map(ImportResult.ImportedSkill::name)
                .collect(Collectors.toSet());

        assertThat(secondSkillNames).isEqualTo(firstSkillNames);
        assertThat(secondResult.skills()).hasSameSizeAs(firstResult.skills());

        // No duplicate skill names in either result
        assertThat(firstResult.skills().stream().map(ImportResult.ImportedSkill::name).distinct().count())
                .isEqualTo(firstResult.skills().size());
        assertThat(secondResult.skills().stream().map(ImportResult.ImportedSkill::name).distinct().count())
                .isEqualTo(secondResult.skills().size());
    }

    // ========================================================================
    // Feature: github-profile-import, Property 12: Stale Skill Removal
    // ========================================================================

    /**
     * Property 12: Stale Skill Removal
     *
     * For any employee with existing source="GITHUB" skills, after an import that detects
     * a strict subset of the previously imported languages, the resulting skills SHALL
     * contain only the currently detected languages. Skills for languages no longer
     * detected SHALL be removed.
     *
     * **Validates: Requirements 6.7**
     */
    @Property(tries = 50)
    void staleSkillsAreRemovedWhenLanguagesNoLongerDetected(
            @ForAll("staleSkillScenarios") StaleSkillScenario scenario
    ) {
        // Arrange mocks
        UUID employeeId = UUID.randomUUID();
        String githubUrl = "https://github.com/" + scenario.username();

        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        GitHubApiClient gitHubApiClient = mock(GitHubApiClient.class);
        SkillRepository skillRepository = mock(SkillRepository.class);
        PortfolioLinkRepository portfolioLinkRepository = mock(PortfolioLinkRepository.class);

        GitHubImportProperties properties = new GitHubImportProperties(
                new GitHubImportProperties.Api("https://api.github.com", "test-pat"),
                Map.of()
        );

        // Mock employee exists and is active
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setActive(true);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        // Build existing GITHUB skills (superset of what will be imported)
        List<Skill> existingSkills = new ArrayList<>();
        for (String skillName : scenario.allPreviousSkillNames()) {
            Skill skill = new Skill();
            skill.setEmployeeId(employeeId);
            skill.setName(skillName);
            skill.setSource("GITHUB");
            skill.setProjectCount(1);
            skill.setProficiency("INTERMEDIATE");
            setSkillId(skill, UUID.nameUUIDFromBytes(skillName.getBytes()));
            existingSkills.add(skill);
        }

        // Mock findByEmployeeIdAndSource to return all existing GITHUB skills
        when(skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB"))
                .thenReturn(existingSkills);

        // Mock findByEmployeeIdAndNameAndSource — return existing skill if present
        for (Skill existingSkill : existingSkills) {
            when(skillRepository.findByEmployeeIdAndNameAndSource(
                    employeeId, existingSkill.getName(), "GITHUB"))
                    .thenReturn(Optional.of(existingSkill));
        }

        // Mock save to return the skill as-is
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> {
            Skill s = invocation.getArgument(0);
            if (s.getId() == null) {
                setSkillId(s, UUID.randomUUID());
            }
            return s;
        });

        // Mock GitHub API to return repos with only the SUBSET of languages
        List<GitHubRepo> repos = List.of(
                new GitHubRepo("repo-1", scenario.username() + "/repo-1",
                        new GitHubRepo.Owner(scenario.username()))
        );
        when(gitHubApiClient.fetchPublicRepos(scenario.username())).thenReturn(repos);

        // Build language map containing only the subset languages
        Map<String, Long> subsetLanguages = new LinkedHashMap<>();
        long byteCount = 100000L;
        for (String lang : scenario.currentSubsetSkillNames()) {
            subsetLanguages.put(lang, byteCount);
            byteCount -= 1000L;
        }
        when(gitHubApiClient.fetchLanguages(scenario.username(), "repo-1"))
                .thenReturn(subsetLanguages);

        // Mock PortfolioLink
        when(portfolioLinkRepository.findByEmployeeIdAndLabel(employeeId, "GitHub"))
                .thenReturn(Optional.of(new PortfolioLink()));
        when(portfolioLinkRepository.save(any(PortfolioLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        GitHubImportServiceImpl service = new GitHubImportServiceImpl(
                gitHubApiClient, skillRepository, portfolioLinkRepository,
                properties, employeeRepository
        );
        ImportResult result = service.importFromGitHub(employeeId, githubUrl);

        // Assert: skills in result contain ONLY the currently detected languages
        Set<String> resultSkillNames = result.skills().stream()
                .map(ImportResult.ImportedSkill::name)
                .collect(Collectors.toSet());
        assertThat(resultSkillNames)
                .as("Result should contain only currently detected languages")
                .containsExactlyInAnyOrderElementsOf(scenario.currentSubsetSkillNames());

        // Assert: stale skills (those in previous but not in current) were deleted
        Set<String> expectedStaleNames = new HashSet<>(scenario.allPreviousSkillNames());
        expectedStaleNames.removeAll(scenario.currentSubsetSkillNames());

        for (String staleName : expectedStaleNames) {
            Skill staleSkill = existingSkills.stream()
                    .filter(s -> s.getName().equals(staleName))
                    .findFirst()
                    .orElseThrow();
            verify(skillRepository).delete(staleSkill);
        }

        // Assert: non-stale skills were NOT deleted
        for (String keptName : scenario.currentSubsetSkillNames()) {
            Skill keptSkill = existingSkills.stream()
                    .filter(s -> s.getName().equals(keptName))
                    .findFirst()
                    .orElseThrow();
            verify(skillRepository, never()).delete(keptSkill);
        }
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<List<Map<String, Long>>> languageMapsForRepos() {
        Arbitrary<String> languageNames = Arbitraries.of(
                "Java", "Python", "JavaScript", "TypeScript", "Go",
                "Rust", "C", "C++", "Ruby", "Kotlin", "Swift", "Scala"
        );

        Arbitrary<Long> byteCounts = Arbitraries.longs().between(100L, 500_000L);

        Arbitrary<Map<String, Long>> singleRepoMap = Arbitraries.maps(languageNames, byteCounts)
                .ofMinSize(1)
                .ofMaxSize(5);

        return singleRepoMap.list().ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<StaleSkillScenario> staleSkillScenarios() {
        Arbitrary<String> usernames = Arbitraries.of(
                "octocat", "torvalds", "gaearon", "mojombo", "defunkt"
        );

        Arbitrary<List<String>> allSkills = Arbitraries.of(
                "Java", "Python", "Go", "Rust", "TypeScript",
                "JavaScript", "C", "Ruby", "Kotlin", "Swift",
                "Haskell", "Scala", "Elixir", "Clojure"
        ).list().ofMinSize(3).ofMaxSize(8).uniqueElements();

        return Combinators.combine(usernames, allSkills).as((username, skills) -> {
            int subsetSize = Math.max(1, skills.size() / 2);
            List<String> subset = skills.subList(0, subsetSize);
            return new StaleSkillScenario(username, new ArrayList<>(skills), new ArrayList<>(subset));
        }).filter(s -> s.currentSubsetSkillNames().size() < s.allPreviousSkillNames().size());
    }

    // ========================================================================
    // Helper types
    // ========================================================================

    record StaleSkillScenario(
            String username,
            List<String> allPreviousSkillNames,
            List<String> currentSubsetSkillNames
    ) {}

    // ========================================================================
    // Feature: github-profile-import, Property 14: GitHub Link Upsert Idempotency
    // ========================================================================

    /**
     * Property 14: GitHub Link Upsert Idempotency
     *
     * For any employee, importing with a GitHub profile URL SHALL result in exactly one
     * PortfolioLink with label "GitHub" for that employee, regardless of how many times
     * the import is run.
     *
     * **Validates: Requirements 7.1, 7.2**
     */
    @Property(tries = 50)
    void githubLinkUpsertIdempotency(
            @ForAll("validGitHubUsernames") String username,
            @ForAll("importRunCounts") int importRuns
    ) {
        // Arrange
        UUID employeeId = UUID.randomUUID();
        String githubProfileUrl = "https://github.com/" + username;

        GitHubApiClient apiClient = mock(GitHubApiClient.class);
        SkillRepository skillRepository = mock(SkillRepository.class);
        PortfolioLinkRepository portfolioLinkRepository = mock(PortfolioLinkRepository.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);

        GitHubImportProperties properties = new GitHubImportProperties(
                new GitHubImportProperties.Api("https://api.github.com", "test-pat"),
                Map.of()
        );

        // Mock active employee
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setActive(true);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        // Mock GitHub API — one repo with one language
        GitHubRepo repo = new GitHubRepo("test-repo", username + "/test-repo",
                new GitHubRepo.Owner(username));
        when(apiClient.fetchPublicRepos(username)).thenReturn(List.of(repo));
        when(apiClient.fetchLanguages(username, "test-repo"))
                .thenReturn(Map.of("Java", 10000L));

        // Mock skill repository
        when(skillRepository.findByEmployeeIdAndNameAndSource(eq(employeeId), anyString(), eq("GITHUB")))
                .thenReturn(Optional.empty());
        when(skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB"))
                .thenReturn(new ArrayList<>());
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> {
            Skill skill = invocation.getArgument(0);
            if (skill.getId() == null) {
                setSkillId(skill, UUID.randomUUID());
            }
            return skill;
        });

        // Simulate PortfolioLink storage: track all saved links
        List<PortfolioLink> storedLinks = new ArrayList<>();

        when(portfolioLinkRepository.findByEmployeeIdAndLabel(employeeId, "GitHub"))
                .thenAnswer(invocation -> storedLinks.stream()
                        .filter(l -> l.getEmployeeId().equals(employeeId)
                                && "GitHub".equals(l.getLabel()))
                        .findFirst());

        when(portfolioLinkRepository.save(any(PortfolioLink.class))).thenAnswer(invocation -> {
            PortfolioLink link = invocation.getArgument(0);
            if (link.getId() != null) {
                // Update: replace existing
                storedLinks.removeIf(l -> l.getId().equals(link.getId()));
            } else {
                // New link: assign ID
                link.setId(UUID.randomUUID());
            }
            storedLinks.add(link);
            return link;
        });

        // Act: run import multiple times
        GitHubImportServiceImpl service = new GitHubImportServiceImpl(
                apiClient, skillRepository, portfolioLinkRepository, properties, employeeRepository
        );

        for (int i = 0; i < importRuns; i++) {
            service.importFromGitHub(employeeId, githubProfileUrl);
        }

        // Assert: exactly one PortfolioLink with label "GitHub" exists
        long githubLinkCount = storedLinks.stream()
                .filter(l -> l.getEmployeeId().equals(employeeId)
                        && "GitHub".equals(l.getLabel()))
                .count();

        assertThat(githubLinkCount)
                .as("Exactly one PortfolioLink with label 'GitHub' should exist after %d imports", importRuns)
                .isEqualTo(1);

        // Assert: URL is correct
        PortfolioLink theLink = storedLinks.stream()
                .filter(l -> l.getEmployeeId().equals(employeeId)
                        && "GitHub".equals(l.getLabel()))
                .findFirst()
                .orElseThrow();
        assertThat(theLink.getUrl()).isEqualTo(githubProfileUrl);
    }

    @Provide
    Arbitrary<String> validGitHubUsernames() {
        Arbitrary<String> alphanumOnly = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(20);

        Arbitrary<String> withHyphens = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars('-')
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !s.startsWith("-"))
                .filter(s -> !s.endsWith("-"))
                .filter(s -> !s.contains("--"));

        return Arbitraries.oneOf(alphanumOnly, withHyphens);
    }

    @Provide
    Arbitrary<Integer> importRunCounts() {
        return Arbitraries.integers().between(1, 5);
    }

    // ========================================================================
    // Utility
    // ========================================================================

    private static void setSkillId(Skill skill, UUID id) {
        try {
            Field idField = Skill.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(skill, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set skill ID via reflection", e);
        }
    }
}
