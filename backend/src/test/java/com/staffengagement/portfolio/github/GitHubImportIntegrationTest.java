package com.staffengagement.portfolio.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffengagement.BaseIntegrationTest;
import com.staffengagement.employee.model.Employee;
import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.portfolio.model.PortfolioLink;
import com.staffengagement.portfolio.repository.PortfolioLinkRepository;
import com.staffengagement.skills.model.Skill;
import com.staffengagement.skills.repository.SkillRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the full GitHub profile import workflow.
 * Uses Testcontainers (PostgreSQL) for real DB and MockWebServer for GitHub API.
 *
 * Validates Requirements: 2.1, 2.2, 3.1, 4.1, 6.1–6.7, 7.1, 7.2
 */
@AutoConfigureMockMvc(addFilters = false)
class GitHubImportIntegrationTest extends BaseIntegrationTest {

    private static MockWebServer mockGitHubServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private PortfolioLinkRepository portfolioLinkRepository;

    private UUID employeeId;

    @BeforeAll
    static void startMockServer() throws IOException {
        mockGitHubServer = new MockWebServer();
        mockGitHubServer.start();
    }

    @AfterAll
    static void stopMockServer() throws IOException {
        if (mockGitHubServer != null) {
            mockGitHubServer.shutdown();
        }
    }

    @DynamicPropertySource
    static void configureGitHub(DynamicPropertyRegistry registry) {
        registry.add("github.api.base-url", () -> mockGitHubServer.url("/").toString().replaceAll("/$", ""));
        registry.add("github.api.pat", () -> "test-pat-token");
        registry.add("github.language-mapping.Jupyter Notebook", () -> "Python");
        registry.add("github.language-mapping.Shell", () -> "Bash");
    }

    @BeforeEach
    void setUp() {
        // Clean up test data
        skillRepository.deleteAll();
        portfolioLinkRepository.deleteAll();

        // Create a test employee
        Employee employee = new Employee();
        employee.setFirstName("Test");
        employee.setLastName("User");
        employee.setEmail("test-github-" + UUID.randomUUID() + "@example.com");
        employee.setDepartment("Engineering");
        employee.setJobTitle("Developer");
        employee.setHireDate(LocalDate.of(2023, 1, 1));
        employee.setActive(true);
        employee = employeeRepository.save(employee);
        employeeId = employee.getId();
    }

    @AfterEach
    void cleanUp() {
        skillRepository.deleteAll();
        portfolioLinkRepository.deleteAll();
        if (employeeId != null) {
            employeeRepository.deleteById(employeeId);
        }
    }

    // ==================== 1. Full Happy Path ====================

    @Test
    @DisplayName("Full happy-path: POST github-import → fetch repos → fetch languages → create skills + PortfolioLink")
    void fullHappyPath_createsSkillsAndPortfolioLink() throws Exception {
        // Mock: GET /users/octocat/repos?type=public&per_page=100
        String reposJson = """
                [
                    {"name": "repo-one", "full_name": "octocat/repo-one", "owner": {"login": "octocat"}},
                    {"name": "repo-two", "full_name": "octocat/repo-two", "owner": {"login": "octocat"}}
                ]
                """;
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(reposJson));

        // Mock: GET /repos/octocat/repo-one/languages
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"Java\": 150000, \"Python\": 30000}"));

        // Mock: GET /repos/octocat/repo-two/languages
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"Java\": 50000, \"TypeScript\": 80000}"));

        // Execute import
        MvcResult result = mockMvc.perform(post("/api/portfolios/{employeeId}/github-import", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"https://github.com/octocat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoriesAnalysed").value(2))
                .andExpect(jsonPath("$.githubProfileUrl").value("https://github.com/octocat"))
                .andExpect(jsonPath("$.skills").isArray())
                .andReturn();

        // Verify skills in DB
        List<Skill> skills = skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB");
        assertThat(skills).hasSize(3);
        assertThat(skills).extracting(Skill::getName)
                .containsExactlyInAnyOrder("Java", "Python", "TypeScript");

        // Java: 200000 bytes (top) → EXPERT, 2 repos
        Skill javaSkill = skills.stream().filter(s -> s.getName().equals("Java")).findFirst().orElseThrow();
        assertThat(javaSkill.getProficiency()).isEqualTo("EXPERT");
        assertThat(javaSkill.getProjectCount()).isEqualTo(2);
        assertThat(javaSkill.getSource()).isEqualTo("GITHUB");

        // TypeScript: 80000 bytes (2nd) → ADVANCED, 1 repo
        Skill tsSkill = skills.stream().filter(s -> s.getName().equals("TypeScript")).findFirst().orElseThrow();
        assertThat(tsSkill.getProficiency()).isEqualTo("ADVANCED");
        assertThat(tsSkill.getProjectCount()).isEqualTo(1);

        // Python: 30000 bytes (3rd) → ADVANCED, 1 repo
        Skill pythonSkill = skills.stream().filter(s -> s.getName().equals("Python")).findFirst().orElseThrow();
        assertThat(pythonSkill.getProficiency()).isEqualTo("ADVANCED");
        assertThat(pythonSkill.getProjectCount()).isEqualTo(1);

        // Verify PortfolioLink created
        Optional<PortfolioLink> link = portfolioLinkRepository.findByEmployeeIdAndLabel(employeeId, "GitHub");
        assertThat(link).isPresent();
        assertThat(link.get().getUrl()).isEqualTo("https://github.com/octocat");
    }

    // ==================== 2. Re-import Idempotency ====================

    @Test
    @DisplayName("Re-import updates existing skills without creating duplicates")
    void reImport_updatesExistingSkills() throws Exception {
        // First import
        enqueueStandardRepoAndLanguageResponses();

        mockMvc.perform(post("/api/portfolios/{employeeId}/github-import", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"https://github.com/octocat\"}"))
                .andExpect(status().isOk());

        List<Skill> firstImportSkills = skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB");
        assertThat(firstImportSkills).hasSize(2);

        // Second import with different byte counts (simulating changed repos)
        String reposJson = """
                [
                    {"name": "repo-one", "full_name": "octocat/repo-one", "owner": {"login": "octocat"}}
                ]
                """;
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(reposJson));
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"Java\": 250000, \"Python\": 100000}"));

        mockMvc.perform(post("/api/portfolios/{employeeId}/github-import", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"https://github.com/octocat\"}"))
                .andExpect(status().isOk());

        // Verify no duplicates — still 2 skills (Java + Python), stale ones removed
        List<Skill> secondImportSkills = skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB");
        assertThat(secondImportSkills).hasSize(2);
        assertThat(secondImportSkills).extracting(Skill::getName)
                .containsExactlyInAnyOrder("Java", "Python");

        // Verify project counts updated
        Skill javaSkill = secondImportSkills.stream()
                .filter(s -> s.getName().equals("Java")).findFirst().orElseThrow();
        assertThat(javaSkill.getProjectCount()).isEqualTo(1);
    }

    // ==================== 3. Stale Skill Removal ====================

    @Test
    @DisplayName("Stale GITHUB skills are removed when language is no longer detected")
    void reImportWithFewerLanguages_removesStaleSkills() throws Exception {
        // First import with Java + Python + TypeScript
        String reposJson = """
                [
                    {"name": "repo-one", "full_name": "octocat/repo-one", "owner": {"login": "octocat"}},
                    {"name": "repo-two", "full_name": "octocat/repo-two", "owner": {"login": "octocat"}}
                ]
                """;
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(reposJson));
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"Java\": 100000, \"Python\": 50000}"));
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"TypeScript\": 80000}"));

        mockMvc.perform(post("/api/portfolios/{employeeId}/github-import", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"https://github.com/octocat\"}"))
                .andExpect(status().isOk());

        assertThat(skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB")).hasSize(3);

        // Second import: only Java detected (Python and TypeScript gone)
        String reposJson2 = """
                [
                    {"name": "repo-one", "full_name": "octocat/repo-one", "owner": {"login": "octocat"}}
                ]
                """;
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(reposJson2));
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"Java\": 200000}"));

        mockMvc.perform(post("/api/portfolios/{employeeId}/github-import", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"https://github.com/octocat\"}"))
                .andExpect(status().isOk());

        // Verify stale skills removed
        List<Skill> remainingSkills = skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB");
        assertThat(remainingSkills).hasSize(1);
        assertThat(remainingSkills.get(0).getName()).isEqualTo("Java");
        assertThat(remainingSkills.get(0).getProficiency()).isEqualTo("EXPERT");
    }

    // ==================== 4. PortfolioLink Update (no duplicate) ====================

    @Test
    @DisplayName("PortfolioLink is updated on re-import, not duplicated")
    void reImport_updatesPortfolioLink_noDuplicate() throws Exception {
        // First import
        enqueueStandardRepoAndLanguageResponses();

        mockMvc.perform(post("/api/portfolios/{employeeId}/github-import", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"https://github.com/octocat\"}"))
                .andExpect(status().isOk());

        Optional<PortfolioLink> firstLink = portfolioLinkRepository.findByEmployeeIdAndLabel(employeeId, "GitHub");
        assertThat(firstLink).isPresent();
        assertThat(firstLink.get().getUrl()).isEqualTo("https://github.com/octocat");

        // Second import with different URL
        enqueueStandardRepoAndLanguageResponses();

        mockMvc.perform(post("/api/portfolios/{employeeId}/github-import", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"https://github.com/octocat\"}"))
                .andExpect(status().isOk());

        // Verify only one PortfolioLink exists (no duplicate)
        List<PortfolioLink> allLinks = portfolioLinkRepository.findByEmployeeId(employeeId);
        long gitHubLinks = allLinks.stream()
                .filter(l -> "GitHub".equals(l.getLabel()))
                .count();
        assertThat(gitHubLinks).isEqualTo(1);
    }

    // ==================== 5. Pagination Handling ====================

    @Test
    @DisplayName("Pagination: follows Link header to fetch all pages of repos")
    void pagination_followsLinkHeader() throws Exception {
        // Page 1: returns repos with Link header pointing to page 2
        String page1Repos = """
                [
                    {"name": "repo-page1", "full_name": "octocat/repo-page1", "owner": {"login": "octocat"}}
                ]
                """;
        String page2Url = mockGitHubServer.url("/users/octocat/repos?type=public&per_page=100&page=2").toString();
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .addHeader("Link", "<" + page2Url + ">; rel=\"next\"")
                .setBody(page1Repos));

        // Page 2: returns another repo, no Link header (last page)
        String page2Repos = """
                [
                    {"name": "repo-page2", "full_name": "octocat/repo-page2", "owner": {"login": "octocat"}}
                ]
                """;
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(page2Repos));

        // Languages for repo-page1
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"Go\": 120000}"));

        // Languages for repo-page2
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"Rust\": 90000}"));

        mockMvc.perform(post("/api/portfolios/{employeeId}/github-import", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"https://github.com/octocat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoriesAnalysed").value(2))
                .andExpect(jsonPath("$.skills").isArray());

        // Verify both languages from both pages are created as skills
        List<Skill> skills = skillRepository.findByEmployeeIdAndSource(employeeId, "GITHUB");
        assertThat(skills).hasSize(2);
        assertThat(skills).extracting(Skill::getName)
                .containsExactlyInAnyOrder("Go", "Rust");
    }

    // ==================== Helper Methods ====================

    /**
     * Enqueues a standard set of mock responses: 1 repo with Java + Python.
     */
    private void enqueueStandardRepoAndLanguageResponses() {
        String reposJson = """
                [
                    {"name": "repo-one", "full_name": "octocat/repo-one", "owner": {"login": "octocat"}}
                ]
                """;
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(reposJson));
        mockGitHubServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"Java\": 150000, \"Python\": 30000}"));
    }
}
