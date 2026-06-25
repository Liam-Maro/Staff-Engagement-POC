package com.staffengagement.staff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffengagement.auth.service.JwtService;
import com.staffengagement.shared.config.JwtAuthenticationFilter;
import com.staffengagement.shared.config.SecurityConfig;
import com.staffengagement.staff.dto.CreateStaffRequest;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.dto.UpdateStaffRequest;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.repository.StaffRepository;
import com.staffengagement.staff.service.StaffService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StaffController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class StaffControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean StaffService staffService;
    @MockitoBean JwtService jwtService;
    @MockitoBean StaffRepository staffRepository;

    private StaffResponse sampleResponse() {
        return new StaffResponse(UUID.randomUUID(), "john@example.com", StaffRole.STAFF, true, LocalDateTime.now());
    }

    // --- GET /api/staff ---

    @Test
    @WithMockUser(roles = "STAFF")
    void findAll_shouldReturn200_forStaffRole() throws Exception {
        when(staffService.findAll()).thenReturn(List.of(sampleResponse()));
        mockMvc.perform(get("/api/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("john@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findAll_shouldReturn200_forAdminRole() throws Exception {
        when(staffService.findAll()).thenReturn(List.of(sampleResponse()));
        mockMvc.perform(get("/api/staff"))
                .andExpect(status().isOk());
    }

    @Test
    void findAll_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/staff"))
                .andExpect(status().is4xxClientError());
    }

    // --- POST /api/staff ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_shouldReturn201_forAdminRole() throws Exception {
        when(staffService.create(any())).thenReturn(sampleResponse());
        var request = new CreateStaffRequest("john@example.com", "password123", StaffRole.STAFF);

        mockMvc.perform(post("/api/staff")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void create_shouldReturn403_forStaffRole() throws Exception {
        var request = new CreateStaffRequest("john@example.com", "password123", StaffRole.STAFF);

        mockMvc.perform(post("/api/staff")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- PUT /api/staff/{id} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_shouldReturn200_forAdminRole() throws Exception {
        UUID id = UUID.randomUUID();
        when(staffService.update(eq(id), any())).thenReturn(sampleResponse());
        var request = new UpdateStaffRequest(StaffRole.ADMIN, true);

        mockMvc.perform(put("/api/staff/" + id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void update_shouldReturn403_forStaffRole() throws Exception {
        var request = new UpdateStaffRequest(StaffRole.ADMIN, true);

        mockMvc.perform(put("/api/staff/" + UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- PATCH /api/staff/{id}/deactivate ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivate_shouldReturn204_forAdminRole() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(staffService).deactivate(id);

        mockMvc.perform(patch("/api/staff/" + id + "/deactivate").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void deactivate_shouldReturn403_forStaffRole() throws Exception {
        mockMvc.perform(patch("/api/staff/" + UUID.randomUUID() + "/deactivate").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // --- DELETE /api/staff/{id} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_shouldReturn204_forAdminRole() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(staffService).delete(id);

        mockMvc.perform(delete("/api/staff/" + id).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void delete_shouldReturn403_forStaffRole() throws Exception {
        mockMvc.perform(delete("/api/staff/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
