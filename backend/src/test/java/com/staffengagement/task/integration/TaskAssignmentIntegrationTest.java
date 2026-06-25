package com.staffengagement.task.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffengagement.employee.model.Employee;
import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.staff.model.Staff;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.repository.StaffRepository;
import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.dto.UpdateStatusRequest;
import com.staffengagement.task.repository.TaskRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Staff Task Assignment feature using Testcontainers with PostgreSQL.
 *
 * Validates: Requirements 1.1, 2.1, 2.2, 2.5, 3.1, 3.3, 5.4, 6.1, 7.2, 7.3, 7.6
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskAssignmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Employee employee;
    private Staff creator;
    private Staff assignee;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        staffRepository.deleteAll();
        employeeRepository.deleteAll();

        // Create Employee
        employee = new Employee();
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john.doe+" + UUID.randomUUID() + "@test.com");
        employee.setDepartment("Engineering");
        employee.setJobTitle("Developer");
        employee.setHireDate(LocalDate.of(2023, 1, 15));
        employee.setActive(true);
        employee = employeeRepository.save(employee);

        // Create Staff - Creator
        creator = new Staff();
        creator.setEmployeeId(employee.getId());
        creator.setEmail("creator+" + UUID.randomUUID() + "@test.com");
        creator.setPassword(passwordEncoder.encode("password123"));
        creator.setRole(StaffRole.STAFF);
        creator.setActive(true);
        creator = staffRepository.save(creator);

        // Create Staff - Assignee
        assignee = new Staff();
        assignee.setEmployeeId(employee.getId());
        assignee.setEmail("assignee+" + UUID.randomUUID() + "@test.com");
        assignee.setPassword(passwordEncoder.encode("password123"));
        assignee.setRole(StaffRole.STAFF);
        assignee.setActive(true);
        assignee = staffRepository.save(assignee);
    }

    /**
     * Test full task creation lifecycle: create → retrieve → filter by assignee → filter by creator → update status.
     * Validates: Requirements 1.1, 2.1, 3.1, 5.4, 6.1
     */
    @Test
    @Order(1)
    void fullTaskLifecycle_createRetrieveFilterUpdateStatus() throws Exception {
        // Step 1: Create task
        var createRequest = new CreateTaskRequest(
                employee.getId(),
                null,
                assignee.getId(),
                "Integration Test Task",
                "A task for integration testing",
                LocalDate.now().plusDays(7)
        );

        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .with(user(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.employeeId").value(employee.getId().toString()))
                .andExpect(jsonPath("$.creatorId").value(creator.getId().toString()))
                .andExpect(jsonPath("$.assigneeId").value(assignee.getId().toString()))
                .andExpect(jsonPath("$.title").value("Integration Test Task"))
                .andExpect(jsonPath("$.description").value("A task for integration testing"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        // Step 2: Retrieve by ID
        mockMvc.perform(get("/api/tasks/" + taskId)
                        .with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.creatorId").value(creator.getId().toString()))
                .andExpect(jsonPath("$.assigneeId").value(assignee.getId().toString()))
                .andExpect(jsonPath("$.title").value("Integration Test Task"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        // Step 3: Filter by assignee
        mockMvc.perform(get("/api/tasks")
                        .param("assigneeId", assignee.getId().toString())
                        .with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(taskId))
                .andExpect(jsonPath("$[0].assigneeId").value(assignee.getId().toString()));

        // Step 4: Filter by creator
        mockMvc.perform(get("/api/tasks")
                        .param("creatorId", creator.getId().toString())
                        .with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(taskId))
                .andExpect(jsonPath("$[0].creatorId").value(creator.getId().toString()));

        // Step 5: Update status (as assignee)
        var statusRequest = new UpdateStatusRequest("IN_PROGRESS");

        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .with(user(assignee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Verify final state
        mockMvc.perform(get("/api/tasks/" + taskId)
                        .with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    /**
     * Test backward compatibility: pre-existing tasks without creator/assignee return null fields.
     * Validates: Requirement 5.4
     */
    @Test
    @Order(2)
    void backwardCompatibility_tasksWithoutCreatorAssigneeReturnNullFields() throws Exception {
        // Insert a task directly via SQL without creator_id/assignee_id
        UUID legacyTaskId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tsk_tasks (id, employee_id, title, description, status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW())",
                legacyTaskId, employee.getId(), "Legacy Task", "Created before assignment feature", "OPEN"
        );

        // Retrieve and verify null fields
        mockMvc.perform(get("/api/tasks/" + legacyTaskId)
                        .with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(legacyTaskId.toString()))
                .andExpect(jsonPath("$.title").value("Legacy Task"))
                .andExpect(jsonPath("$.creatorId").doesNotExist())
                .andExpect(jsonPath("$.assigneeId").doesNotExist());
    }

    /**
     * Test database FK constraint enforcement on creator_id.
     * Validates: Design doc - FK constraints on creator_id
     */
    @Test
    @Order(3)
    void databaseConstraint_invalidCreatorIdViolatesForeignKey() {
        UUID fakeStaffId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        Assertions.assertThrows(Exception.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO tsk_tasks (id, employee_id, creator_id, title, status, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, NOW())",
                    taskId, employee.getId(), fakeStaffId, "Task with bad creator", "OPEN"
            );
        });
    }

    /**
     * Test database FK constraint enforcement on assignee_id.
     * Validates: Design doc - FK constraints on assignee_id
     */
    @Test
    @Order(4)
    void databaseConstraint_invalidAssigneeIdViolatesForeignKey() {
        UUID fakeStaffId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        Assertions.assertThrows(Exception.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO tsk_tasks (id, employee_id, assignee_id, title, status, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, NOW())",
                    taskId, employee.getId(), fakeStaffId, "Task with bad assignee", "OPEN"
            );
        });
    }

    /**
     * Test sort behavior with real PostgreSQL ordering: descending.
     * Validates: Requirements 7.2, 7.6
     */
    @Test
    @Order(5)
    void sortBehavior_descendingOrderReturnsNewestFirst() throws Exception {
        // Create multiple tasks with controlled ordering
        createTaskViaApi("First Task", creator, assignee);
        Thread.sleep(50); // Ensure different timestamps
        createTaskViaApi("Second Task", creator, assignee);
        Thread.sleep(50);
        createTaskViaApi("Third Task", creator, assignee);

        // Query with sortOrder=desc
        mockMvc.perform(get("/api/tasks")
                        .param("assigneeId", assignee.getId().toString())
                        .param("sortOrder", "desc")
                        .with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Third Task"))
                .andExpect(jsonPath("$[1].title").value("Second Task"))
                .andExpect(jsonPath("$[2].title").value("First Task"));
    }

    /**
     * Test sort behavior with real PostgreSQL ordering: ascending.
     * Validates: Requirements 7.3, 7.6
     */
    @Test
    @Order(6)
    void sortBehavior_ascendingOrderReturnsOldestFirst() throws Exception {
        // Create multiple tasks with controlled ordering
        createTaskViaApi("First Task", creator, assignee);
        Thread.sleep(50);
        createTaskViaApi("Second Task", creator, assignee);
        Thread.sleep(50);
        createTaskViaApi("Third Task", creator, assignee);

        // Query with sortOrder=asc
        mockMvc.perform(get("/api/tasks")
                        .param("assigneeId", assignee.getId().toString())
                        .param("sortOrder", "asc")
                        .with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("First Task"))
                .andExpect(jsonPath("$[1].title").value("Second Task"))
                .andExpect(jsonPath("$[2].title").value("Third Task"));
    }

    /**
     * Test empty result sets for non-matching assignee filter.
     * Validates: Requirements 2.2, 2.5
     */
    @Test
    @Order(7)
    void emptyResultSet_nonMatchingAssigneeReturnsEmptyList() throws Exception {
        UUID nonExistentStaffId = UUID.randomUUID();

        mockMvc.perform(get("/api/tasks")
                        .param("assigneeId", nonExistentStaffId.toString())
                        .with(user(creator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * Test empty result sets for non-matching creator filter.
     * Validates: Requirement 3.3
     */
    @Test
    @Order(8)
    void emptyResultSet_nonMatchingCreatorReturnsError() throws Exception {
        // Per requirement 3.5, a non-existent creatorId returns 400 "creator not found"
        UUID nonExistentStaffId = UUID.randomUUID();

        mockMvc.perform(get("/api/tasks")
                        .param("creatorId", nonExistentStaffId.toString())
                        .with(user(creator)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Helper: Create a task via the API.
     */
    private void createTaskViaApi(String title, Staff taskCreator, Staff taskAssignee) throws Exception {
        var request = new CreateTaskRequest(
                employee.getId(),
                null,
                taskAssignee.getId(),
                title,
                "Description for " + title,
                LocalDate.now().plusDays(7)
        );

        mockMvc.perform(post("/api/tasks")
                        .with(user(taskCreator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
