package com.staffengagement.task.property;

import com.staffengagement.shared.exception.InvalidParameterException;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.repository.TaskRepository;
import com.staffengagement.task.service.TaskService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for input validation rejection.
 * Tests Properties 2, 3, and 13 from the design document.
 *
 * Validates: Requirements 1.3, 1.6, 1.7, 4.1, 4.2
 */
class TaskValidationPropertyTest {

    private final Validator validator;

    TaskValidationPropertyTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    // ========================================================================
    // Feature: staff-task-assignment, Property 2: Description validation rejects blank and oversized inputs
    // ========================================================================

    @Property(tries = 100)
    void blankDescriptionsAreRejected(@ForAll("blankDescriptions") String description) {
        // **Validates: Requirements 1.3**
        var request = new CreateTaskRequest(
                UUID.randomUUID(), null, UUID.randomUUID(),
                description, LocalDate.now().plusDays(1)
        );

        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("description"))).isTrue();
    }

    @Property(tries = 100)
    void longDescriptionsAreRejected(@ForAll("longDescriptions") String description) {
        // **Validates: Requirements 1.3, 4.1**
        var request = new CreateTaskRequest(
                UUID.randomUUID(), null, UUID.randomUUID(),
                description, LocalDate.now().plusDays(1)
        );

        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("description"))).isTrue();
    }

    @Property(tries = 100)
    void nullAssigneeIdIsRejected(@ForAll("validDescriptions") String description) {
        // **Validates: Requirements 4.1**
        var request = new CreateTaskRequest(
                UUID.randomUUID(), null, null,
                description, LocalDate.now().plusDays(1)
        );

        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("assigneeId"))).isTrue();
    }

    @Property(tries = 100)
    void nullIndividualIdIsRejected(@ForAll("validDescriptions") String description) {
        // **Validates: Requirements 4.1**
        var request = new CreateTaskRequest(
                null, null, UUID.randomUUID(),
                description, LocalDate.now().plusDays(1)
        );

        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("individualId"))).isTrue();
    }

    // ========================================================================
    // Feature: staff-task-assignment, Property 3: Multi-field validation returns all errors simultaneously
    // ========================================================================

    @Property(tries = 100)
    void multipleInvalidFieldsReturnAllErrors(
            @ForAll @IntRange(min = 2, max = 3) int numberOfInvalidFields
    ) {
        // **Validates: Requirements 1.7**
        // Build a request with exactly N invalid fields
        String description = "Valid description";
        UUID assigneeId = UUID.randomUUID();
        UUID individualId = UUID.randomUUID();

        int invalidCount = 0;

        // Field 1: blank description
        if (invalidCount < numberOfInvalidFields) {
            description = "";
            invalidCount++;
        }

        // Field 2: null assigneeId
        if (invalidCount < numberOfInvalidFields) {
            assigneeId = null;
            invalidCount++;
        }

        // Field 3: null individualId
        if (invalidCount < numberOfInvalidFields) {
            individualId = null;
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
    // Feature: staff-task-assignment, Property 13: Past due dates are rejected
    // ========================================================================

    @Property(tries = 100)
    void pastDueDatesAreRejected(@ForAll("pastDates") LocalDate pastDate) {
        // **Validates: Requirements 4.2**
        var creatorId = UUID.randomUUID();
        var assigneeId = UUID.randomUUID();
        var individualId = UUID.randomUUID();

        var request = new CreateTaskRequest(
                individualId, null, assigneeId,
                "Valid description", pastDate
        );

        // Set up mocks for the service layer validation
        TaskRepository repository = mock(TaskRepository.class);
        StaffService staffService = mock(StaffService.class);

        var activeCreator = new StaffResponse(creatorId,
                "creator@test.com", StaffRole.ADMIN, true, LocalDateTime.now());
        var activeAssignee = new StaffResponse(assigneeId,
                "assignee@test.com", StaffRole.STAFF, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);

        // Create TaskServiceImpl via reflection since it is package-private
        TaskService taskService = createTaskServiceImpl(repository, staffService);

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("Due date must not be in the past");

        verify(repository, never()).save(any(Task.class));
    }

    // ========================================================================
    // Generators
    // ========================================================================

    @Provide
    Arbitrary<String> blankDescriptions() {
        return Arbitraries.of("", "   ", "  \t  ", "\n", "\t");
    }

    @Provide
    Arbitrary<String> longDescriptions() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(2001)
                .ofMaxLength(3000);
    }

    @Provide
    Arbitrary<String> validDescriptions() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(2000);
    }

    @Provide
    Arbitrary<LocalDate> pastDates() {
        return Arbitraries.integers()
                .between(1, 3650)
                .map(daysAgo -> LocalDate.now().minusDays(daysAgo));
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private TaskService createTaskServiceImpl(TaskRepository repository, StaffService staffService) {
        try {
            Class<?> implClass = Class.forName("com.staffengagement.task.service.TaskServiceImpl");
            Constructor<?> constructor = implClass.getDeclaredConstructor(
                    TaskRepository.class, StaffService.class,
                    com.staffengagement.employee.service.EmployeeService.class,
                    com.staffengagement.interaction.service.InteractionService.class);
            constructor.setAccessible(true);
            var employeeService = mock(com.staffengagement.employee.service.EmployeeService.class);
            when(employeeService.existsById(any(UUID.class))).thenReturn(true);
            return (TaskService) constructor.newInstance(repository, staffService,
                    employeeService,
                    mock(com.staffengagement.interaction.service.InteractionService.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create TaskServiceImpl via reflection", e);
        }
    }
}
