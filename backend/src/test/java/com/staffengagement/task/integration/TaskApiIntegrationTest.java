package com.staffengagement.task.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffengagement.employee.model.Employee;
import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.interaction.model.Interaction;
import com.staffengagement.interaction.repository.InteractionRepository;
import com.staffengagement.staff.model.Staff;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.repository.StaffRepository;
import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.dto.UpdateStatusRequest;
import com.staffengagement.task.dto.UpdateTaskRequest;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.repository.TaskRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Task API lifecycle.
 * Uses @SpringBootTest with Testcontainers PostgreSQL for a real database.
 * Security filters are bypassed via addFilters=false; authentication is
 * provided via SecurityMockMvcRequestPostProcessors.user().
 *
 * These tests require Docker to be running. They are automatically skipped
 * if Docker is not available.
 *
 * Validates: Requirements 1.1, 2.1, 2.6, 6.1, 7.9, 8.1, 9.1, 9.3, 10.3
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIf(value = "isDockerAvailable", disabledReason = "Docker is not available")
class TaskApiIntegrationTest {

    static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Staff creator;
    private Staff assignee;
    private Employee individual;

    @BeforeAll
    void setUpTestData() {
        taskRepository.deleteAll();
        interactionRepository.deleteAll();

        creator = new Staff();
        creator.setEmail("creator-integration@test.com");
        creator.setPassword(passwordEncoder.encode("password"));
        creator.setRole(StaffRole.STAFF);
        creator.setActive(true);
        creator = staffRepository.save(creator);

        assignee = new Staff();
        assignee.setEmail("assignee-integration@test.com");
        assignee.setPassword(passwordEncoder.encode("password"));
        assignee.setRole(StaffRole.STAFF);
        assignee.setActive(true);
        assignee = staffRepository.save(assignee);

        individual = new Employee();
        individual.setFirstName("Test");
        individual.setLastName("Individual");
        individual.setEmail("individual-integration@test.com");
        individual.setDepartment("Engineering");
        individual.setJobTitle("Engineer");
        individual.setActive(true);
        individual = employeeRepository.save(individual);
    }

    @AfterAll
    void tearDown() {
        taskRepository.deleteAll();
        interactionRepository.deleteAll();
    }

    @BeforeEach
    void cleanTasks() {
        taskRepository.deleteAll();
    }

    // =========================================================================
    // Test 1: Full Lifecycle — create → retrieve → filter → update status → edit → delete
    // Validates: Requirements 1.1, 2.1, 6.1, 8.1, 9.1
    // =========================================================================

    @Test
    @DisplayName("Full lifecycle: create → retrieve → filter → update status → edit → delete")
    void fullLifecycle() throws Exception {
        LocalDate dueDate = LocalDate.now().plusDays(7);

        // 1. CREATE via POST → 201
        var createRequest = new CreateTaskRequest(
                individual.getId(), null, assignee.getId(),
                "Integration test task description", dueDate
        );

        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .with(user(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.individualId").value(individual.getId().toString()))
                .andExpect(jsonPath("$.creatorId").value(creator.getId().toString()))
                .andExpect(jsonPath("$.assigneeId").value(assignee.getId().toString()))
                .andExpect(jsonPath("$.description").value("Integration test task description"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueDate").value(dueDate.toString()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        // 2. RETRIEVE via GET /{id} → 200
        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.description").value("Integration test task description"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.assigneeId").value(assignee.getId().toString()));

        // 3. FILTER — by assignee
        mockMvc.perform(get("/api/tasks")
                        .with(user(creator))
                        .param("assigneeId", assignee.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks", hasSize(1)))
                .andExpect(jsonPath("$.tasks[0].id").value(taskId));

        // 4. UPDATE STATUS via PATCH (assignee updates)
        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .with(user(assignee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest("IN_PROGRESS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 5. EDIT via PUT (creator edits)
        var updateRequest = new UpdateTaskRequest(
                individual.getId(), null, assignee.getId(),
                "Updated description via edit", dueDate.plusDays(3)
        );

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .with(user(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description via edit"))
                .andExpect(jsonPath("$.dueDate").value(dueDate.plusDays(3).toString()));

        // 6. DELETE via DELETE (creator deletes) → 204
        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .with(user(creator)))
                .andExpect(status().isNoContent());

        // Verify task is gone → 404
        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .with(user(creator)))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Test 2: Database constraint verification (FK constraints, NOT NULL)
    // =========================================================================

    @Test
    @DisplayName("Non-existent assignee returns 404 (FK constraint via service)")
    void databaseConstraints_rejectsNonExistentAssignee() throws Exception {
        UUID nonExistentStaffId = UUID.randomUUID();

        var request = new CreateTaskRequest(
                individual.getId(), null, nonExistentStaffId,
                "Task with bad assignee", null
        );

        mockMvc.perform(post("/api/tasks")
                        .with(user(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Null description rejected with 400 (NOT NULL)")
    void databaseConstraints_rejectsNullDescription() throws Exception {
        String json = """
                {
                    "individualId": "%s",
                    "assigneeId": "%s",
                    "description": null
                }
                """.formatted(individual.getId(), assignee.getId());

        mockMvc.perform(post("/api/tasks")
                        .with(user(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Null individualId rejected with 400 (NOT NULL)")
    void databaseConstraints_rejectsNullIndividualId() throws Exception {
        String json = """
                {
                    "individualId": null,
                    "assigneeId": "%s",
                    "description": "Valid description"
                }
                """.formatted(assignee.getId());

        mockMvc.perform(post("/api/tasks")
                        .with(user(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // Test 3: Pagination with >50 tasks
    // Validates: Requirement 2.6
    // =========================================================================

    @Test
    @DisplayName("Pagination returns max 50 tasks per page with correct totalCount")
    void pagination_moreThan50Tasks() throws Exception {
        for (int i = 0; i < 55; i++) {
            Task task = new Task(
                    individual.getId(), null, creator.getId(), assignee.getId(),
                    "Paginated task " + i, TaskStatus.TODO, null
            );
            taskRepository.save(task);
        }

        // Page 0 → 50 tasks
        mockMvc.perform(get("/api/tasks")
                        .with(user(creator))
                        .param("assigneeId", assignee.getId().toString())
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks", hasSize(50)))
                .andExpect(jsonPath("$.totalCount").value(55))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(50));

        // Page 1 → 5 tasks
        mockMvc.perform(get("/api/tasks")
                        .with(user(creator))
                        .param("assigneeId", assignee.getId().toString())
                        .param("page", "1")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks", hasSize(5)))
                .andExpect(jsonPath("$.totalCount").value(55));
    }

    // =========================================================================
    // Test 4: Sort with null dueDate positioning
    // Validates: Requirement 7.9
    // =========================================================================

    @Test
    @DisplayName("Sort by dueDate places null due dates last regardless of direction")
    void sortByDueDate_nullsLast() throws Exception {
        taskRepository.save(new Task(
                individual.getId(), null, creator.getId(), assignee.getId(),
                "Has due date", TaskStatus.TODO, LocalDate.now().plusDays(5)
        ));
        taskRepository.save(new Task(
                individual.getId(), null, creator.getId(), assignee.getId(),
                "No due date", TaskStatus.TODO, null
        ));

        // Ascending: non-null first, null last
        mockMvc.perform(get("/api/tasks")
                        .with(user(creator))
                        .param("assigneeId", assignee.getId().toString())
                        .param("sortBy", "dueDate")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks", hasSize(2)))
                .andExpect(jsonPath("$.tasks[0].description").value("Has due date"))
                .andExpect(jsonPath("$.tasks[1].description").value("No due date"));

        // Descending: non-null first, null last
        mockMvc.perform(get("/api/tasks")
                        .with(user(creator))
                        .param("assigneeId", assignee.getId().toString())
                        .param("sortBy", "dueDate")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks", hasSize(2)))
                .andExpect(jsonPath("$.tasks[0].description").value("Has due date"))
                .andExpect(jsonPath("$.tasks[1].description").value("No due date"));
    }

    // =========================================================================
    // Test 5: GET /api/tasks/interactions returns 404 for non-existent individual
    // Validates: Requirement 10.3
    // =========================================================================

    @Test
    @DisplayName("GET /api/tasks/interactions returns 404 for non-existent individual")
    void interactionsEndpoint_nonExistentIndividual_returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/tasks/interactions")
                        .with(user(creator))
                        .param("individualId", nonExistentId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/tasks/interactions returns interactions for existing individual")
    void interactionsEndpoint_existingIndividual_returnsInteractions() throws Exception {
        Interaction interaction = new Interaction();
        interaction.setEmployeeId(individual.getId());
        interaction.setStaffId(creator.getId());
        interaction.setType("CHECK_IN");
        interaction.setNotes("Integration test interaction");
        interaction.setOccurredAt(LocalDateTime.now().minusDays(1));
        interactionRepository.save(interaction);

        mockMvc.perform(get("/api/tasks/interactions")
                        .with(user(creator))
                        .param("individualId", individual.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].employeeId").value(individual.getId().toString()))
                .andExpect(jsonPath("$[0].type").value("CHECK_IN"));

        interactionRepository.deleteAll();
    }

    // =========================================================================
    // Test 6: Delete — task persists unchanged after 403
    // Validates: Requirement 9.3
    // =========================================================================

    @Test
    @DisplayName("Delete by non-creator returns 403 and task persists unchanged")
    void delete_nonCreator_returns403AndTaskUnchanged() throws Exception {
        var createRequest = new CreateTaskRequest(
                individual.getId(), null, assignee.getId(),
                "Task that should persist after 403", LocalDate.now().plusDays(5)
        );

        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .with(user(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        // Non-creator attempts delete → 403
        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .with(user(assignee)))
                .andExpect(status().isForbidden());

        // Verify task still exists and is unchanged
        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.description").value("Task that should persist after 403"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.assigneeId").value(assignee.getId().toString()));
    }

    // =========================================================================
    // Additional: Filter by assignee returns correct subset
    // Validates: Requirement 2.1
    // =========================================================================

    @Test
    @DisplayName("Filter by assignee returns only tasks assigned to that staff member")
    void filterByAssignee_returnsOnlyMatchingTasks() throws Exception {
        taskRepository.save(new Task(
                individual.getId(), null, creator.getId(), assignee.getId(),
                "For assignee", TaskStatus.TODO, null
        ));
        taskRepository.save(new Task(
                individual.getId(), null, creator.getId(), creator.getId(),
                "Self-assigned", TaskStatus.TODO, null
        ));

        mockMvc.perform(get("/api/tasks")
                        .with(user(creator))
                        .param("assigneeId", assignee.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks", hasSize(1)))
                .andExpect(jsonPath("$.tasks[0].description").value("For assignee"));
    }

    // =========================================================================
    // Additional: Status update by non-assignee returns 403
    // Validates: Requirement 6.1
    // =========================================================================

    @Test
    @DisplayName("Status update by non-assignee returns 403")
    void statusUpdate_nonAssignee_returns403() throws Exception {
        Task task = new Task(
                individual.getId(), null, creator.getId(), assignee.getId(),
                "Status test task", TaskStatus.TODO, null
        );
        task = taskRepository.save(task);

        mockMvc.perform(patch("/api/tasks/{id}/status", task.getId())
                        .with(user(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest("DONE"))))
                .andExpect(status().isForbidden());
    }
}
