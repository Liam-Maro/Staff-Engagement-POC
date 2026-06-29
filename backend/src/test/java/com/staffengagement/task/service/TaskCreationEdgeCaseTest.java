package com.staffengagement.task.service;

import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.interaction.service.InteractionService;
import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.shared.exception.TaskAssignmentForbiddenException;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskServiceImpl.create() edge cases.
 *
 * Validates: Requirements 1.7, 1.9, 4.2, 4.3
 */
@ExtendWith(MockitoExtension.class)
class TaskCreationEdgeCaseTest {

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

    private final UUID creatorId = UUID.randomUUID();
    private final UUID assigneeId = UUID.randomUUID();
    private final UUID individualId = UUID.randomUUID();

    // --- Requirement 1.9: Null dueDate creates task successfully ---

    @Test
    void create_withNullDueDate_shouldCreateTaskSuccessfully() {
        // Arrange: all fields valid, dueDate is null
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Valid description", null);
        stubActiveCreatorAndAssignee();
        when(employeeService.existsById(individualId)).thenReturn(true);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskResponse result = taskService.create(request, creatorId);

        // Assert: task created with null dueDate
        assertThat(result).isNotNull();
        assertThat(result.dueDate()).isNull();
        assertThat(result.status()).isEqualTo("To Do");
        assertThat(result.description()).isEqualTo("Valid description");
        assertThat(result.creatorId()).isEqualTo(creatorId);
        assertThat(result.assigneeId()).isEqualTo(assigneeId);
        verify(repository).save(any(Task.class));
    }

    // --- Requirement 1.7: Interaction validation skipped when interactionId is null ---

    @Test
    void create_withNullInteractionId_shouldNeverCallInteractionService() {
        // Arrange: interactionId is null, all other fields valid
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Task without interaction", null);
        stubActiveCreatorAndAssignee();
        when(employeeService.existsById(individualId)).thenReturn(true);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskResponse result = taskService.create(request, creatorId);

        // Assert: InteractionService is never called
        verify(interactionService, never()).findById(any());
        assertThat(result.interactionId()).isNull();
        assertThat(result.status()).isEqualTo("To Do");
    }

    // --- Description boundary: 2000-char description passes through to repository ---
    // Note: Length validation (max 2000) is enforced at the DTO/controller layer via @Size annotation.
    // The service layer accepts whatever passes DTO validation and persists it.

    @Test
    void create_with2000CharDescription_shouldSucceed() {
        // Arrange: exactly 2000-char description
        String description2000 = "A".repeat(2000);
        var request = new CreateTaskRequest(individualId, null, assigneeId, description2000, null);
        stubActiveCreatorAndAssignee();
        when(employeeService.existsById(individualId)).thenReturn(true);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskResponse result = taskService.create(request, creatorId);

        // Assert: 2000-char description persisted successfully
        assertThat(result.description()).hasSize(2000);
        assertThat(result.description()).isEqualTo(description2000);
        verify(repository).save(any(Task.class));
    }

    @Test
    void create_withLongDescription_shouldPassThroughToRepository() {
        // The service doesn't enforce length — that's the controller's job.
        // A 2001-char string that somehow bypasses DTO validation would still be persisted.
        String description2001 = "B".repeat(2001);
        var request = new CreateTaskRequest(individualId, null, assigneeId, description2001, null);
        stubActiveCreatorAndAssignee();
        when(employeeService.existsById(individualId)).thenReturn(true);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TaskResponse result = taskService.create(request, creatorId);

        // Assert: service doesn't reject — it passes through to repository
        assertThat(result.description()).hasSize(2001);
        verify(repository).save(any(Task.class));
    }

    // --- Requirement 4.3 / 4.2: Validation order — creator check executes before assignee check ---

    @Test
    void create_whenCreatorInactiveAndAssigneeNotFound_shouldThrowForbiddenForCreator() {
        // Arrange: creator is inactive, assignee doesn't exist
        // Per spec: creator validation happens FIRST (step 1), assignee check is step 2
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Description", null);
        var inactiveCreator = new StaffResponse(creatorId,
                "creator@example.com", StaffRole.ADMIN, false, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(inactiveCreator);
        // Assignee lookup should NOT be reached

        // Act & Assert: 403 for inactive creator, not 404 for missing assignee
        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(TaskAssignmentForbiddenException.class);

        // Verify assignee was never checked
        verify(staffService, never()).findById(assigneeId);
        verify(repository, never()).save(any());
    }

    @Test
    void create_whenCreatorNotFoundAndAssigneeNotFound_shouldThrowForbiddenForCreator() {
        // Arrange: creator doesn't exist at all, assignee also doesn't exist
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Description", null);

        when(staffService.findById(creatorId))
                .thenThrow(new EntityNotFoundException("Staff not found with id: " + creatorId));

        // Act & Assert: 403 for creator not found (mapped to forbidden), not 404 for assignee
        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(TaskAssignmentForbiddenException.class);

        // Verify assignee was never checked — creator validation failed first
        verify(staffService, times(1)).findById(creatorId);
        verify(staffService, never()).findById(assigneeId);
        verify(employeeService, never()).existsById(any());
        verify(repository, never()).save(any());
    }

    // --- Helper methods ---

    private void stubActiveCreatorAndAssignee() {
        var activeCreator = new StaffResponse(creatorId,
                "creator@example.com", StaffRole.ADMIN, true, LocalDateTime.now());
        var activeAssignee = new StaffResponse(assigneeId,
                "assignee@example.com", StaffRole.STAFF, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);
    }
}
