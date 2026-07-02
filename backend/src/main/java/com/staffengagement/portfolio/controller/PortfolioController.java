package com.staffengagement.portfolio.controller;

import com.staffengagement.portfolio.dto.CreateEducationRequest;
import com.staffengagement.portfolio.dto.CreateLinkRequest;
import com.staffengagement.portfolio.dto.CreateProjectRequest;
import com.staffengagement.portfolio.dto.EducationResponse;
import com.staffengagement.portfolio.dto.FullPortfolioResponse;
import com.staffengagement.portfolio.dto.LinkResponse;
import com.staffengagement.portfolio.dto.PortfolioSkillResponse;
import com.staffengagement.portfolio.dto.ProjectResponse;
import com.staffengagement.portfolio.dto.UpdateEducationRequest;
import com.staffengagement.portfolio.dto.UpdateLinkRequest;
import com.staffengagement.portfolio.dto.UpdateProjectRequest;
import com.staffengagement.portfolio.github.GitHubImportRequest;
import com.staffengagement.portfolio.github.GitHubImportService;
import com.staffengagement.portfolio.github.ImportResult;
import com.staffengagement.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolios")
class PortfolioController {

    private final PortfolioService portfolioService;
    private final GitHubImportService gitHubImportService;

    PortfolioController(PortfolioService portfolioService, GitHubImportService gitHubImportService) {
        this.portfolioService = portfolioService;
        this.gitHubImportService = gitHubImportService;
    }

    // ==================== Skills (read-only, managed via /api/skills) ====================

    @GetMapping("/{employeeId}/skills")
    List<PortfolioSkillResponse> getSkillsByEmployee(@PathVariable UUID employeeId) {
        return portfolioService.getSkillsByEmployee(employeeId);
    }

    // ==================== Education ====================

    @PostMapping("/{employeeId}/education")
    @ResponseStatus(HttpStatus.CREATED)
    EducationResponse createEducation(@PathVariable UUID employeeId,
                                      @RequestBody @Valid CreateEducationRequest request) {
        return portfolioService.createEducation(employeeId, request);
    }

    @GetMapping("/{employeeId}/education")
    List<EducationResponse> getEducationByEmployee(@PathVariable UUID employeeId) {
        return portfolioService.getEducationByEmployee(employeeId);
    }

    @PutMapping("/education/{educationId}")
    EducationResponse updateEducation(@PathVariable UUID educationId,
                                      @RequestBody @Valid UpdateEducationRequest request) {
        return portfolioService.updateEducation(educationId, request);
    }

    @DeleteMapping("/education/{educationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteEducation(@PathVariable UUID educationId) {
        portfolioService.deleteEducation(educationId);
    }

    // ==================== Projects ====================

    @PostMapping("/{employeeId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    ProjectResponse createProject(@PathVariable UUID employeeId,
                                   @RequestBody @Valid CreateProjectRequest request) {
        return portfolioService.createProject(employeeId, request);
    }

    @GetMapping("/{employeeId}/projects")
    List<ProjectResponse> getProjectsByEmployee(@PathVariable UUID employeeId) {
        return portfolioService.getProjectsByEmployee(employeeId);
    }

    @PutMapping("/projects/{projectId}")
    ProjectResponse updateProject(@PathVariable UUID projectId,
                                   @RequestBody @Valid UpdateProjectRequest request) {
        return portfolioService.updateProject(projectId, request);
    }

    @DeleteMapping("/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteProject(@PathVariable UUID projectId) {
        portfolioService.deleteProject(projectId);
    }

    // ==================== Links ====================

    @PostMapping("/{employeeId}/links")
    @ResponseStatus(HttpStatus.CREATED)
    LinkResponse createLink(@PathVariable UUID employeeId,
                            @RequestBody @Valid CreateLinkRequest request) {
        return portfolioService.createLink(employeeId, request);
    }

    @GetMapping("/{employeeId}/links")
    List<LinkResponse> getLinksByEmployee(@PathVariable UUID employeeId) {
        return portfolioService.getLinksByEmployee(employeeId);
    }

    @PutMapping("/links/{linkId}")
    LinkResponse updateLink(@PathVariable UUID linkId,
                            @RequestBody @Valid UpdateLinkRequest request) {
        return portfolioService.updateLink(linkId, request);
    }

    @DeleteMapping("/links/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteLink(@PathVariable UUID linkId) {
        portfolioService.deleteLink(linkId);
    }

    // ==================== Full Portfolio ====================

    @GetMapping("/{employeeId}")
    FullPortfolioResponse getFullPortfolio(@PathVariable UUID employeeId) {
        return portfolioService.getFullPortfolio(employeeId);
    }

    // ==================== GitHub Import ====================

    @PostMapping("/{employeeId}/github-import")
    ImportResult importGitHubSkills(@PathVariable UUID employeeId,
                                    @RequestBody @Valid GitHubImportRequest request) {
        return gitHubImportService.importFromGitHub(employeeId, request.githubProfileUrl());
    }
}
