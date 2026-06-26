package com.staffengagement.interaction.controller;

import com.staffengagement.BaseIntegrationTest;
import com.staffengagement.config.TestSecurityConfig;
import com.staffengagement.employee.model.Employee;
import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.interaction.dto.CreateFollowUpTaskRequest;
import com.staffengagement.interaction.dto.CreateInteractionRequest;
import com.staffengagement.interaction.dto.InteractionResponse;
import com.staffengagement.interaction.dto.UpdateInteractionRequest;
import com.staffengagement.interaction.model.InteractionType;
import com.staffengagement.interaction.repository.InteractionRepository;
import com.staffengagement.staff.model.Staff;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.repository.StaffRepository;
import com.staffengagement.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for InteractionController.
 * Tests full HTTP request lifecycle with real database (Testcontainers PostgreSQL).
 *
 * Validates: Requirements 1.10, 2.5, 2.6, 3.7, 4.3, 5.8, 10.1, 10.2, 10.7
 */
@Import(TestSecurityConfig.class)
class InteractionControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID employeeId;
    private UUID staffId;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        interactionRepository.deleteAll();
        staffRepository.deleteAll();
        employeeRepository.deleteAll();

        Employee employee = new Employee();
        employee.setFirstName("Jane");
        employee.setLastName("Smith");
        employee.setEmail("jane.smith+" + UUID.randomUUID() + "@example.com");
        employee.setDepartment("Engineering");
        employee.setJobTitle("Senior Developer");
        employee.setActive(true);
        employee = employeeRepository.save(employee);
        employeeId = employee.getId();

        Staff staff = new Staff();
        staff.setEmail("staff+" + UUID.randomUUID() + "@example.com");
        staff.setPassword(passwordEncoder.encode("password123"));
        staff.setRole(StaffRole.STAFF);
        staff.setActive(true);
        staff = staffRepository.save(staff);
        staffId = staff.getId();
    }

    // --- CREATE ---

    @Test
    void createInteraction_withValidData_returns201() {
        var request = new CreateInteractionRequest(
                employeeId, staffId, InteractionType.CHECK_IN,
                "Regular check-in notes", LocalDateTime.now().minusHours(1)
        );

        ResponseEntity<InteractionResponse> response = restTemplate.postForEntity(
                "/api/interactions", request, InteractionResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        InteractionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.employeeId()).isEqualTo(employeeId);
        assertThat(body.staffId()).isEqualTo(staffId);
        assertThat(body.type()).isEqualTo(InteractionType.CHECK_IN);
        assertThat(body.notes()).isEqualTo("Regular check-in notes");
        assertThat(body.occurredAt()).isNotNull();
        assertThat(body.createdAt()).isNotNull();
        assertThat(body.updatedAt()).isNotNull();
    }

    @Test
    void createInteraction_withMissingFields_returns400() {
        // Missing employeeId, staffId, type, occurredAt (all required)
        var request = Map.of("notes", "Just notes, nothing else");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/interactions", request, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createInteraction_withFutureDate_returns400() {
        var request = new CreateInteractionRequest(
                employeeId, staffId, InteractionType.MENTORING,
                "Future session", LocalDateTime.now().plusDays(5)
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/interactions", request, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- FIND BY ID ---

    @Test
    void findById_withExistingId_returns200() {
        InteractionResponse created = createTestInteraction();

        ResponseEntity<InteractionResponse> response = restTemplate.getForEntity(
                "/api/interactions/" + created.id(), InteractionResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        InteractionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(created.id());
        assertThat(body.employeeId()).isEqualTo(employeeId);
        assertThat(body.type()).isEqualTo(InteractionType.CHECK_IN);
    }

    @Test
    void findById_withNonExistentId_returns404() {
        UUID nonExistent = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/interactions/" + nonExistent, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Interaction not found");
    }

    @Test
    void findById_withInvalidUuid_returns400() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/interactions/not-a-valid-uuid", String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- FIND ALL WITH PAGINATION ---

    @Test
    void findAll_withPagination_returns200WithPageMetadata() {
        // Create 7 interactions
        for (int i = 0; i < 7; i++) {
            createTestInteraction();
        }

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/interactions?page=0&size=5", String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        // Spring Page JSON contains these fields
        assertThat(body).contains("\"totalElements\":7");
        assertThat(body).contains("\"totalPages\":2");
        assertThat(body).contains("\"number\":0");
        assertThat(body).contains("\"size\":5");
    }

    @Test
    void findAll_withEmployeeFilter_returnsFilteredResults() {
        // Create interaction for our employee
        createTestInteraction();

        // Create a second employee and interaction
        Employee other = new Employee();
        other.setFirstName("Other");
        other.setLastName("Person");
        other.setEmail("other+" + UUID.randomUUID() + "@example.com");
        other.setDepartment("HR");
        other.setJobTitle("Recruiter");
        other.setActive(true);
        other = employeeRepository.save(other);

        var otherRequest = new CreateInteractionRequest(
                other.getId(), staffId, InteractionType.INFORMAL,
                "Other interaction", LocalDateTime.now().minusHours(2)
        );
        restTemplate.postForEntity("/api/interactions", otherRequest, InteractionResponse.class);

        // Filter by our employee only
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/interactions?employeeId=" + employeeId, String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(employeeId.toString());
        assertThat(response.getBody()).doesNotContain(other.getId().toString());
    }

    @Test
    void findAll_withTypeFilter_returnsFilteredResults() {
        // Create different types
        createTestInteractionWithType(InteractionType.CHECK_IN);
        createTestInteractionWithType(InteractionType.MENTORING);
        createTestInteractionWithType(InteractionType.MENTORING);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/interactions?type=MENTORING", String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).contains("\"totalElements\":2");
    }

    // --- UPDATE ---

    @Test
    void update_withValidData_returns200() {
        InteractionResponse created = createTestInteraction();

        var updateRequest = new UpdateInteractionRequest(
                InteractionType.PERFORMANCE_REVIEW,
                "Updated notes for performance review",
                LocalDateTime.now().minusDays(1)
        );

        ResponseEntity<InteractionResponse> response = restTemplate.exchange(
                "/api/interactions/" + created.id(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                InteractionResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        InteractionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(created.id());
        assertThat(body.type()).isEqualTo(InteractionType.PERFORMANCE_REVIEW);
        assertThat(body.notes()).isEqualTo("Updated notes for performance review");
        // employeeId and staffId should remain unchanged
        assertThat(body.employeeId()).isEqualTo(employeeId);
        assertThat(body.staffId()).isEqualTo(staffId);
    }

    @Test
    void update_withNonExistentId_returns404() {
        UUID nonExistent = UUID.randomUUID();
        var updateRequest = new UpdateInteractionRequest(
                InteractionType.CATCH_UP, "notes", LocalDateTime.now().minusHours(1)
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/interactions/" + nonExistent,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void update_withInvalidData_returns400() {
        InteractionResponse created = createTestInteraction();

        // Future occurredAt should be rejected
        var updateRequest = new UpdateInteractionRequest(
                InteractionType.CHECK_IN, "notes", LocalDateTime.now().plusDays(10)
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/interactions/" + created.id(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- DELETE ---

    @Test
    void delete_withExistingId_returns204() {
        InteractionResponse created = createTestInteraction();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/interactions/" + created.id(),
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify it's gone
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
                "/api/interactions/" + created.id(), String.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_withNonExistentId_returns404() {
        UUID nonExistent = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/interactions/" + nonExistent,
                HttpMethod.DELETE,
                null,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- FOLLOW-UP TASK ---

    @Test
    void createFollowUpTask_withValidData_returns201() {
        InteractionResponse created = createTestInteraction();

        var taskRequest = new CreateFollowUpTaskRequest(
                "Follow up with employee",
                "Discuss progress on project goals",
                LocalDate.now().plusDays(7)
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/interactions/" + created.id() + "/tasks",
                taskRequest,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).contains("\"title\":\"Follow up with employee\"");
        assertThat(body).contains("\"employeeId\":\"" + employeeId + "\"");
    }

    @Test
    void createFollowUpTask_withNonExistentInteraction_returns404() {
        UUID nonExistent = UUID.randomUUID();

        var taskRequest = new CreateFollowUpTaskRequest(
                "Task title", "Task description", LocalDate.now().plusDays(3)
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/interactions/" + nonExistent + "/tasks",
                taskRequest,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- FK RESTRICT CONSTRAINT ---

    @Test
    void fkRestrict_preventsDeletionOfEmployeeWithInteractions() {
        createTestInteraction();

        // Attempt to delete the employee — should fail due to FK RESTRICT
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/employees/" + employeeId,
                HttpMethod.DELETE,
                null,
                String.class
        );

        // The delete should be rejected (either 409 Conflict or 500 due to DB constraint)
        assertThat(response.getStatusCode().value()).isGreaterThanOrEqualTo(400);

        // Verify employee still exists
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
                "/api/employees/" + employeeId, String.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- INVALID DATE RANGE ---

    @Test
    void findAll_withInvalidDateRange_returns400() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/interactions?fromDate=2024-06-01T00:00:00&toDate=2024-01-01T00:00:00",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("fromDate must be on or before toDate");
    }

    // --- HELPERS ---

    private InteractionResponse createTestInteraction() {
        return createTestInteractionWithType(InteractionType.CHECK_IN);
    }

    private InteractionResponse createTestInteractionWithType(InteractionType type) {
        var request = new CreateInteractionRequest(
                employeeId, staffId, type,
                "Test interaction notes", LocalDateTime.now().minusHours(1)
        );
        ResponseEntity<InteractionResponse> response = restTemplate.postForEntity(
                "/api/interactions", request, InteractionResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }
}
