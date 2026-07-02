package com.staffengagement.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffengagement.auth.service.JwtService;
import com.staffengagement.portfolio.github.*;
import com.staffengagement.portfolio.service.PortfolioService;
import com.staffengagement.shared.config.JwtAuthenticationFilter;
import com.staffengagement.shared.config.SecurityConfig;
import com.staffengagement.shared.exception.GlobalExceptionHandler;
import com.staffengagement.staff.repository.StaffRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class PortfolioControllerGitHubImportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PortfolioService portfolioService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private StaffRepository staffRepository;

    @MockitoBean
    private GitHubImportService gitHubImportService;

    private static final String ENDPOINT = "/api/portfolios/{employeeId}/github-import";

    // ==================== Valid Request ====================

    @Test
    void importGitHub_validRequest_returns200WithImportResult() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String url = "https://github.com/octocat";

        var importResult = new ImportResult(
                List.of(new ImportResult.ImportedSkill(
                        UUID.randomUUID(), "Java", 5, "EXPERT", "GITHUB"
                )),
                url,
                10,
                List.of()
        );

        when(gitHubImportService.importFromGitHub(eq(employeeId), eq(url)))
                .thenReturn(importResult);

        mockMvc.perform(post(ENDPOINT, employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"" + url + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.githubProfileUrl").value(url))
                .andExpect(jsonPath("$.repositoriesAnalysed").value(10))
                .andExpect(jsonPath("$.skills[0].name").value("Java"))
                .andExpect(jsonPath("$.skills[0].proficiency").value("EXPERT"))
                .andExpect(jsonPath("$.skills[0].source").value("GITHUB"))
                .andExpect(jsonPath("$.skills[0].projectCount").value(5))
                .andExpect(jsonPath("$.skippedRepositories").isEmpty());
    }

    // ==================== Missing Request Body ====================

    @Test
    void importGitHub_emptyJsonBody_returns400() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(post(ENDPOINT, employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void importGitHub_blankGithubProfileUrl_returns400() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(post(ENDPOINT, employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void importGitHub_nullGithubProfileUrl_returns400() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(post(ENDPOINT, employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    // ==================== Invalid GitHub URL ====================

    @Test
    void importGitHub_invalidUrl_returns400() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String invalidUrl = "https://gitlab.com/someuser";

        when(gitHubImportService.importFromGitHub(eq(employeeId), eq(invalidUrl)))
                .thenThrow(new InvalidGitHubUrlException(
                        "Invalid GitHub profile URL format. Expected: https://github.com/{username}"));

        mockMvc.perform(post(ENDPOINT, employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"" + invalidUrl + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Invalid GitHub profile URL format. Expected: https://github.com/{username}"));
    }

    // ==================== Invalid UUID ====================

    @Test
    void importGitHub_invalidUuidPath_returns400() throws Exception {
        mockMvc.perform(post("/api/portfolios/{employeeId}/github-import", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"https://github.com/octocat\"}"))
                .andExpect(status().isBadRequest());
    }

    // ==================== Employee Not Found ====================

    @Test
    void importGitHub_employeeNotFound_returns404() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String url = "https://github.com/octocat";

        when(gitHubImportService.importFromGitHub(eq(employeeId), eq(url)))
                .thenThrow(new EntityNotFoundException("Employee not found: " + employeeId));

        mockMvc.perform(post(ENDPOINT, employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"" + url + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found: " + employeeId));
    }

    // ==================== GitHub User Not Found ====================

    @Test
    void importGitHub_gitHubUserNotFound_returns404() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String url = "https://github.com/nonexistentuser";

        when(gitHubImportService.importFromGitHub(eq(employeeId), eq(url)))
                .thenThrow(new GitHubUserNotFoundException("nonexistentuser"));

        mockMvc.perform(post(ENDPOINT, employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"" + url + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("GitHub user not found: nonexistentuser"));
    }

    // ==================== GitHub Rate Limit ====================

    @Test
    void importGitHub_rateLimitExceeded_returns429() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String url = "https://github.com/octocat";
        Instant resetTime = Instant.parse("2025-01-15T12:00:00Z");

        when(gitHubImportService.importFromGitHub(eq(employeeId), eq(url)))
                .thenThrow(new GitHubRateLimitException(resetTime));

        mockMvc.perform(post(ENDPOINT, employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"" + url + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(
                        "GitHub API rate limit exceeded. Resets at: " + resetTime));
    }

    // ==================== GitHub API Unavailable ====================

    @Test
    void importGitHub_apiUnavailable_returns502() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String url = "https://github.com/octocat";

        when(gitHubImportService.importFromGitHub(eq(employeeId), eq(url)))
                .thenThrow(new GitHubApiUnavailableException(500));

        mockMvc.perform(post(ENDPOINT, employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"" + url + "\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("GitHub API is currently unavailable (HTTP 500)"));
    }

    // ==================== GitHub Timeout ====================

    @Test
    void importGitHub_timeout_returns504() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String url = "https://github.com/octocat";

        when(gitHubImportService.importFromGitHub(eq(employeeId), eq(url)))
                .thenThrow(new GitHubTimeoutException());

        mockMvc.perform(post(ENDPOINT, employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"" + url + "\"}"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value(504))
                .andExpect(jsonPath("$.message").value("GitHub API request timed out"));
    }

    // ==================== GitHub Not Configured ====================

    @Test
    void importGitHub_notConfigured_returns503() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String url = "https://github.com/octocat";

        when(gitHubImportService.importFromGitHub(eq(employeeId), eq(url)))
                .thenThrow(new GitHubNotConfiguredException());

        mockMvc.perform(post(ENDPOINT, employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubProfileUrl\":\"" + url + "\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value(
                        "GitHub integration is not configured (missing PAT or base URL)"));
    }
}
