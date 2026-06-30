package com.staffengagement.task.service;

import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.interaction.service.InteractionService;
import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.shared.exception.InactiveStaffException;
import com.staffengagement.shared.exception.TaskAssignmentForbiddenException;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.repository.TaskRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for task creation (Properties 1–5).
 *
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.8, 1.9, 4.1, 4.3, 4.4, 4.5, 5.1, 5.2, 5.5
 */
class TaskCreationPropertyTest {

    private TaskRepository repository;
    private StaffService staffService;
    private EmployeeService employeeService;
    private InteractionService interactionService;
    private TaskService taskService;
    private Validator validator;

    @BeforeProperty
    void setUp() {
        repository = mock(TaskRepository.class);
        staffService = mock(StaffService.class);
        employeeService = mock(EmployeeService.class);
        interactionService = mock(InteractionService.class);

        // TaskServiceImpl is package-private — instantiate directly since test is in same package
        taskService = new TaskServiceImpl(repository, staffService, employeeService, interactionService);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ========================================================================
    // Property 1: Task creation round-trip preserves all input data
    // ========================================================================

    /**
     * Property 1: Task creation round-trip preserves all input data.
     *
     * For any valid CreateTaskRequest with active creator/assignee, existing individual,
     * and valid optional dueDate (today or future or null), the TaskResponse SHALL contain
     * the same individualId, interactionId, assigneeId, description, matching dueDate,
     * creatorId matching the authenticated user, status=TODO, non-null id and createdAt.
     *
     * **Validates: Requirements 1.1, 1.2, 1.9, 5.1, 5.2, 5.5**
     */
    @Property(tries = 100)
    void taskCreationRoundTripPreservesAllInputData(
            @ForAll("validCreateTaskRequestsNoInteraction") CreateTaskRequest request,
            @ForAll("randomUUIDs") UUID creatorId
    ) {
        // Arrange: active creator and assignee, existing individual
        StaffResponse activeCreator = new StaffResponse(
                creatorId, "creator@test.com", StaffRole.STAFF, true, LocalDateTime.now());
        StaffResponse activeAssignee = new StaffResponse(
                request.assigneeId(), "assignee@test.com", StaffRole.STAFF, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(request.assigneeId())).thenReturn(activeAssignee);
        when(employeeService.existsById(request.individualId())).thenReturn(true);

        // Mock repository.save() to return entity with generated id
        when(repository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, UUID.randomUUID());
            return task;
        });

        // Act
        TaskResponse response = taskService.create(request, creatorId);

        // Assert round-trip preservation
        assertThat(response.id()).isNotNull();
        assertThat(response.individualId()).isEqualTo(request.individualId());
        assertThat(response.interactionId()).isEqualTo(request.interactionId());
        assertThat(response.assigneeId()).isEqualTo(request.assigneeId());
        assertThat(response.creatorId()).isEqualTo(creatorId);
        assertThat(response.description()).isEqualTo(request.description());
        assertThat(response.dueDate()).isEqualTo(request.dueDate());
        assertThat(response.status()).isEqualTo(TaskStatus.TODO.getDisplayName());
        assertThat(response.createdAt()).isNotNull();
    }

    // ========================================================================
    // Property 2: Description validation rejects blank and oversized inputs
    // ========================================================================

    /**
     * Property 2: Blank descriptions are rejected at the DTO validation level.
     *
     * For any string that is blank (null, empty, or whitespace-only),
     * Jakarta Validator SHALL produce a violation on the description field.
     *
     * **Validates: Requirements 1.3, 4.1**
     */
    @Property(tries = 50)
    void blankDescriptionsAreRejectedByValidator(@ForAll("blankDescriptions") String description) {
        var request = new CreateTaskRequest(
                UUID.randomUUID(), null, UUID.randomUUID(),
                description, LocalDate.now().plusDays(1)
        );

        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("description"))).isTrue();
    }

    /**
     * Property 2: Oversized descriptions (>2000 chars) are rejected.
     *
     * **Validates: Requirements 1.3, 4.1**
     */
    @Property(tries = 50)
    void oversizedDescriptionsAreRejectedByValidator(@ForAll("oversizedDescriptions") String description) {
        var request = new CreateTaskRequest(
                UUID.randomUUID(), null, UUID.randomUUID(),
                description, LocalDate.now().plusDays(1)
        );

        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("description"))).isTrue();
    }

    // ========================================================================
    // Property 3: Multi-field validation returns all errors simultaneously
    // ========================================================================

    /**
     * Property 3: Multiple invalid fields produce at least that many violations.
     *
     * For any request with N invalid fields (N >= 2), the validator returns >= N violations.
     *
     * **Validates: Requirements 1.8, 4.1**
     */
    @Property(tries = 50)
    void multiFieldValidationReturnsAllErrorsSimultaneously(
            @ForAll @IntRange(min = 2, max = 3) int numberOfInvalidFields
    ) {
        String description = "Valid description";
        UUID assigneeId = UUID.randomUUID();
        UUID individualId = UUID.randomUUID();

        int invalidCount = 0;

        if (invalidCount < numberOfInvalidFields) {
            description = "";  // blank → @NotBlank violation
            invalidCount++;
        }
        if (invalidCount < numberOfInvalidFields) {
            assigneeId = null;  // null → @NotNull violation
            invalidCount++;
        }
        if (invalidCount < numberOfInvalidFields) {
            individualId = null;  // null → @NotNull violation
            invalidCount++;
        }

        var request = new CreateTaskRequest(
                individualId, null, assigneeId,
                description, LocalDate.now().plusDays(1)
        );

        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertThat(violations.size()).isGreaterThanOrEqualTo(numberOfInvalidFields);
    }

    // ========================================================================
    // Property 4: Non-existent assignee rejected with 404; inactive with 400
    // ========================================================================

    /**
     * Property 4a: Non-existent assignee UUID triggers EntityNotFoundException (HTTP 404).
     *
     * **Validates: Requirements 1.4, 4.4**
     */
    @Property(tries = 100)
    void nonExistentAssigneeRejectedWith404(
            @ForAll("validCreateTaskRequests") CreateTaskRequest request,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID nonExistentAssigneeId
    ) {
        // Active creator
        StaffResponse activeCreator = new StaffResponse(
                creatorId, "creator@test.com", StaffRole.ADMIN, true, LocalDateTime.now());
        when(staffService.findById(creatorId)).thenReturn(activeCreator);

        // Assignee not found
        when(staffService.findById(nonExistentAssigneeId))
                .thenThrow(new EntityNotFoundException("Staff not found with id: " + nonExistentAssigneeId));

        CreateTaskRequest requestWithBadAssignee = new CreateTaskRequest(
                request.individualId(), request.interactionId(),
                nonExistentAssigneeId, request.description(), request.dueDate()
        );

        assertThatThrownBy(() -> taskService.create(requestWithBadAssignee, creatorId))
                .isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).save(any(Task.class));
    }

    /**
     * Property 4b: Inactive assignee triggers InactiveStaffException (HTTP 400).
     *
     * **Validates: Requirements 1.5, 4.5**
     */
    @Property(tries = 100)
    void inactiveAssigneeRejectedWith400(
            @ForAll("validCreateTaskRequests") CreateTaskRequest request,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID inactiveAssigneeId
    ) {
        // Active creator
        StaffResponse activeCreator = new StaffResponse(
                creatorId, "creator@test.com", StaffRole.ADMIN, true, LocalDateTime.now());
        when(staffService.findById(creatorId)).thenReturn(activeCreator);

        // Inactive assignee
        StaffResponse inactiveAssignee = new StaffResponse(
                inactiveAssigneeId, "inactive@test.com", StaffRole.STAFF, false, LocalDateTime.now());
        when(staffService.findById(inactiveAssigneeId)).thenReturn(inactiveAssignee);

        CreateTaskRequest requestWithInactiveAssignee = new CreateTaskRequest(
                request.individualId(), request.interactionId(),
                inactiveAssigneeId, request.description(), request.dueDate()
        );

        assertThatThrownBy(() -> taskService.create(requestWithInactiveAssignee, creatorId))
                .isInstanceOf(InactiveStaffException.class);

        verify(repository, never()).save(any(Task.class));
    }

    // ========================================================================
    // Property 5: Inactive creator rejected with 403
    // ========================================================================

    /**
     * Property 5: For any Staff_Member who is not active, attempting to create a task
     * SHALL be rejected with TaskAssignmentForbiddenException (HTTP 403).
     *
     * **Validates: Requirements 4.3**
     */
    @Property(tries = 100)
    void inactiveCreatorRejectedWith403(
            @ForAll("validCreateTaskRequests") CreateTaskRequest request,
            @ForAll("randomUUIDs") UUID creatorId
    ) {
        // Creator exists but is inactive
        StaffResponse inactiveCreator = new StaffResponse(
                creatorId, "inactive-creator@test.com", StaffRole.STAFF, false, LocalDateTime.now());
        when(staffService.findById(creatorId)).thenReturn(inactiveCreator);

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(TaskAssignmentForbiddenException.class);

        verify(repository, never()).save(any(Task.class));
    }

    // ========================================================================
    // Generators
    // ========================================================================

    @Provide
    Arbitrary<CreateTaskRequest> validCreateTaskRequests() {
        Arbitrary<UUID> uuids = Arbitraries.create(UUID::randomUUID);
        Arbitrary<String> descriptions = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '.', ',')
                .ofMinLength(1)
                .ofMaxLength(2000)
                .filter(s -> !s.isBlank());
        Arbitrary<LocalDate> dueDates = Arbitraries.integers()
                .between(0, 365)
                .map(days -> LocalDate.now().plusDays(days))
                .injectNull(0.3);

        return Combinators.combine(uuids, uuids, uuids, descriptions, dueDates)
                .as((individualId, interactionId, assigneeId, description, dueDate) ->
                        new CreateTaskRequest(individualId, interactionId, assigneeId, description, dueDate));
    }

    @Provide
    Arbitrary<CreateTaskRequest> validCreateTaskRequestsNoInteraction() {
        Arbitrary<UUID> uuids = Arbitraries.create(UUID::randomUUID);
        Arbitrary<String> descriptions = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '.', ',')
                .ofMinLength(1)
                .ofMaxLength(2000)
                .filter(s -> !s.isBlank());
        Arbitrary<LocalDate> dueDates = Arbitraries.integers()
                .between(0, 365)
                .map(days -> LocalDate.now().plusDays(days))
                .injectNull(0.3);

        return Combinators.combine(uuids, uuids, descriptions, dueDates)
                .as((individualId, assigneeId, description, dueDate) ->
                        new CreateTaskRequest(individualId, null, assigneeId, description, dueDate));
    }

    @Provide
    Arbitrary<UUID> randomUUIDs() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<String> blankDescriptions() {
        return Arbitraries.of("", "   ", "  \t  ", "\n\n", "\t", null);
    }

    @Provide
    Arbitrary<String> oversizedDescriptions() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(2001)
                .ofMaxLength(3000);
    }
}
