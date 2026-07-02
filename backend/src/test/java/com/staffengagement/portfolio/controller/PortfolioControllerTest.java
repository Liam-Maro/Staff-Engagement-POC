package com.staffengagement.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffengagement.auth.service.JwtService;
import com.staffengagement.portfolio.dto.*;
import com.staffengagement.portfolio.github.GitHubImportService;
import com.staffengagement.portfolio.service.PortfolioService;
import com.staffengagement.shared.config.JwtAuthenticationFilter;
import com.staffengagement.shared.config.SecurityConfig;
import com.staffengagement.shared.exception.GlobalExceptionHandler;
import com.staffengagement.staff.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class PortfolioControllerTest {

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

    // ==================== Skills ====================

    @Test
    void getSkillsByEmployee_returns200WithSkillsList() throws Exception {
        UUID employeeId = UUID.randomUUID();
        var skill = new PortfolioSkillResponse(UUID.randomUUID(), employeeId, "Java", 5, 3, "Advanced", LocalDateTime.now());

        when(portfolioService.getSkillsByEmployee(employeeId)).thenReturn(List.of(skill));

        mockMvc.perform(get("/api/portfolios/{employeeId}/skills", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[0].yearsExperience").value(5))
                .andExpect(jsonPath("$[0].proficiency").value("Advanced"));

        verify(portfolioService).getSkillsByEmployee(employeeId);
    }

    // ==================== Education ====================

    @Test
    void getEducationByEmployee_returns200WithList() throws Exception {
        UUID employeeId = UUID.randomUUID();
        var education = new EducationResponse(UUID.randomUUID(), employeeId, "MIT", "BSc",
                "Computer Science", LocalDate.of(2020, 6, 15), LocalDateTime.now());

        when(portfolioService.getEducationByEmployee(employeeId)).thenReturn(List.of(education));

        mockMvc.perform(get("/api/portfolios/{employeeId}/education", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].institution").value("MIT"))
                .andExpect(jsonPath("$[0].degree").value("BSc"));

        verify(portfolioService).getEducationByEmployee(employeeId);
    }

    @Test
    void createEducation_returns201WithResponse() throws Exception {
        UUID employeeId = UUID.randomUUID();
        var request = new CreateEducationRequest("MIT", "BSc", "Computer Science", LocalDate.of(2020, 6, 15));
        var response = new EducationResponse(UUID.randomUUID(), employeeId, "MIT", "BSc",
                "Computer Science", LocalDate.of(2020, 6, 15), LocalDateTime.now());

        when(portfolioService.createEducation(eq(employeeId), any(CreateEducationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/portfolios/{employeeId}/education", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.institution").value("MIT"))
                .andExpect(jsonPath("$.degree").value("BSc"));

        verify(portfolioService).createEducation(eq(employeeId), any(CreateEducationRequest.class));
    }

    @Test
    void updateEducation_returns200WithResponse() throws Exception {
        UUID educationId = UUID.randomUUID();
        var request = new UpdateEducationRequest("Harvard", "MSc", "AI", LocalDate.of(2022, 5, 20));
        var response = new EducationResponse(educationId, UUID.randomUUID(), "Harvard", "MSc",
                "AI", LocalDate.of(2022, 5, 20), LocalDateTime.now());

        when(portfolioService.updateEducation(eq(educationId), any(UpdateEducationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/portfolios/education/{educationId}", educationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institution").value("Harvard"))
                .andExpect(jsonPath("$.degree").value("MSc"));

        verify(portfolioService).updateEducation(eq(educationId), any(UpdateEducationRequest.class));
    }

    @Test
    void deleteEducation_returns204() throws Exception {
        UUID educationId = UUID.randomUUID();

        mockMvc.perform(delete("/api/portfolios/education/{educationId}", educationId))
                .andExpect(status().isNoContent());

        verify(portfolioService).deleteEducation(educationId);
    }

    // ==================== Projects ====================

    @Test
    void getProjectsByEmployee_returns200WithList() throws Exception {
        UUID employeeId = UUID.randomUUID();
        var project = new ProjectResponse(UUID.randomUUID(), employeeId, "Staff Engagement",
                "A POC", "Developer", List.of("Java", "Angular"),
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), LocalDateTime.now());

        when(portfolioService.getProjectsByEmployee(employeeId)).thenReturn(List.of(project));

        mockMvc.perform(get("/api/portfolios/{employeeId}/projects", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectName").value("Staff Engagement"))
                .andExpect(jsonPath("$[0].role").value("Developer"));

        verify(portfolioService).getProjectsByEmployee(employeeId);
    }

    @Test
    void createProject_returns201WithResponse() throws Exception {
        UUID employeeId = UUID.randomUUID();
        var request = new CreateProjectRequest("Staff Engagement", "A POC", "Developer",
                List.of("Java", "Angular"), LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));
        var response = new ProjectResponse(UUID.randomUUID(), employeeId, "Staff Engagement",
                "A POC", "Developer", List.of("Java", "Angular"),
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), LocalDateTime.now());

        when(portfolioService.createProject(eq(employeeId), any(CreateProjectRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/portfolios/{employeeId}/projects", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectName").value("Staff Engagement"))
                .andExpect(jsonPath("$.role").value("Developer"));

        verify(portfolioService).createProject(eq(employeeId), any(CreateProjectRequest.class));
    }

    // ==================== Links ====================

    @Test
    void getLinksByEmployee_returns200WithList() throws Exception {
        UUID employeeId = UUID.randomUUID();
        var link = new LinkResponse(UUID.randomUUID(), employeeId,
                "https://github.com/johndoe", "GitHub", LocalDateTime.now());

        when(portfolioService.getLinksByEmployee(employeeId)).thenReturn(List.of(link));

        mockMvc.perform(get("/api/portfolios/{employeeId}/links", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].url").value("https://github.com/johndoe"))
                .andExpect(jsonPath("$[0].label").value("GitHub"));

        verify(portfolioService).getLinksByEmployee(employeeId);
    }

    @Test
    void createLink_returns201WithResponse() throws Exception {
        UUID employeeId = UUID.randomUUID();
        var request = new CreateLinkRequest("https://github.com/johndoe", "GitHub");
        var response = new LinkResponse(UUID.randomUUID(), employeeId,
                "https://github.com/johndoe", "GitHub", LocalDateTime.now());

        when(portfolioService.createLink(eq(employeeId), any(CreateLinkRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/portfolios/{employeeId}/links", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("https://github.com/johndoe"))
                .andExpect(jsonPath("$.label").value("GitHub"));

        verify(portfolioService).createLink(eq(employeeId), any(CreateLinkRequest.class));
    }

    // ==================== Full Portfolio ====================

    @Test
    void getFullPortfolio_returns200WithAggregatedData() throws Exception {
        UUID employeeId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        var skills = List.of(new PortfolioSkillResponse(UUID.randomUUID(), employeeId,
                "Java", 5, 3, "Advanced", now));
        var education = List.of(new EducationResponse(UUID.randomUUID(), employeeId,
                "MIT", "BSc", "CS", LocalDate.of(2020, 6, 15), now));
        var projects = List.of(new ProjectResponse(UUID.randomUUID(), employeeId,
                "Project", "Desc", "Dev", List.of("Java"),
                LocalDate.of(2023, 1, 1), null, now));
        var links = List.of(new LinkResponse(UUID.randomUUID(), employeeId,
                "https://github.com/johndoe", "GitHub", now));

        var fullPortfolio = new FullPortfolioResponse(skills, education, projects, links);
        when(portfolioService.getFullPortfolio(employeeId)).thenReturn(fullPortfolio);

        mockMvc.perform(get("/api/portfolios/{employeeId}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills[0].name").value("Java"))
                .andExpect(jsonPath("$.education[0].institution").value("MIT"))
                .andExpect(jsonPath("$.projects[0].projectName").value("Project"))
                .andExpect(jsonPath("$.links[0].label").value("GitHub"));

        verify(portfolioService).getFullPortfolio(employeeId);
    }
}
