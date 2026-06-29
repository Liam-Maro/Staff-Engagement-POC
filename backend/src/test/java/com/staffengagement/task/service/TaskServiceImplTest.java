package com.staffengagement.task.service;

import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.interaction.service.InteractionService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

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

    @Test
    void create_shouldCreateTaskWithStatusTodo_whenAllValidationsPass() {
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Description", LocalDate.now().plusDays(7));
        var activeCreator = new StaffResponse(creatorId, "creator@example.com", StaffRole.ADMIN, true, LocalDateTime.now());
        var activeAssignee = new StaffResponse(assigneeId, "assignee@example.com", StaffRole.STAFF, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);
        when(employeeService.existsById(individualId)).thenReturn(true);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.create(request, creatorId);

        assertThat(result.status()).isEqualTo("To Do");
        assertThat(result.creatorId()).isEqualTo(creatorId);
        assertThat(result.assigneeId()).isEqualTo(assigneeId);
        assertThat(result.individualId()).isEqualTo(individualId);
        assertThat(result.description()).isEqualTo("Description");
        verify(repository).save(any(Task.class));
    }

    @Test
    void create_shouldThrowTaskAssignmentForbiddenException_whenCreatorNotFound() {
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Description", null);

        when(staffService.findById(creatorId)).thenThrow(new EntityNotFoundException("Staff not found with id: " + creatorId));

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(TaskAssignmentForbiddenException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldThrowTaskAssignmentForbiddenException_whenCreatorIsInactive() {
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Description", null);
        var inactiveCreator = new StaffResponse(creatorId, "creator@example.com", StaffRole.ADMIN, false, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(inactiveCreator);

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(TaskAssignmentForbiddenException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldThrowEntityNotFoundException_whenAssigneeNotFound() {
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Description", null);
        var activeCreator = new StaffResponse(creatorId, "creator@example.com", StaffRole.ADMIN, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenThrow(new EntityNotFoundException("Staff not found with id: " + assigneeId));

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldThrowInactiveStaffException_whenAssigneeIsInactive() {
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Description", null);
        var activeCreator = new StaffResponse(creatorId, "creator@example.com", StaffRole.ADMIN, true, LocalDateTime.now());
        var inactiveAssignee = new StaffResponse(assigneeId, "assignee@example.com", StaffRole.STAFF, false, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenReturn(inactiveAssignee);

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(InactiveStaffException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldThrowInvalidParameterException_whenDueDateIsInThePast() {
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Description", LocalDate.now().minusDays(1));
        var activeCreator = new StaffResponse(creatorId, "creator@example.com", StaffRole.ADMIN, true, LocalDateTime.now());
        var activeAssignee = new StaffResponse(assigneeId, "assignee@example.com", StaffRole.STAFF, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);
        when(employeeService.existsById(individualId)).thenReturn(true);

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(InvalidParameterException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldAllowNullDueDate() {
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Description", null);
        var activeCreator = new StaffResponse(creatorId, "creator@example.com", StaffRole.ADMIN, true, LocalDateTime.now());
        var activeAssignee = new StaffResponse(assigneeId, "assignee@example.com", StaffRole.STAFF, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);
        when(employeeService.existsById(individualId)).thenReturn(true);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.create(request, creatorId);

        assertThat(result.dueDate()).isNull();
        assertThat(result.status()).isEqualTo("To Do");
    }

    @Test
    void create_shouldAllowTodayAsDueDate() {
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Description", LocalDate.now());
        var activeCreator = new StaffResponse(creatorId, "creator@example.com", StaffRole.ADMIN, true, LocalDateTime.now());
        var activeAssignee = new StaffResponse(assigneeId, "assignee@example.com", StaffRole.STAFF, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(assigneeId)).thenReturn(activeAssignee);
        when(employeeService.existsById(individualId)).thenReturn(true);
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse result = taskService.create(request, creatorId);

        assertThat(result.dueDate()).isEqualTo(LocalDate.now());
        assertThat(result.status()).isEqualTo("To Do");
    }

    @Test
    void findById_shouldReturnTask_whenTaskExists() {
        UUID taskId = UUID.randomUUID();
        var task = new Task(individualId, null, creatorId, assigneeId, "Desc", TaskStatus.TODO, null);
        try {
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, taskId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(repository.findById(taskId)).thenReturn(Optional.of(task));

        TaskResponse result = taskService.findById(taskId);

        assertThat(result.id()).isEqualTo(taskId);
        assertThat(result.description()).isEqualTo("Desc");
    }

    @Test
    void findById_shouldThrowEntityNotFoundException_whenTaskNotFound() {
        UUID taskId = UUID.randomUUID();
        when(repository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(taskId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
