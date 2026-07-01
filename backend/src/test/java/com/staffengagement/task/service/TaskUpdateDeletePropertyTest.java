package com.staffengagement.task.service;

import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.interaction.service.InteractionService;
import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.shared.exception.InvalidParameterException;
import com.staffengagement.shared.exception.TaskAssignmentForbiddenException;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.dto.UpdateStatusRequest;
import com.staffengagement.task.dto.UpdateTaskRequest;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.repository.TaskRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for status update, edit, and delete (Properties 13–18).
 *
 * Validates: Requirements 4.2, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 8.1, 8.2, 8.3, 8.5, 9.1, 9.2, 9.3
 */
class TaskUpdateDeletePropertyTest {

    private TaskRepository repository;
    private StaffService staffService;
    private EmployeeService employeeService;
    private InteractionService interactionService;
    private TaskService taskService;

    @BeforeProperty
    void setUp() {
        repository = mock(TaskRepository.class);
        staffService = mock(StaffService.class);
        employeeService = mock(EmployeeService.class);
        interactionService = mock(InteractionService.class);

        taskService = new TaskServiceImpl(repository, staffService, employeeService, interactionService);
    }

    // ========================================================================
    // Property 13: Past due dates are rejected
    // ========================================================================

    /**
     * Property 13: For any date strictly before today's server-local date, a task creation
     * request specifying that date as dueDate SHALL be rejected with InvalidParameterException (HTTP 400).
     *
     * **Validates: Requirements 4.2**
     */
    @Property(tries = 100)
    void pastDueDatesAreRejectedOnCreate(
            @ForAll("pastDates") LocalDate pastDate,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID assigneeId,
            @ForAll("randomUUIDs") UUID individualId
    ) {
        // Arrange: active creator and assignee, existing individual
        StaffResponse activeCreator = new StaffResponse(
                creatorId, "creator@test.com", StaffRole.STAFF, true, LocalDateTime.now());
        StaffResponse activeAssignee = new StaffResponse(
                assigneeId, "assignee@test.com", StaffRole.STAFF, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);
        when(employeeService.existsById(individualId)).thenReturn(true);

        CreateTaskRequest request = new CreateTaskRequest(
                individualId, null, assigneeId, "Valid description", pastDate);

        // Act & Assert
        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("past");

        verify(repository, never()).save(any(Task.class));
    }

    /**
     * Property 13: For any date strictly before today, an edit request specifying that date
     * as dueDate SHALL be rejected with InvalidParameterException (HTTP 400).
     *
     * **Validates: Requirements 4.2**
     */
    @Property(tries = 100)
    void pastDueDatesAreRejectedOnEdit(
            @ForAll("pastDates") LocalDate pastDate,
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID assigneeId,
            @ForAll("randomUUIDs") UUID individualId
    ) {
        // Arrange: existing task owned by requester
        Task existingTask = createMockTask(taskId, creatorId, assigneeId, individualId);
        when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));

        // Requester (creator) is active
        StaffResponse activeRequester = new StaffResponse(
                creatorId, "creator@test.com", StaffRole.STAFF, true, LocalDateTime.now());
        when(staffService.findById(creatorId)).thenReturn(activeRequester);

        // Active assignee for edit validation
        StaffResponse activeAssignee = new StaffResponse(
                assigneeId, "assignee@test.com", StaffRole.STAFF, true, LocalDateTime.now());
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);
        when(employeeService.existsById(individualId)).thenReturn(true);

        UpdateTaskRequest request = new UpdateTaskRequest(
                individualId, null, assigneeId, "Updated description", pastDate);

        // Act & Assert
        assertThatThrownBy(() -> taskService.update(taskId, request, creatorId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("past");

        verify(repository, never()).save(any(Task.class));
    }

    // ========================================================================
    // Property 14: Status update validation order and authorization
    // ========================================================================

    /**
     * Property 14a: Status update on non-existent task returns 404 first.
     *
     * For any non-existent taskId, status update SHALL reject with EntityNotFoundException
     * regardless of requester identity or status value.
     *
     * **Validates: Requirements 6.3, 6.5**
     */
    @Property(tries = 100)
    void statusUpdateNonExistentTaskReturns404(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID requesterId,
            @ForAll("validStatusStrings") String status
    ) {
        when(repository.findById(taskId)).thenReturn(Optional.empty());

        UpdateStatusRequest request = new UpdateStatusRequest(status);

        assertThatThrownBy(() -> taskService.updateStatus(taskId, request, requesterId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    /**
     * Property 14b: Non-assignee requester is rejected with 403 (after existence check).
     *
     * For any task and any Staff_Member who is NOT the task's assignee,
     * a status update SHALL be rejected with TaskAssignmentForbiddenException (HTTP 403).
     *
     * **Validates: Requirements 6.4, 6.5**
     */
    @Property(tries = 100)
    void statusUpdateNonAssigneeRejectedWith403(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID assigneeId,
            @ForAll("randomUUIDs") UUID nonAssigneeRequesterId,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID individualId,
            @ForAll("validStatusStrings") String status
    ) {
        // Ensure requester is NOT the assignee
        Assume.that(!nonAssigneeRequesterId.equals(assigneeId));

        Task existingTask = createMockTask(taskId, creatorId, assigneeId, individualId);
        when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));

        UpdateStatusRequest request = new UpdateStatusRequest(status);

        assertThatThrownBy(() -> taskService.updateStatus(taskId, request, nonAssigneeRequesterId))
                .isInstanceOf(TaskAssignmentForbiddenException.class);

        verify(repository, never()).save(any(Task.class));
    }

    /**
     * Property 14c: Invalid status value is rejected with 400 (after existence + authorization).
     *
     * For any task where the requester IS the assignee, an invalid status value
     * SHALL be rejected with InvalidParameterException (HTTP 400).
     *
     * **Validates: Requirements 6.2, 6.5**
     */
    @Property(tries = 100)
    void statusUpdateInvalidStatusRejectedWith400(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID assigneeId,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID individualId,
            @ForAll("invalidStatusStrings") String invalidStatus
    ) {
        Task existingTask = createMockTask(taskId, creatorId, assigneeId, individualId);
        when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));

        UpdateStatusRequest request = new UpdateStatusRequest(invalidStatus);

        assertThatThrownBy(() -> taskService.updateStatus(taskId, request, assigneeId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid status");

        verify(repository, never()).save(any(Task.class));
    }

    // ========================================================================
    // Property 15: Any valid status can transition to any other valid status
    // ========================================================================

    /**
     * Property 15: For all (from, to) pairs of valid TaskStatus (including same-to-same),
     * a status update by the assignee SHALL succeed.
     *
     * **Validates: Requirements 6.1, 6.6**
     */
    @Property(tries = 100)
    void anyValidStatusCanTransitionToAnyOtherValidStatus(
            @ForAll("allTaskStatuses") TaskStatus fromStatus,
            @ForAll("allTaskStatuses") TaskStatus toStatus,
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID assigneeId,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID individualId
    ) {
        // Reset mocks to avoid accumulated invocations across property tries
        reset(repository);

        Task existingTask = createMockTask(taskId, creatorId, assigneeId, individualId);
        existingTask.setStatus(fromStatus);
        when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateStatusRequest request = new UpdateStatusRequest(toStatus.name());

        // Act
        TaskResponse response = taskService.updateStatus(taskId, request, assigneeId);

        // Assert: transition succeeded
        assertThat(response.status()).isEqualTo(toStatus.getDisplayName());
        verify(repository).save(any(Task.class));
    }

    // ========================================================================
    // Property 16: Edit authorization and round-trip
    // ========================================================================

    /**
     * Property 16a: Any active staff member can edit a task (not restricted to creator).
     *
     * For any task and a requester who is NOT the creator but IS an active staff member,
     * the edit SHALL succeed.
     *
     * **Validates: Requirements 8.3**
     */
    @Property(tries = 100)
    void editByNonCreatorSucceedsWhenRequesterIsActive(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID nonCreatorRequesterId,
            @ForAll("randomUUIDs") UUID assigneeId,
            @ForAll("randomUUIDs") UUID individualId
    ) {
        Assume.that(!nonCreatorRequesterId.equals(creatorId));

        Task existingTask = createMockTask(taskId, creatorId, assigneeId, individualId);
        when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));

        // Non-creator requester is active
        StaffResponse activeRequester = new StaffResponse(
                nonCreatorRequesterId, "requester@test.com", StaffRole.STAFF, true, LocalDateTime.now());
        when(staffService.findById(nonCreatorRequesterId)).thenReturn(activeRequester);

        // Assignee is active
        StaffResponse activeAssignee = new StaffResponse(
                assigneeId, "assignee@test.com", StaffRole.STAFF, true, LocalDateTime.now());
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);

        when(employeeService.existsById(individualId)).thenReturn(true);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest request = new UpdateTaskRequest(
                individualId, null, assigneeId, "Updated desc", LocalDate.now().plusDays(1));

        TaskResponse result = taskService.update(taskId, request, nonCreatorRequesterId);

        assertThat(result).isNotNull();
        assertThat(result.description()).isEqualTo("Updated desc");
        verify(repository).save(any(Task.class));
    }

    /**
     * Property 16b: Non-existent task edit returns 404.
     *
     * For any non-existent task ID, edit SHALL be rejected with EntityNotFoundException (HTTP 404).
     *
     * **Validates: Requirements 8.2**
     */
    @Property(tries = 100)
    void editNonExistentTaskReturns404(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID requesterId,
            @ForAll("randomUUIDs") UUID assigneeId,
            @ForAll("randomUUIDs") UUID individualId
    ) {
        when(repository.findById(taskId)).thenReturn(Optional.empty());

        UpdateTaskRequest request = new UpdateTaskRequest(
                individualId, null, assigneeId, "Some desc", LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> taskService.update(taskId, request, requesterId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    /**
     * Property 16c: Edit round-trip — any active staff member can update all fields correctly.
     *
     * For any valid edit request by an active staff member, the returned TaskResponse SHALL reflect
     * all updated field values.
     *
     * **Validates: Requirements 8.1, 8.2, 8.3**
     */
    @Property(tries = 100)
    void editRoundTripPreservesAllUpdatedFields(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID newAssigneeId,
            @ForAll("randomUUIDs") UUID newIndividualId,
            @ForAll("validDescriptions") String newDescription,
            @ForAll("futureDates") LocalDate newDueDate
    ) {
        // Existing task with creator as owner
        Task existingTask = createMockTask(taskId, creatorId, UUID.randomUUID(), UUID.randomUUID());
        when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));

        // Requester (creator) is active
        StaffResponse activeCreator = new StaffResponse(
                creatorId, "creator@test.com", StaffRole.STAFF, true, LocalDateTime.now());
        when(staffService.findById(creatorId)).thenReturn(activeCreator);

        // New assignee is active
        StaffResponse activeAssignee = new StaffResponse(
                newAssigneeId, "new-assignee@test.com", StaffRole.STAFF, true, LocalDateTime.now());
        when(staffService.findById(newAssigneeId)).thenReturn(activeAssignee);
        when(employeeService.existsById(newIndividualId)).thenReturn(true);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest request = new UpdateTaskRequest(
                newIndividualId, null, newAssigneeId, newDescription, newDueDate);

        // Act
        TaskResponse response = taskService.update(taskId, request, creatorId);

        // Assert round-trip
        assertThat(response.description()).isEqualTo(newDescription);
        assertThat(response.assigneeId()).isEqualTo(newAssigneeId);
        assertThat(response.individualId()).isEqualTo(newIndividualId);
        assertThat(response.dueDate()).isEqualTo(newDueDate);
        assertThat(response.creatorId()).isEqualTo(creatorId);
    }

    // ========================================================================
    // Property 17: Interaction validation conditional on field presence
    // ========================================================================

    /**
     * Property 17a: When interactionId is null in edit request, interactionService is never called.
     *
     * For any edit request that omits interactionId, the system SHALL skip interaction
     * validation entirely.
     *
     * **Validates: Requirements 8.5**
     */
    @Property(tries = 100)
    void editWithNullInteractionIdSkipsValidation(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID assigneeId,
            @ForAll("randomUUIDs") UUID individualId,
            @ForAll("validDescriptions") String description
    ) {
        Task existingTask = createMockTask(taskId, creatorId, assigneeId, individualId);
        when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));

        StaffResponse activeRequester = new StaffResponse(
                creatorId, "creator@test.com", StaffRole.STAFF, true, LocalDateTime.now());
        when(staffService.findById(creatorId)).thenReturn(activeRequester);

        StaffResponse activeAssignee = new StaffResponse(
                assigneeId, "assignee@test.com", StaffRole.STAFF, true, LocalDateTime.now());
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);
        when(employeeService.existsById(individualId)).thenReturn(true);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // interactionId is null — should skip validation
        UpdateTaskRequest request = new UpdateTaskRequest(
                individualId, null, assigneeId, description, LocalDate.now().plusDays(1));

        // Act
        taskService.update(taskId, request, creatorId);

        // Assert: interactionService.findById() was NEVER called
        verify(interactionService, never()).findById(any(UUID.class));
    }

    /**
     * Property 17b: When interactionId is provided, it IS validated.
     *
     * For any edit request that includes an interactionId, the system SHALL validate
     * that the interaction exists and belongs to the specified individual.
     *
     * **Validates: Requirements 8.5**
     */
    @Property(tries = 100)
    void editWithProvidedInteractionIdIsValidated(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID assigneeId,
            @ForAll("randomUUIDs") UUID individualId,
            @ForAll("randomUUIDs") UUID interactionId,
            @ForAll("validDescriptions") String description
    ) {
        Task existingTask = createMockTask(taskId, creatorId, assigneeId, individualId);
        when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));

        StaffResponse activeRequester = new StaffResponse(
                creatorId, "creator@test.com", StaffRole.STAFF, true, LocalDateTime.now());
        when(staffService.findById(creatorId)).thenReturn(activeRequester);

        StaffResponse activeAssignee = new StaffResponse(
                assigneeId, "assignee@test.com", StaffRole.STAFF, true, LocalDateTime.now());
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);
        when(employeeService.existsById(individualId)).thenReturn(true);

        // Interaction exists and belongs to the individual
        var interactionResponse = new com.staffengagement.interaction.dto.InteractionResponse(
                interactionId, individualId, UUID.randomUUID(),
                com.staffengagement.interaction.model.InteractionType.CHECK_IN, "notes",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(interactionService.findById(interactionId)).thenReturn(interactionResponse);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest request = new UpdateTaskRequest(
                individualId, interactionId, assigneeId, description, LocalDate.now().plusDays(1));

        // Act
        taskService.update(taskId, request, creatorId);

        // Assert: interactionService.findById() WAS called with the provided interactionId
        verify(interactionService).findById(interactionId);
    }

    // ========================================================================
    // Property 18: Delete authorization is strict — no data modification on unauthorized attempts
    // ========================================================================

    /**
     * Property 18a: Non-creator delete is rejected with 403 and repository.delete() is never called.
     *
     * For any task and a non-creator requester, deletion SHALL be rejected with HTTP 403
     * AND the repository.delete() method SHALL never be invoked (no data modification).
     *
     * **Validates: Requirements 9.2, 9.3**
     */
    @Property(tries = 100)
    void deleteByNonCreatorRejectedWith403AndNoDataModification(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID nonCreatorRequesterId,
            @ForAll("randomUUIDs") UUID assigneeId,
            @ForAll("randomUUIDs") UUID individualId
    ) {
        Assume.that(!nonCreatorRequesterId.equals(creatorId));

        Task existingTask = createMockTask(taskId, creatorId, assigneeId, individualId);
        when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));

        // Act & Assert
        assertThatThrownBy(() -> taskService.delete(taskId, nonCreatorRequesterId))
                .isInstanceOf(TaskAssignmentForbiddenException.class);

        // Verify NO data modification occurred
        verify(repository, never()).delete(any(Task.class));
        verify(repository, never()).deleteById(any(UUID.class));
        verify(repository, never()).flush();
    }

    /**
     * Property 18b: Non-existent task delete returns 404 regardless of requester.
     *
     * For any non-existent task ID, deletion SHALL return HTTP 404 regardless of requester identity.
     *
     * **Validates: Requirements 9.2**
     */
    @Property(tries = 100)
    void deleteNonExistentTaskReturns404(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID requesterId
    ) {
        when(repository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(taskId, requesterId))
                .isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).delete(any(Task.class));
    }

    /**
     * Property 18c: Creator delete succeeds — task is removed.
     *
     * For any task and its creator, deletion SHALL succeed (no exception thrown)
     * and repository.delete() SHALL be called.
     *
     * **Validates: Requirements 9.1**
     */
    @Property(tries = 100)
    void deleteByCreatorSucceeds(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID creatorId,
            @ForAll("randomUUIDs") UUID assigneeId,
            @ForAll("randomUUIDs") UUID individualId
    ) {
        Task existingTask = createMockTask(taskId, creatorId, assigneeId, individualId);
        when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));
        doNothing().when(repository).delete(existingTask);
        doNothing().when(repository).flush();

        // Act — should not throw
        taskService.delete(taskId, creatorId);

        // Assert: deletion was performed
        verify(repository).delete(existingTask);
        verify(repository).flush();
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private Task createMockTask(UUID taskId, UUID creatorId, UUID assigneeId, UUID individualId) {
        Task task = new Task(individualId, null, creatorId, assigneeId, "Existing task", TaskStatus.TODO, null);
        // Set the ID via reflection since it's auto-generated
        try {
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, taskId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set task ID via reflection", e);
        }
        return task;
    }

    // ========================================================================
    // Generators
    // ========================================================================

    @Provide
    Arbitrary<UUID> randomUUIDs() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<LocalDate> pastDates() {
        return Arbitraries.integers()
                .between(1, 365)
                .map(days -> LocalDate.now().minusDays(days));
    }

    @Provide
    Arbitrary<LocalDate> futureDates() {
        return Arbitraries.integers()
                .between(0, 365)
                .map(days -> LocalDate.now().plusDays(days));
    }

    @Provide
    Arbitrary<String> validStatusStrings() {
        return Arbitraries.of("TODO", "IN_PROGRESS", "DONE");
    }

    @Provide
    Arbitrary<String> invalidStatusStrings() {
        // Note: "todo", "in_progress", "done" are excluded because TaskServiceImpl
        // calls toUpperCase() before valueOf(), making them valid.
        return Arbitraries.of(
                "INVALID", "PENDING", "CANCELLED", "Open", "CLOSED",
                "ACTIVE", "NOT_A_STATUS", "STARTED", "BLOCKED", "ARCHIVED"
        );
    }

    @Provide
    Arbitrary<TaskStatus> allTaskStatuses() {
        return Arbitraries.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE);
    }

    @Provide
    Arbitrary<String> validDescriptions() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '.', ',')
                .ofMinLength(1)
                .ofMaxLength(2000)
                .filter(s -> !s.isBlank());
    }
}
