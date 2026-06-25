package com.staffengagement.task.service;

import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.shared.exception.InactiveStaffException;
import com.staffengagement.shared.exception.InvalidParameterException;
import com.staffengagement.shared.exception.TaskAssignmentForbiddenException;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository repository;

    @Mock
    private StaffService staffService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private final UUID creatorId = UUID.randomUUID();
    private final UUID assigneeId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();

    @Test
    void create_shouldCreateTaskWithStatusOpen_whenAllValidationsPass() {
        var request = new CreateTaskRequest(employeeId, null, assigneeId, "Test Task", "Description", LocalDate.now().plusDays(7));
        var activeCreator = new StaffResponse(creatorId, UUID.randomUUID(), "creator@example.com", StaffRole.ADMIN, true, LocalDateTime.now());
        var activeAssignee = new StaffResponse(assigneeId, UUID.randomUUID(), "assignee@example.com", StaffRole.STAFF, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.create(request, creatorId);

        assertThat(result.status()).isEqualTo("OPEN");
        assertThat(result.creatorId()).isEqualTo(creatorId);
        assertThat(result.assigneeId()).isEqualTo(assigneeId);
        assertThat(result.title()).isEqualTo("Test Task");
        assertThat(result.employeeId()).isEqualTo(employeeId);
        verify(repository).save(any(Task.class));
    }

    @Test
    void create_shouldThrowTaskAssignmentForbiddenException_whenCreatorNotFound() {
        var request = new CreateTaskRequest(employeeId, null, assigneeId, "Test Task", null, null);

        when(staffService.findById(creatorId)).thenThrow(new EntityNotFoundException("Staff not found with id: " + creatorId));

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(TaskAssignmentForbiddenException.class)
                .hasMessage("Creator is not an active staff member");

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldThrowTaskAssignmentForbiddenException_whenCreatorIsInactive() {
        var request = new CreateTaskRequest(employeeId, null, assigneeId, "Test Task", null, null);
        var inactiveCreator = new StaffResponse(creatorId, UUID.randomUUID(), "creator@example.com", StaffRole.ADMIN, false, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(inactiveCreator);

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(TaskAssignmentForbiddenException.class)
                .hasMessage("Creator is not an active staff member");

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldThrowEntityNotFoundException_whenAssigneeNotFound() {
        var request = new CreateTaskRequest(employeeId, null, assigneeId, "Test Task", null, null);
        var activeCreator = new StaffResponse(creatorId, UUID.randomUUID(), "creator@example.com", StaffRole.ADMIN, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenThrow(new EntityNotFoundException("Staff not found with id: " + assigneeId));

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldThrowInactiveStaffException_whenAssigneeIsInactive() {
        var request = new CreateTaskRequest(employeeId, null, assigneeId, "Test Task", null, null);
        var activeCreator = new StaffResponse(creatorId, UUID.randomUUID(), "creator@example.com", StaffRole.ADMIN, true, LocalDateTime.now());
        var inactiveAssignee = new StaffResponse(assigneeId, UUID.randomUUID(), "assignee@example.com", StaffRole.STAFF, false, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenReturn(inactiveAssignee);

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(InactiveStaffException.class)
                .hasMessage("Tasks cannot be assigned to inactive staff members");

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldThrowInvalidParameterException_whenDueDateIsInThePast() {
        var request = new CreateTaskRequest(employeeId, null, assigneeId, "Test Task", null, LocalDate.now().minusDays(1));
        var activeCreator = new StaffResponse(creatorId, UUID.randomUUID(), "creator@example.com", StaffRole.ADMIN, true, LocalDateTime.now());
        var activeAssignee = new StaffResponse(assigneeId, UUID.randomUUID(), "assignee@example.com", StaffRole.STAFF, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("Due date must be today or in the future");

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldAllowNullDueDate() {
        var request = new CreateTaskRequest(employeeId, null, assigneeId, "Test Task", "Desc", null);
        var activeCreator = new StaffResponse(creatorId, UUID.randomUUID(), "creator@example.com", StaffRole.ADMIN, true, LocalDateTime.now());
        var activeAssignee = new StaffResponse(assigneeId, UUID.randomUUID(), "assignee@example.com", StaffRole.STAFF, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.create(request, creatorId);

        assertThat(result.dueDate()).isNull();
        assertThat(result.status()).isEqualTo("OPEN");
    }

    @Test
    void create_shouldAllowTodayAsDueDate() {
        var request = new CreateTaskRequest(employeeId, null, assigneeId, "Test Task", null, LocalDate.now());
        var activeCreator = new StaffResponse(creatorId, UUID.randomUUID(), "creator@example.com", StaffRole.ADMIN, true, LocalDateTime.now());
        var activeAssignee = new StaffResponse(assigneeId, UUID.randomUUID(), "assignee@example.com", StaffRole.STAFF, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.create(request, creatorId);

        assertThat(result.dueDate()).isEqualTo(LocalDate.now());
        assertThat(result.status()).isEqualTo("OPEN");
    }

    @Test
    void findByAssigneeId_shouldReturnMatchingTasksSortedCorrectly() {
        var task1 = new Task(employeeId, null, creatorId, assigneeId, "Task 1", "Desc 1", TaskStatus.OPEN, null);
        var task2 = new Task(employeeId, null, creatorId, assigneeId, "Task 2", "Desc 2", TaskStatus.IN_PROGRESS, null);
        List<Task> tasks = List.of(task2, task1);

        Sort expectedSort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        when(repository.findByAssigneeId(eq(assigneeId), eq(expectedSort))).thenReturn(tasks);

        List<TaskResponse> result = taskService.findByAssigneeId(assigneeId, "desc");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("Task 2");
        assertThat(result.get(1).title()).isEqualTo("Task 1");
        verify(repository).findByAssigneeId(eq(assigneeId), eq(expectedSort));
    }

    @Test
    void findByCreatorId_shouldReturnMatchingTasksSortedCorrectly() {
        var task1 = new Task(employeeId, null, creatorId, assigneeId, "Task A", null, TaskStatus.OPEN, null);
        var task2 = new Task(employeeId, null, creatorId, assigneeId, "Task B", null, TaskStatus.COMPLETED, null);
        List<Task> tasks = List.of(task1, task2);

        Sort expectedSort = Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"));
        when(repository.findByCreatorId(eq(creatorId), eq(expectedSort))).thenReturn(tasks);

        List<TaskResponse> result = taskService.findByCreatorId(creatorId, "asc");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("Task A");
        assertThat(result.get(1).title()).isEqualTo("Task B");
        verify(repository).findByCreatorId(eq(creatorId), eq(expectedSort));
    }

    @Test
    void findByAssigneeId_shouldReturnEmptyList_whenNoTasksMatch() {
        UUID unknownAssigneeId = UUID.randomUUID();
        Sort expectedSort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        when(repository.findByAssigneeId(eq(unknownAssigneeId), eq(expectedSort))).thenReturn(Collections.emptyList());

        List<TaskResponse> result = taskService.findByAssigneeId(unknownAssigneeId, "desc");

        assertThat(result).isEmpty();
    }

    @Test
    void updateStatus_shouldSucceed_whenRequesterIsAssignee() {
        UUID taskId = UUID.randomUUID();
        var task = new Task(employeeId, null, creatorId, assigneeId, "Task", "Desc", TaskStatus.OPEN, null);
        // Use reflection to set the ID since Task has no setId
        try {
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, taskId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(repository.findById(taskId)).thenReturn(Optional.of(task));
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.updateStatus(taskId, "IN_PROGRESS", assigneeId);

        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        verify(repository).save(task);
    }

    @Test
    void updateStatus_shouldThrowTaskAssignmentForbiddenException_whenRequesterIsNotAssignee() {
        UUID taskId = UUID.randomUUID();
        UUID nonAssigneeId = UUID.randomUUID();
        var task = new Task(employeeId, null, creatorId, assigneeId, "Task", "Desc", TaskStatus.OPEN, null);
        try {
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, taskId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(repository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(taskId, "IN_PROGRESS", nonAssigneeId))
                .isInstanceOf(TaskAssignmentForbiddenException.class)
                .hasMessage("Staff member is not the assignee of this task");

        verify(repository, never()).save(any());
    }

    @Test
    void updateStatus_shouldThrowInvalidParameterException_whenStatusIsInvalid() {
        UUID taskId = UUID.randomUUID();
        var task = new Task(employeeId, null, creatorId, assigneeId, "Task", "Desc", TaskStatus.OPEN, null);
        try {
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, taskId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(repository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(taskId, "INVALID_STATUS", assigneeId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid status value");

        verify(repository, never()).save(any());
    }

    @Test
    void backwardCompatibility_shouldReturnNullCreatorAndAssignee_whenTaskHasNullFields() {
        UUID taskId = UUID.randomUUID();
        var task = new Task(employeeId, null, null, null, "Legacy Task", "Old task", TaskStatus.OPEN, null);
        try {
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, taskId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(repository.findById(taskId)).thenReturn(Optional.of(task));

        TaskResponse result = taskService.findById(taskId);

        assertThat(result.creatorId()).isNull();
        assertThat(result.assigneeId()).isNull();
        assertThat(result.title()).isEqualTo("Legacy Task");
        assertThat(result.status()).isEqualTo("OPEN");
    }
}
