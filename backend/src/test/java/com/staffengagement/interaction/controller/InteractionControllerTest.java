package com.staffengagement.interaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffengagement.config.TestSecurityConfig;
import com.staffengagement.auth.service.JwtService;
import com.staffengagement.interaction.dto.CreateFollowUpTaskRequest;
import com.staffengagement.interaction.dto.CreateInteractionRequest;
import com.staffengagement.interaction.dto.InteractionResponse;
import com.staffengagement.interaction.dto.UpdateInteractionRequest;
import com.staffengagement.interaction.model.InteractionType;
import com.staffengagement.interaction.service.InteractionService;
import com.staffengagement.staff.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InteractionController.class)
@Import(TestSecurityConfig.class)
class InteractionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private InteractionService service;
    @MockitoBean private StaffRepository staffRepository;
    @MockitoBean private JwtService jwtService;

    @Test
    void findAll_shouldReturn200() throws Exception {
        var now = LocalDateTime.now();
        var response = new InteractionResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                InteractionType.CHECK_IN, "Notes", now, now, now);
        Page<InteractionResponse> page = new PageImpl<>(List.of(response));
        when(service.findAll(any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/interactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].notes").value("Notes"));
    }

    @Test
    void findById_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        var now = LocalDateTime.now();
        var response = new InteractionResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                InteractionType.MENTORING, "Mentoring notes", now, now, now);
        when(service.findById(id)).thenReturn(response);

        mockMvc.perform(get("/api/interactions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Mentoring notes"));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        UUID empId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        var occurredAt = LocalDateTime.now().minusDays(1);
        var request = new CreateInteractionRequest(empId, staffId, InteractionType.CHECK_IN, "Some notes", occurredAt);
        var now = LocalDateTime.now();
        var response = new InteractionResponse(UUID.randomUUID(), empId, staffId,
                InteractionType.CHECK_IN, "Some notes", occurredAt, now, now);
        when(service.create(any(CreateInteractionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/interactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notes").value("Some notes"));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        var occurredAt = LocalDateTime.now().minusDays(2);
        var request = new UpdateInteractionRequest(InteractionType.MENTORING, "Updated notes", occurredAt);
        var now = LocalDateTime.now();
        var response = new InteractionResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                InteractionType.MENTORING, "Updated notes", occurredAt, now, now);
        when(service.update(eq(id), any(UpdateInteractionRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/interactions/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Updated notes"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(service).delete(id);

        mockMvc.perform(delete("/api/interactions/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void createFollowUpTask_shouldReturn201() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new CreateFollowUpTaskRequest("Follow up", "Description", LocalDate.now().plusDays(7));
        when(service.createFollowUpTask(eq(id), any())).thenReturn(Map.of("id", UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/interactions/" + id + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
