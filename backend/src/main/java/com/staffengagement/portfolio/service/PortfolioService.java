package com.staffengagement.portfolio.service;

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

import java.util.List;
import java.util.UUID;

public interface PortfolioService {

    // Skills (read-only — sourced from Skills Register module)
    List<PortfolioSkillResponse> getSkillsByEmployee(UUID employeeId);

    // Education
    EducationResponse createEducation(UUID employeeId, CreateEducationRequest request);
    List<EducationResponse> getEducationByEmployee(UUID employeeId);
    EducationResponse updateEducation(UUID educationId, UpdateEducationRequest request);
    void deleteEducation(UUID educationId);

    // Projects
    ProjectResponse createProject(UUID employeeId, CreateProjectRequest request);
    List<ProjectResponse> getProjectsByEmployee(UUID employeeId);
    ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request);
    void deleteProject(UUID projectId);

    // Links
    LinkResponse createLink(UUID employeeId, CreateLinkRequest request);
    List<LinkResponse> getLinksByEmployee(UUID employeeId);
    LinkResponse updateLink(UUID linkId, UpdateLinkRequest request);
    void deleteLink(UUID linkId);

    // Full portfolio
    FullPortfolioResponse getFullPortfolio(UUID employeeId);
}
