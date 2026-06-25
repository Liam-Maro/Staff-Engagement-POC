package com.staffengagement.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffengagement.auth.service.JwtService;
import com.staffengagement.shared.config.JwtAuthenticationFilter;
import com.staffengagement.shared.config.SecurityConfig;
import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.shared.exception.TaskAssignmentForbiddenException;
import com.staffengagement.staff.model.Staff;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.repository.StaffRepository;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.dto.UpdateStatusRequest;
import com.staffengagement.task.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TaskControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean TaskService taskService;
    @MockitoBean StaffService staffService;
    @MockitoBean JwtService jwtService;
    @MockitoBean StaffRepository staffRepository;

    private Staff authenticatedStaff;
    private UUID staffId;

    @BeforeEach
    void setUp() {
        staffId = UUID.randomUUID();
        authenticatedStaff = new Staff();
        authenticatedStaff.setEmail("test@example.com");
        authenticatedStaff.setPassword("password");
        authenticatedStaff.setRole(StaffRole.STAFF);
        authenticatedStaff.setActive(true);
        authenticatedStaff.setEmployeeId(UUID.randomUUID());
        // Use reflection to set the ID since there's no public setter
        setStaffId(authenticatedStaff, staffId);
    }

    private void setStaffId(Staff staff, UUID id) {
        try {
            var field = Staff.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(staff, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set staff ID", e);
        }
    }

    private TaskResponse sampleTaskResponse() {
        return new TaskResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                staffId,
                UUID.randomUUID(),
                "Test Task",
                "A test description",
                "OPEN",
                LocalDate.now().plusDays(7),
                LocalDateTime.now()
        );
    }

    // --- POST /api/tasks ---

    @Test
    void create_withValidPayload_returns201WithTaskResponse() throws Exception {
        TaskResponse response = sampleTaskResponse();
        when(taskService.create(any(CreateTaskRequest.class), eq(staffId))).thenReturn(response);

        CreateTaskRequest request = new CreateTaskRequest(
                UUID.randomUUID(), null, UUID.randomUUID(), "Valid Task", "Description", LocalDate.now().plusDays(5)
        );

        mockMvc.perform(post("/api/tasks")
                        .with(user(authenticatedStaff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.title").value("Test Task"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.creatorId").value(staffId.toString()));
    }

    @Test
    void create_withMissingTitle_returns400WithValidationErrors() throws Exception {
        String payload = """
                {
                    "employeeId": "%s",
                    "assigneeId": "%s",
                    "title": "",
                    "description": "Some description"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/tasks")
                        .with(user(authenticatedStaff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("title"))));
    }

    @Test
    void create_withNullAssigneeId_returns400() throws Exception {
        String payload = """
                {
                    "employeeId": "%s",
                    "assigneeId": null,
                    "title": "Some Task",
                    "description": "Description"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/tasks")
                        .with(user(authenticatedStaff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasItem(containsString("assigneeId"))));
    }

    @Test
    void create_withMultipleInvalidFields_returnsAllErrorsInSingle400() throws Exception {
        String payload = """
                {
                    "employeeId": null,
                    "assigneeId": null,
                    "title": "",
                    "description": "Description"
                }
                """.formatted();

        mockMvc.perform(post("/api/tasks")
                        .with(user(authenticatedStaff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.errors", hasItem(containsString("assigneeId"))))
                .andExpect(jsonPath("$.errors", hasItem(containsString("title"))));
    }

    // --- GET /api/tasks?assigneeId={valid-uuid} ---

    @Test
    void findAll_withValidAssigneeId_returnsFilteredResults() throws Exception {
        UUID assigneeId = UUID.randomUUID();
        TaskResponse response = sampleTaskResponse();
        when(taskService.findByAssigneeId(eq(assigneeId), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/tasks")
                        .with(user(authenticatedStaff))
                        .param("assigneeId", assigneeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Test Task"));
    }

    @Test
    void findAll_withInvalidAssigneeIdUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .with(user(authenticatedStaff))
                        .param("assigneeId", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Invalid UUID format")));
    }

    // --- GET /api/tasks?creatorId={valid-uuid} ---

    @Test
    void findAll_withValidCreatorId_returnsFilteredResults() throws Exception {
        UUID creatorId = UUID.randomUUID();
        TaskResponse response = sampleTaskResponse();
        when(staffService.findById(creatorId)).thenReturn(null);
        when(taskService.findByCreatorId(eq(creatorId), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/tasks")
                        .with(user(authenticatedStaff))
                        .param("creatorId", creatorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Test Task"));
    }

    @Test
    void findAll_withInvalidCreatorIdUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .with(user(authenticatedStaff))
                        .param("creatorId", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Invalid UUID format")));
    }

    // --- GET /api/tasks?sortOrder=invalid ---

    @Test
    void findAll_withInvalidSortOrder_returns400WithValidValuesListed() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .with(user(authenticatedStaff))
                        .param("assigneeId", UUID.randomUUID().toString())
                        .param("sortOrder", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("asc")))
                .andExpect(jsonPath("$.message", containsString("desc")));
    }

    // --- PATCH /api/tasks/{id}/status ---

    @Test
    void updateStatus_withValidAssignee_returns200() throws Exception {
        UUID taskId = UUID.randomUUID();
        TaskResponse response = new TaskResponse(
                taskId, UUID.randomUUID(), null, UUID.randomUUID(), staffId,
                "Task", "Desc", "IN_PROGRESS", null, LocalDateTime.now()
        );
        when(taskService.updateStatus(eq(taskId), eq("IN_PROGRESS"), eq(staffId))).thenReturn(response);

        UpdateStatusRequest request = new UpdateStatusRequest("IN_PROGRESS");

        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .with(user(authenticatedStaff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.id").value(taskId.toString()));
    }

    @Test
    void updateStatus_withNonAssignee_returns403() throws Exception {
        UUID taskId = UUID.randomUUID();
        when(taskService.updateStatus(eq(taskId), eq("IN_PROGRESS"), eq(staffId)))
                .thenThrow(new TaskAssignmentForbiddenException("Staff member is not the assignee of this task"));

        UpdateStatusRequest request = new UpdateStatusRequest("IN_PROGRESS");

        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .with(user(authenticatedStaff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message", containsString("not the assignee")));
    }

    @Test
    void updateStatus_withInvalidStatus_returns400() throws Exception {
        UUID taskId = UUID.randomUUID();
        when(taskService.updateStatus(eq(taskId), eq("INVALID_STATUS"), eq(staffId)))
                .thenThrow(new com.staffengagement.shared.exception.InvalidParameterException(
                        "Invalid status value: 'INVALID_STATUS'. Valid values are: OPEN, IN_PROGRESS, COMPLETED"));

        UpdateStatusRequest request = new UpdateStatusRequest("INVALID_STATUS");

        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .with(user(authenticatedStaff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("OPEN")))
                .andExpect(jsonPath("$.message", containsString("IN_PROGRESS")))
                .andExpect(jsonPath("$.message", containsString("COMPLETED")));
    }

    @Test
    void updateStatus_withNonExistentTaskId_returns404() throws Exception {
        UUID taskId = UUID.randomUUID();
        when(taskService.updateStatus(eq(taskId), eq("IN_PROGRESS"), eq(staffId)))
                .thenThrow(new EntityNotFoundException("Task not found with id: " + taskId));

        UpdateStatusRequest request = new UpdateStatusRequest("IN_PROGRESS");

        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .with(user(authenticatedStaff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("Task not found")));
    }
}
