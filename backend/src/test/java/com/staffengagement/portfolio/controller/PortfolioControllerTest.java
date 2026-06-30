package com.staffengagement.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffengagement.auth.service.JwtService;
import com.staffengagement.config.TestSecurityConfig;
import com.staffengagement.portfolio.dto.*;
import com.staffengagement.portfolio.service.PortfolioService;
import com.staffengagement.staff.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
@Import(TestSecurityConfig.class)
class PortfolioControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PortfolioService portfolioService;
    @MockitoBean private StaffRepository staffRepository;
    @MockitoBean private JwtService jwtService;

    private final UUID employeeId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();

    // ==================== Skills ====================

    @Test
    void getSkillsByEmployee_shouldReturn200() throws Exception {
        var skill = new PortfolioSkillResponse(UUID.randomUUID(), employeeId, "Java", 5, 3, "Advanced", now);
        when(portfolioService.getSkillsByEmployee(employeeId)).thenReturn(List.of(skill));

        mockMvc.perform(get("/api/portfolios/" + employeeId + "/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"));
    }

    // ==================== Education ====================

    @Test
    void createEducation_shouldReturn201() throws Exception {
        var request = new CreateEducationRequest("MIT", "BSc", "CS", LocalDate.of(2020, 6, 15));
        var response = new EducationResponse(UUID.randomUUID(), employeeId, "MIT", "BSc", "CS", LocalDate.of(2020, 6, 15), now);
        when(portfolioService.createEducation(eq(employeeId), any())).thenReturn(response);

        mockMvc.perform(post("/api/portfolios/" + employeeId + "/education")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.institution").value("MIT"));
    }

    @Test
    void getEducationByEmployee_shouldReturn200() throws Exception {
        var response = new EducationResponse(UUID.randomUUID(), employeeId, "MIT", "BSc", "CS", LocalDate.of(2020, 6, 15), now);
        when(portfolioService.getEducationByEmployee(employeeId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/portfolios/" + employeeId + "/education"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].degree").value("BSc"));
    }

    @Test
    void updateEducation_shouldReturn200() throws Exception {
        UUID educationId = UUID.randomUUID();
        var request = new UpdateEducationRequest("Harvard", "MSc", "AI", LocalDate.of(2022, 5, 1));
        var response = new EducationResponse(educationId, employeeId, "Harvard", "MSc", "AI", LocalDate.of(2022, 5, 1), now);
        when(portfolioService.updateEducation(eq(educationId), any())).thenReturn(response);

        mockMvc.perform(put("/api/portfolios/education/" + educationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institution").value("Harvard"));
    }

    @Test
    void deleteEducation_shouldReturn204() throws Exception {
        UUID educationId = UUID.randomUUID();
        doNothing().when(portfolioService).deleteEducation(educationId);

        mockMvc.perform(delete("/api/portfolios/education/" + educationId))
                .andExpect(status().isNoContent());
    }

    // ==================== Projects ====================

    @Test
    void createProject_shouldReturn201() throws Exception {
        var request = new CreateProjectRequest("Project X", "Desc", "Lead", List.of("Java", "Spring"), LocalDate.of(2023, 1, 1), null);
        var response = new ProjectResponse(UUID.randomUUID(), employeeId, "Project X", "Desc", "Lead", List.of("Java", "Spring"), LocalDate.of(2023, 1, 1), null, now);
        when(portfolioService.createProject(eq(employeeId), any())).thenReturn(response);

        mockMvc.perform(post("/api/portfolios/" + employeeId + "/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectName").value("Project X"));
    }

    @Test
    void getProjectsByEmployee_shouldReturn200() throws Exception {
        var response = new ProjectResponse(UUID.randomUUID(), employeeId, "Project X", "Desc", "Lead", List.of("Java"), LocalDate.of(2023, 1, 1), null, now);
        when(portfolioService.getProjectsByEmployee(employeeId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/portfolios/" + employeeId + "/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectName").value("Project X"));
    }

    @Test
    void updateProject_shouldReturn200() throws Exception {
        UUID projectId = UUID.randomUUID();
        var request = new UpdateProjectRequest("Updated", "New desc", "Dev", List.of("React"), LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));
        var response = new ProjectResponse(projectId, employeeId, "Updated", "New desc", "Dev", List.of("React"), LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), now);
        when(portfolioService.updateProject(eq(projectId), any())).thenReturn(response);

        mockMvc.perform(put("/api/portfolios/projects/" + projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectName").value("Updated"));
    }

    @Test
    void deleteProject_shouldReturn204() throws Exception {
        UUID projectId = UUID.randomUUID();
        doNothing().when(portfolioService).deleteProject(projectId);

        mockMvc.perform(delete("/api/portfolios/projects/" + projectId))
                .andExpect(status().isNoContent());
    }

    // ==================== Links ====================

    @Test
    void createLink_shouldReturn201() throws Exception {
        var request = new CreateLinkRequest("https://github.com/user", "GitHub");
        var response = new LinkResponse(UUID.randomUUID(), employeeId, "https://github.com/user", "GitHub", now);
        when(portfolioService.createLink(eq(employeeId), any())).thenReturn(response);

        mockMvc.perform(post("/api/portfolios/" + employeeId + "/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("GitHub"));
    }

    @Test
    void getLinksByEmployee_shouldReturn200() throws Exception {
        var response = new LinkResponse(UUID.randomUUID(), employeeId, "https://github.com/user", "GitHub", now);
        when(portfolioService.getLinksByEmployee(employeeId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/portfolios/" + employeeId + "/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].url").value("https://github.com/user"));
    }

    @Test
    void updateLink_shouldReturn200() throws Exception {
        UUID linkId = UUID.randomUUID();
        var request = new UpdateLinkRequest("https://linkedin.com/in/user", "LinkedIn");
        var response = new LinkResponse(linkId, employeeId, "https://linkedin.com/in/user", "LinkedIn", now);
        when(portfolioService.updateLink(eq(linkId), any())).thenReturn(response);

        mockMvc.perform(put("/api/portfolios/links/" + linkId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("LinkedIn"));
    }

    @Test
    void deleteLink_shouldReturn204() throws Exception {
        UUID linkId = UUID.randomUUID();
        doNothing().when(portfolioService).deleteLink(linkId);

        mockMvc.perform(delete("/api/portfolios/links/" + linkId))
                .andExpect(status().isNoContent());
    }

    // ==================== Full Portfolio ====================

    @Test
    void getFullPortfolio_shouldReturn200() throws Exception {
        var response = new FullPortfolioResponse(List.of(), List.of(), List.of(), List.of());
        when(portfolioService.getFullPortfolio(employeeId)).thenReturn(response);

        mockMvc.perform(get("/api/portfolios/" + employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills").isArray());
    }
}
