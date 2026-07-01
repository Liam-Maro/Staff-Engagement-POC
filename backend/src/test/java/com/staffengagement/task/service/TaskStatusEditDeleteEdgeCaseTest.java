package com.staffengagement.task.service;

import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.interaction.service.InteractionService;
import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.shared.exception.InvalidParameterException;
import com.staffengagement.shared.exception.TaskAssignmentForbiddenException;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.dto.UpdateStatusRequest;
import com.staffengagement.task.dto.UpdateTaskRequest;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskServiceImpl status update, edit, and delete edge cases.
 *
 * Validates: Requirements 6.1, 6.5, 8.5, 9.3
 */
@ExtendWith(MockitoExtension.class)
class TaskStatusEditDeleteEdgeCaseTest {

    @Mock
    private TaskRepository repository;

    @Mock
    private StaffService staffService;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private InteractionService interactionService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private final UUID taskId = UUID.randomUUID();
    private final UUID creatorId = UUID.randomUUID();
    private final UUID assigneeId = UUID.randomUUID();
    private final UUID individualId = UUID.randomUUID();

    // ========================================================================
    // Requirement 6.1, 6.6: Same-to-same status update succeeds
    // ========================================================================

    @Test
    void updateStatus_withSameCurrentStatus_shouldSucceed() {
        // Arrange: task currently has status TODO, request updates to TODO
        Task task = createTask(TaskStatus.TODO);
        when(repository.findById(taskId)).thenReturn(Optional.of(task));
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateStatusRequest("TODO");

        // Act
        TaskResponse result = taskService.updateStatus(taskId, request, assigneeId);

        // Assert: update succeeds, status remains TODO
        assertThat(result.status()).isEqualTo("To Do");
        verify(repository).save(task);
    }

    @Test
    void updateStatus_inProgressToInProgress_shouldSucceed() {
        // Arrange: task currently IN_PROGRESS, request updates to IN_PROGRESS
        Task task = createTask(TaskStatus.IN_PROGRESS);
        when(repository.findById(taskId)).thenReturn(Optional.of(task));
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateStatusRequest("IN_PROGRESS");

        // Act
        TaskResponse result = taskService.updateStatus(taskId, request, assigneeId);

        // Assert
        assertThat(result.status()).isEqualTo("In Progress");
        verify(repository).save(task);
    }

    // ========================================================================
    // Requirement 8.5: Interaction validation skipped when interactionId omitted on edit
    // ========================================================================

    @Test
    void update_withNullInteractionId_shouldNeverCallInteractionService() {
        // Arrange: edit request with interactionId = null
        Task task = createTask(TaskStatus.TODO);
        when(repository.findById(taskId)).thenReturn(Optional.of(task));

        // Requester (creator) is active
        var activeRequester = new StaffResponse(creatorId,
                "creator@example.com", StaffRole.STAFF, true, LocalDateTime.now());
        when(staffService.findById(creatorId)).thenReturn(activeRequester);

        var activeAssignee = new StaffResponse(assigneeId,
                "assignee@example.com", StaffRole.STAFF, true, LocalDateTime.now());
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);
        when(employeeService.existsById(individualId)).thenReturn(true);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateTaskRequest(individualId, null, assigneeId, "Updated description", null);

        // Act
        TaskResponse result = taskService.update(taskId, request, creatorId);

        // Assert: interactionService is never called when interactionId is null
        verify(interactionService, never()).findById(any());
        assertThat(result.interactionId()).isNull();
        assertThat(result.description()).isEqualTo("Updated description");
    }

    // ========================================================================
    // Requirement 9.3: Delete — task remains unchanged after 403 rejection
    // ========================================================================

    @Test
    void delete_whenRequesterIsNotCreator_shouldThrow403AndNeverDeleteOrModifyTask() {
        // Arrange: task exists, requester is NOT the creator
        Task task = createTask(TaskStatus.TODO);
        UUID nonCreatorId = UUID.randomUUID();
        when(repository.findById(taskId)).thenReturn(Optional.of(task));

        // Act & Assert: 403 thrown
        assertThatThrownBy(() -> taskService.delete(taskId, nonCreatorId))
                .isInstanceOf(TaskAssignmentForbiddenException.class);

        // Verify: repository.delete() was never called — no data modification
        verify(repository, never()).delete(any(Task.class));
        verify(repository, never()).deleteById(any());
        verify(repository, never()).flush();

        // Verify task fields unchanged (task object is same reference, never modified)
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getCreatorId()).isEqualTo(creatorId);
        assertThat(task.getAssigneeId()).isEqualTo(assigneeId);
        assertThat(task.getDescription()).isEqualTo("Test task description");
    }

    // ========================================================================
    // Requirement 6.5: Status update validation order
    // (existence → authorization → value validity)
    // ========================================================================

    @Test
    void updateStatus_whenTaskDoesNotExist_shouldThrow404() {
        // Arrange: task not found
        when(repository.findById(taskId)).thenReturn(Optional.empty());

        var request = new UpdateStatusRequest("INVALID_STATUS");

        // Act & Assert: 404 comes first, even though status value is also invalid
        assertThatThrownBy(() -> taskService.updateStatus(taskId, request, assigneeId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateStatus_whenRequesterIsNotAssignee_shouldThrow403BeforeValidatingStatusValue() {
        // Arrange: task exists, requester is NOT the assignee, status value is invalid
        Task task = createTask(TaskStatus.TODO);
        UUID nonAssigneeId = UUID.randomUUID();
        when(repository.findById(taskId)).thenReturn(Optional.of(task));

        var request = new UpdateStatusRequest("INVALID_STATUS");

        // Act & Assert: 403 comes before 400 (authorization before value validity)
        assertThatThrownBy(() -> taskService.updateStatus(taskId, request, nonAssigneeId))
                .isInstanceOf(TaskAssignmentForbiddenException.class);

        // Verify: no save was attempted
        verify(repository, never()).save(any());
    }

    @Test
    void updateStatus_whenRequesterIsAssigneeButStatusInvalid_shouldThrow400() {
        // Arrange: task exists, requester IS the assignee, but status value is invalid
        Task task = createTask(TaskStatus.TODO);
        when(repository.findById(taskId)).thenReturn(Optional.of(task));

        var request = new UpdateStatusRequest("INVALID_STATUS");

        // Act & Assert: 400 for invalid status value (after passing existence + authorization)
        assertThatThrownBy(() -> taskService.updateStatus(taskId, request, assigneeId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid status value");

        // Verify: no save was attempted
        verify(repository, never()).save(any());
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private Task createTask(TaskStatus status) {
        return new Task(individualId, null, creatorId, assigneeId,
                "Test task description", status, null);
    }
}
