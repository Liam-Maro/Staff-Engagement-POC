package com.staffengagement.skills.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffengagement.auth.service.JwtService;
import com.staffengagement.shared.config.JwtAuthenticationFilter;
import com.staffengagement.shared.config.SecurityConfig;
import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.shared.exception.GlobalExceptionHandler;
import com.staffengagement.skills.dto.CreateSkillRequest;
import com.staffengagement.skills.dto.SkillResponse;
import com.staffengagement.skills.dto.SkillSearchResult;
import com.staffengagement.skills.dto.UpdateSkillRequest;
import com.staffengagement.skills.service.SkillService;
import com.staffengagement.staff.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SkillController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SkillService skillService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private StaffRepository staffRepository;

    @Test
    void search_withValidQuery_returns200WithJsonArray() throws Exception {
        List<SkillSearchResult> results = List.of(
                new SkillSearchResult("John", "Doe", "john@example.com", "Angular", 5, 10, "Advanced")
        );
        when(skillService.search("Angular")).thenReturn(results);

        mockMvc.perform(get("/api/skills/search").param("query", "Angular"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].employeeFirstName").value("John"))
                .andExpect(jsonPath("$[0].skillName").value("Angular"))
                .andExpect(jsonPath("$[0].yearsExperience").value(5));
    }

    @Test
    void create_withValidBody_returns201() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        CreateSkillRequest request = new CreateSkillRequest(employeeId, "Java", 5, 10, "Advanced");
        SkillResponse response = new SkillResponse(skillId, employeeId, "Java", 5, 10, "Advanced", LocalDateTime.now());

        when(skillService.create(any(CreateSkillRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(skillId.toString()))
                .andExpect(jsonPath("$.name").value("Java"))
                .andExpect(jsonPath("$.proficiency").value("Advanced"));
    }

    @Test
    void create_withBlankName_returns400WithFieldErrors() throws Exception {
        UUID employeeId = UUID.randomUUID();
        CreateSkillRequest request = new CreateSkillRequest(employeeId, "", 5, 10, "Advanced");

        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void create_withInvalidProficiency_returns400() throws Exception {
        UUID employeeId = UUID.randomUUID();
        CreateSkillRequest request = new CreateSkillRequest(employeeId, "Java", 5, 10, "InvalidLevel");

        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void create_withYearsExperienceExceeding50_returns400() throws Exception {
        UUID employeeId = UUID.randomUUID();
        CreateSkillRequest request = new CreateSkillRequest(employeeId, "Java", 51, 10, "Advanced");

        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void create_withProjectCountExceeding500_returns400() throws Exception {
        UUID employeeId = UUID.randomUUID();
        CreateSkillRequest request = new CreateSkillRequest(employeeId, "Java", 5, 501, "Advanced");

        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void update_withValidBody_returns200() throws Exception {
        UUID skillId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UpdateSkillRequest request = new UpdateSkillRequest("Python", 3, 7, "Intermediate");
        SkillResponse response = new SkillResponse(skillId, employeeId, "Python", 3, 7, "Intermediate", LocalDateTime.now());

        when(skillService.update(eq(skillId), any(UpdateSkillRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/skills/{id}", skillId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(skillId.toString()))
                .andExpect(jsonPath("$.name").value("Python"))
                .andExpect(jsonPath("$.proficiency").value("Intermediate"));
    }

    @Test
    void update_withNonExistentId_returns404() throws Exception {
        UUID skillId = UUID.randomUUID();
        UpdateSkillRequest request = new UpdateSkillRequest("Python", 3, 7, "Intermediate");

        when(skillService.update(eq(skillId), any(UpdateSkillRequest.class)))
                .thenThrow(new EntityNotFoundException("Skill not found: " + skillId));

        mockMvc.perform(put("/api/skills/{id}", skillId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Skill not found: " + skillId));
    }

    @Test
    void delete_withExistingId_returns204() throws Exception {
        UUID skillId = UUID.randomUUID();
        doNothing().when(skillService).delete(skillId);

        mockMvc.perform(delete("/api/skills/{id}", skillId))
                .andExpect(status().isNoContent());
    }

    @Test
    void search_withEmptyQuery_returns400() throws Exception {
        mockMvc.perform(get("/api/skills/search").param("query", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void search_withQueryExceeding100Characters_returns400() throws Exception {
        String longQuery = "a".repeat(101);

        mockMvc.perform(get("/api/skills/search").param("query", longQuery))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }
}
