package com.staffengagement.task.property;

import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.shared.exception.InactiveStaffException;
import com.staffengagement.shared.exception.TaskAssignmentForbiddenException;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.repository.TaskRepository;
import com.staffengagement.task.service.TaskService;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for task assignment validation.
 * Tests Properties 4 and 5 from the design document.
 *
 * Validates: Requirements 1.4, 1.5, 4.3, 4.4, 4.5
 */
class TaskAssignmentPropertyTest {

    private TaskRepository repository;
    private StaffService staffService;
    private TaskService taskService;

    @BeforeProperty
    void setUp() throws Exception {
        repository = Mockito.mock(TaskRepository.class);
        staffService = Mockito.mock(StaffService.class);

        // TaskServiceImpl is package-private, instantiate via reflection
        Class<?> clazz = Class.forName("com.staffengagement.task.service.TaskServiceImpl");
        Constructor<?> constructor = clazz.getDeclaredConstructor(
                TaskRepository.class, StaffService.class,
                com.staffengagement.employee.service.EmployeeService.class,
                com.staffengagement.interaction.service.InteractionService.class);
        constructor.setAccessible(true);
        taskService = (TaskService) constructor.newInstance(repository, staffService,
                Mockito.mock(com.staffengagement.employee.service.EmployeeService.class),
                Mockito.mock(com.staffengagement.interaction.service.InteractionService.class));
    }

    // Feature: staff-task-assignment, Property 4: Non-existent assignee is rejected with 404

    /**
     * Property 4: Non-existent assignee is rejected with 404
     *
     * For any UUID that does not correspond to an existing Staff_Member,
     * a task creation request specifying that UUID as assigneeId SHALL be rejected
     * with EntityNotFoundException (mapped to HTTP 404).
     *
     * Validates: Requirements 1.4, 4.4
     */
    @Property(tries = 100)
    void nonExistentAssigneeIsRejectedWith404(
            @ForAll("validCreateTaskRequests") CreateTaskRequest request,
            @ForAll("randomUuids") UUID creatorId,
            @ForAll("randomUuids") UUID nonExistentAssigneeId
    ) {
        // Arrange: creator is valid and active
        StaffResponse activeCreator = new StaffResponse(
                creatorId,
                "creator@example.com",
                StaffRole.ADMIN,
                true,
                LocalDateTime.now()
        );
        when(staffService.findById(creatorId)).thenReturn(activeCreator);

        // Assignee does not exist — throws EntityNotFoundException
        when(staffService.findById(nonExistentAssigneeId))
                .thenThrow(new EntityNotFoundException("Staff not found with id: " + nonExistentAssigneeId));

        // Build request with the non-existent assignee
        CreateTaskRequest requestWithNonExistentAssignee = new CreateTaskRequest(
                request.individualId(),
                request.interactionId(),
                nonExistentAssigneeId,
                request.description(),
                request.dueDate()
        );

        // Act & Assert: should throw EntityNotFoundException (404)
        assertThatThrownBy(() -> taskService.create(requestWithNonExistentAssignee, creatorId))
                .isInstanceOf(EntityNotFoundException.class);

        // Verify no task was persisted
        verify(repository, never()).save(any(Task.class));
    }

    // Feature: staff-task-assignment, Property 5: Inactive staff assignment is rejected

    /**
     * Property 5A: Inactive creator is rejected with 403
     *
     * For any task creation request where the creator is not an active Staff_Member,
     * the system SHALL reject with TaskAssignmentForbiddenException (mapped to HTTP 403).
     *
     * Validates: Requirements 4.3
     */
    @Property(tries = 100)
    void inactiveCreatorIsRejectedWith403(
            @ForAll("validCreateTaskRequests") CreateTaskRequest request,
            @ForAll("randomUuids") UUID creatorId
    ) {
        // Arrange: creator exists but is inactive
        StaffResponse inactiveCreator = new StaffResponse(
                creatorId,
                "inactive-creator@example.com",
                StaffRole.STAFF,
                false,
                LocalDateTime.now()
        );
        when(staffService.findById(creatorId)).thenReturn(inactiveCreator);

        // Act & Assert: should throw TaskAssignmentForbiddenException (403)
        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(TaskAssignmentForbiddenException.class)
                .hasMessage("Creator is not active");

        // Verify no task was persisted
        verify(repository, never()).save(any(Task.class));
    }

    /**
     * Property 5B: Inactive assignee is rejected with 400
     *
     * For any task creation request where the assignee is an inactive Staff_Member,
     * the system SHALL reject with InactiveStaffException (mapped to HTTP 400).
     *
     * Validates: Requirements 1.5, 4.5
     */
    @Property(tries = 100)
    void inactiveAssigneeIsRejectedWith400(
            @ForAll("validCreateTaskRequests") CreateTaskRequest request,
            @ForAll("randomUuids") UUID creatorId,
            @ForAll("randomUuids") UUID inactiveAssigneeId
    ) {
        // Arrange: creator is valid and active
        StaffResponse activeCreator = new StaffResponse(
                creatorId,
                "creator@example.com",
                StaffRole.ADMIN,
                true,
                LocalDateTime.now()
        );
        when(staffService.findById(creatorId)).thenReturn(activeCreator);

        // Assignee exists but is inactive
        StaffResponse inactiveAssignee = new StaffResponse(
                inactiveAssigneeId,
                "inactive-assignee@example.com",
                StaffRole.STAFF,
                false,
                LocalDateTime.now()
        );
        when(staffService.findById(inactiveAssigneeId)).thenReturn(inactiveAssignee);

        // Build request with the inactive assignee
        CreateTaskRequest requestWithInactiveAssignee = new CreateTaskRequest(
                request.individualId(),
                request.interactionId(),
                inactiveAssigneeId,
                request.description(),
                request.dueDate()
        );

        // Act & Assert: should throw InactiveStaffException (400)
        assertThatThrownBy(() -> taskService.create(requestWithInactiveAssignee, creatorId))
                .isInstanceOf(InactiveStaffException.class);

        // Verify no task was persisted
        verify(repository, never()).save(any(Task.class));
    }

    // --- Arbitraries / Generators ---

    @Provide
    Arbitrary<UUID> randomUuids() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<CreateTaskRequest> validCreateTaskRequests() {
        Arbitrary<UUID> uuids = Arbitraries.create(UUID::randomUUID);
        Arbitrary<String> descriptions = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(2000)
                .filter(s -> !s.isBlank());
        Arbitrary<LocalDate> dueDates = Arbitraries.of(
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(365)
        ).injectNull(0.3);

        return Combinators.combine(uuids, uuids, uuids, descriptions, dueDates)
                .as((individualId, interactionId, assigneeId, description, dueDate) ->
                        new CreateTaskRequest(individualId, interactionId, assigneeId, description, dueDate));
    }
}
