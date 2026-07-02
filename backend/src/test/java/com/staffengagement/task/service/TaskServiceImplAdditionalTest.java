package com.staffengagement.task.service;

import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.interaction.dto.InteractionResponse;
import com.staffengagement.interaction.model.InteractionType;
import com.staffengagement.interaction.service.InteractionService;
import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.shared.exception.InactiveStaffException;
import com.staffengagement.shared.exception.InvalidParameterException;
import com.staffengagement.shared.exception.TaskAssignmentForbiddenException;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.*;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplAdditionalTest {

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
    private final UUID taskId = UUID.randomUUID();

    private Task createTask() {
        Task task = new Task(individualId, null, creatorId, assigneeId, "Test task", TaskStatus.TODO, LocalDate.now().plusDays(7));
        setTaskId(task, taskId);
        return task;
    }

    private StaffResponse activeStaff(UUID id) {
        return new StaffResponse(id, "staff@example.com", StaffRole.STAFF, true, LocalDateTime.now());
    }

    private StaffResponse inactiveStaff(UUID id) {
        return new StaffResponse(id, "staff@example.com", StaffRole.STAFF, false, LocalDateTime.now());
    }

    // ==================== update() ====================

    @Test
    void update_shouldUpdateTaskFields_whenAllValid() {
        Task task = createTask();
        UUID newAssigneeId = UUID.randomUUID();
        var request = new UpdateTaskRequest(individualId, null, newAssigneeId, "Updated desc", LocalDate.now().plusDays(14));

        when(repository.findById(taskId)).thenReturn(Optional.of(task));
        when(staffService.findById(creatorId)).thenReturn(activeStaff(creatorId));
        when(staffService.findById(newAssigneeId)).thenReturn(activeStaff(newAssigneeId));
        when(employeeService.existsById(individualId)).thenReturn(true);
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse result = taskService.update(taskId, request, creatorId);

        assertThat(result.description()).isEqualTo("Updated desc");
        assertThat(result.assigneeId()).isEqualTo(newAssigneeId);
    }

    @Test
    void update_shouldThrowEntityNotFoundException_whenTaskNotFound() {
        var request = new UpdateTaskRequest(individualId, null, assigneeId, "Desc", null);
        when(repository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(taskId, request, creatorId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void update_shouldThrowForbidden_whenRequesterInactive() {
        Task task = createTask();
        var request = new UpdateTaskRequest(individualId, null, assigneeId, "Desc", null);

        when(repository.findById(taskId)).thenReturn(Optional.of(task));
        when(staffService.findById(creatorId)).thenReturn(inactiveStaff(creatorId));

        assertThatThrownBy(() -> taskService.update(taskId, request, creatorId))
                .isInstanceOf(TaskAssignmentForbiddenException.class);
    }

    @Test
    void update_shouldThrowEntityNotFoundException_whenAssigneeNotFound() {
        Task task = createTask();
        UUID newAssigneeId = UUID.randomUUID();
        var request = new UpdateTaskRequest(individualId, null, newAssigneeId, "Desc", null);

        when(repository.findById(taskId)).thenReturn(Optional.of(task));
        when(staffService.findById(creatorId)).thenReturn(activeStaff(creatorId));
        when(staffService.findById(newAssigneeId)).thenThrow(new EntityNotFoundException("Not found"));

        assertThatThrownBy(() -> taskService.update(taskId, request, creatorId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Assignee");
    }

    @Test
    void update_shouldThrowInactiveStaffException_whenAssigneeInactive() {
        Task task = createTask();
        UUID newAssigneeId = UUID.randomUUID();
        var request = new UpdateTaskRequest(individualId, null, newAssigneeId, "Desc", null);

        when(repository.findById(taskId)).thenReturn(Optional.of(task));
        when(staffService.findById(creatorId)).thenReturn(activeStaff(creatorId));
        when(staffService.findById(newAssigneeId)).thenReturn(inactiveStaff(newAssigneeId));

        assertThatThrownBy(() -> taskService.update(taskId, request, creatorId))
                .isInstanceOf(InactiveStaffException.class);
    }

    @Test
    void update_shouldThrowInvalidParameterException_whenIndividualNotFound() {
        Task task = createTask();
        var request = new UpdateTaskRequest(individualId, null, assigneeId, "Desc", null);

        when(repository.findById(taskId)).thenReturn(Optional.of(task));
        when(staffService.findById(creatorId)).thenReturn(activeStaff(creatorId));
        when(staffService.findById(assigneeId)).thenReturn(activeStaff(assigneeId));
        when(employeeService.existsById(individualId)).thenReturn(false);

        assertThatThrownBy(() -> taskService.update(taskId, request, creatorId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Individual");
    }

    @Test
    void update_shouldValidateInteractionBelongsToIndividual() {
        Task task = createTask();
        UUID interactionId = UUID.randomUUID();
        var request = new UpdateTaskRequest(individualId, interactionId, assigneeId, "Desc", null);

        when(repository.findById(taskId)).thenReturn(Optional.of(task));
        when(staffService.findById(creatorId)).thenReturn(activeStaff(creatorId));
        when(staffService.findById(assigneeId)).thenReturn(activeStaff(assigneeId));
        when(employeeService.existsById(individualId)).thenReturn(true);

        UUID otherEmployeeId = UUID.randomUUID();
        var interactionResp = new InteractionResponse(interactionId, otherEmployeeId,
                UUID.randomUUID(), InteractionType.CHECK_IN, "notes", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(interactionService.findById(interactionId)).thenReturn(interactionResp);

        assertThatThrownBy(() -> taskService.update(taskId, request, creatorId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void update_shouldThrowInvalidParameterException_whenInteractionNotFound() {
        Task task = createTask();
        UUID interactionId = UUID.randomUUID();
        var request = new UpdateTaskRequest(individualId, interactionId, assigneeId, "Desc", null);

        when(repository.findById(taskId)).thenReturn(Optional.of(task));
        when(staffService.findById(creatorId)).thenReturn(activeStaff(creatorId));
        when(staffService.findById(assigneeId)).thenReturn(activeStaff(assigneeId));
        when(employeeService.existsById(individualId)).thenReturn(true);
        when(interactionService.findById(interactionId)).thenThrow(new RuntimeException("Not found"));

        assertThatThrownBy(() -> taskService.update(taskId, request, creatorId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Interaction not found");
    }

    @Test
    void update_shouldThrowInvalidParameterException_whenDueDateInPast() {
        Task task = createTask();
        var request = new UpdateTaskRequest(individualId, null, assigneeId, "Desc", LocalDate.now().minusDays(1));

        when(repository.findById(taskId)).thenReturn(Optional.of(task));
        when(staffService.findById(creatorId)).thenReturn(activeStaff(creatorId));
        when(staffService.findById(assigneeId)).thenReturn(activeStaff(assigneeId));
        when(employeeService.existsById(individualId)).thenReturn(true);

        assertThatThrownBy(() -> taskService.update(taskId, request, creatorId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("past");
    }

    // ==================== updateStatus() ====================

    @Test
    void updateStatus_shouldUpdateStatus_whenRequesterIsAssignee() {
        Task task = createTask();
        var request = new UpdateStatusRequest("In Progress");

        when(repository.findById(taskId)).thenReturn(Optional.of(task));
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse result = taskService.updateStatus(taskId, request, assigneeId);

        assertThat(result.status()).isEqualTo("In Progress");
    }

    @Test
    void updateStatus_shouldThrowEntityNotFoundException_whenTaskNotFound() {
        var request = new UpdateStatusRequest("Done");
        when(repository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateStatus(taskId, request, assigneeId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateStatus_shouldThrowForbidden_whenRequesterIsNotAssignee() {
        Task task = createTask();
        UUID otherStaffId = UUID.randomUUID();
        var request = new UpdateStatusRequest("Done");

        when(repository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(taskId, request, otherStaffId))
                .isInstanceOf(TaskAssignmentForbiddenException.class);
    }

    @Test
    void updateStatus_shouldThrowInvalidParameterException_whenStatusInvalid() {
        Task task = createTask();
        var request = new UpdateStatusRequest("INVALID_STATUS");

        when(repository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(taskId, request, assigneeId))
                .isInstanceOf(InvalidParameterException.class);
    }

    // ==================== delete() ====================

    @Test
    void delete_shouldDeleteTask_whenRequesterIsCreator() {
        Task task = createTask();
        when(repository.findById(taskId)).thenReturn(Optional.of(task));

        taskService.delete(taskId, creatorId);

        verify(repository).delete(task);
        verify(repository).flush();
    }

    @Test
    void delete_shouldThrowEntityNotFoundException_whenTaskNotFound() {
        when(repository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(taskId, creatorId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_shouldThrowForbidden_whenRequesterIsNotCreator() {
        Task task = createTask();
        UUID otherStaffId = UUID.randomUUID();
        when(repository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.delete(taskId, otherStaffId))
                .isInstanceOf(TaskAssignmentForbiddenException.class);
    }

    // ==================== findTasks() ====================

    @Test
    @SuppressWarnings("unchecked")
    void findTasks_shouldReturnPaginatedResults() {
        Task task = createTask();
        Page<Task> page = new PageImpl<>(List.of(task));

        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var params = new TaskQueryParams(
                assigneeId, creatorId, false, TaskStatus.TODO,
                LocalDate.now().minusDays(7), LocalDate.now(),
                LocalDate.now().minusDays(30), LocalDate.now(),
                "dueDate", "asc", 0, 50
        );

        TaskQueryResult result = taskService.findTasks(params);

        assertThat(result.tasks()).hasSize(1);
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.currentPage()).isZero();
        assertThat(result.pageSize()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findTasks_shouldWorkWithNullFilters() {
        Page<Task> page = new PageImpl<>(List.of());
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var params = new TaskQueryParams(
                null, null, null, null,
                null, null, null, null,
                "createdDate", "desc", 0, 50
        );

        TaskQueryResult result = taskService.findTasks(params);

        assertThat(result.tasks()).isEmpty();
        assertThat(result.totalCount()).isZero();
    }

    // ==================== findByStaffId() ====================

    @Test
    void findByStaffId_shouldReturnTasksForAssignee() {
        Task task = createTask();
        when(repository.findByAssigneeId(assigneeId)).thenReturn(List.of(task));

        List<TaskResponse> result = taskService.findByStaffId(assigneeId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).assigneeId()).isEqualTo(assigneeId);
    }

    // ==================== getInteractionsForIndividual() ====================

    @Test
    @SuppressWarnings("unchecked")
    void getInteractionsForIndividual_shouldReturnInteractions_whenIndividualExists() {
        when(employeeService.existsById(individualId)).thenReturn(true);

        var interaction = new InteractionResponse(
                UUID.randomUUID(), individualId, UUID.randomUUID(),
                InteractionType.CHECK_IN, "Notes", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
        Page<InteractionResponse> page = new PageImpl<>(List.of(interaction));
        when(interactionService.findAll(eq(individualId), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        var result = taskService.getInteractionsForIndividual(individualId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).employeeId()).isEqualTo(individualId);
    }

    @Test
    void getInteractionsForIndividual_shouldThrowEntityNotFoundException_whenIndividualNotFound() {
        when(employeeService.existsById(individualId)).thenReturn(false);

        assertThatThrownBy(() -> taskService.getInteractionsForIndividual(individualId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Individual");
    }

    // ==================== create with interactionId ====================

    @Test
    void create_shouldValidateInteractionBelongsToIndividual() {
        UUID interactionId = UUID.randomUUID();
        var request = new CreateTaskRequest(individualId, interactionId, assigneeId, "Desc", null);

        when(staffService.findById(creatorId)).thenReturn(activeStaff(creatorId));
        when(staffService.findById(assigneeId)).thenReturn(activeStaff(assigneeId));
        when(employeeService.existsById(individualId)).thenReturn(true);

        UUID otherEmployeeId = UUID.randomUUID();
        var interactionResp = new InteractionResponse(interactionId, otherEmployeeId,
                UUID.randomUUID(), InteractionType.CHECK_IN, "notes", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(interactionService.findById(interactionId)).thenReturn(interactionResp);

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void create_shouldThrowInvalidParameterException_whenInteractionNotFound() {
        UUID interactionId = UUID.randomUUID();
        var request = new CreateTaskRequest(individualId, interactionId, assigneeId, "Desc", null);

        when(staffService.findById(creatorId)).thenReturn(activeStaff(creatorId));
        when(staffService.findById(assigneeId)).thenReturn(activeStaff(assigneeId));
        when(employeeService.existsById(individualId)).thenReturn(true);
        when(interactionService.findById(interactionId)).thenThrow(new RuntimeException("Not found"));

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Interaction not found");
    }

    @Test
    void create_shouldThrowEntityNotFoundException_whenIndividualNotFound() {
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Desc", null);

        when(staffService.findById(creatorId)).thenReturn(activeStaff(creatorId));
        when(staffService.findById(assigneeId)).thenReturn(activeStaff(assigneeId));
        when(employeeService.existsById(individualId)).thenReturn(false);

        assertThatThrownBy(() -> taskService.create(request, creatorId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Individual");
    }

    // ==================== Helpers ====================

    private void setTaskId(Task task, UUID id) {
        try {
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
