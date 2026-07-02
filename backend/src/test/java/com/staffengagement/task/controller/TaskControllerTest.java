package com.staffengagement.task.controller;

import com.staffengagement.staff.model.Staff;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.task.dto.*;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskController covering create, update, updateStatus, delete,
 * findById, and findByStaffId methods that need authentication principal.
 */
class TaskControllerTest {

    private TaskService taskService;
    private TaskController controller;

    private final UUID staffId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();
    private final UUID individualId = UUID.randomUUID();
    private final UUID assigneeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        taskService = mock(TaskService.class);
        controller = new TaskController(taskService);
    }

    private Staff createStaff() {
        Staff staff = new Staff();
        staff.setEmail("test@example.com");
        staff.setPassword("password");
        staff.setRole(StaffRole.STAFF);
        staff.setActive(true);
        // Set ID via reflection since there's no setter
        try {
            var idField = Staff.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(staff, staffId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return staff;
    }

    @Test
    void create_shouldDelegateToServiceWithCreatorId() {
        Staff staff = createStaff();
        var request = new CreateTaskRequest(individualId, null, assigneeId, "Task desc", LocalDate.now().plusDays(7));
        var expectedResponse = new TaskResponse(taskId, individualId, null, staffId, assigneeId, "Task desc", "To Do", LocalDate.now().plusDays(7), LocalDateTime.now());

        when(taskService.create(any(CreateTaskRequest.class), eq(staffId))).thenReturn(expectedResponse);

        TaskResponse result = controller.create(request, staff);

        assertThat(result.creatorId()).isEqualTo(staffId);
        assertThat(result.description()).isEqualTo("Task desc");
        verify(taskService).create(request, staffId);
    }

    @Test
    void update_shouldDelegateToServiceWithRequesterIdAndParsedTaskId() {
        Staff staff = createStaff();
        var request = new UpdateTaskRequest(individualId, null, assigneeId, "Updated", null);
        var expectedResponse = new TaskResponse(taskId, individualId, null, staffId, assigneeId, "Updated", "To Do", null, LocalDateTime.now());

        when(taskService.update(eq(taskId), any(UpdateTaskRequest.class), eq(staffId))).thenReturn(expectedResponse);

        TaskResponse result = controller.update(taskId.toString(), request, staff);

        assertThat(result.description()).isEqualTo("Updated");
        verify(taskService).update(taskId, request, staffId);
    }

    @Test
    void updateStatus_shouldDelegateToServiceWithRequesterIdAndParsedTaskId() {
        Staff staff = createStaff();
        var request = new UpdateStatusRequest("In Progress");
        var expectedResponse = new TaskResponse(taskId, individualId, null, staffId, assigneeId, "Desc", "In Progress", null, LocalDateTime.now());

        when(taskService.updateStatus(eq(taskId), any(UpdateStatusRequest.class), eq(staffId))).thenReturn(expectedResponse);

        TaskResponse result = controller.updateStatus(taskId.toString(), request, staff);

        assertThat(result.status()).isEqualTo("In Progress");
        verify(taskService).updateStatus(taskId, request, staffId);
    }

    @Test
    void delete_shouldDelegateToServiceWithRequesterIdAndParsedTaskId() {
        Staff staff = createStaff();

        controller.delete(taskId.toString(), staff);

        verify(taskService).delete(taskId, staffId);
    }

    @Test
    void findById_shouldDelegateToService() {
        var expectedResponse = new TaskResponse(taskId, individualId, null, staffId, assigneeId, "Desc", "To Do", null, LocalDateTime.now());
        when(taskService.findById(taskId)).thenReturn(expectedResponse);

        TaskResponse result = controller.findById(taskId.toString());

        assertThat(result.id()).isEqualTo(taskId);
        verify(taskService).findById(taskId);
    }

    @Test
    void findByStaffId_shouldDelegateToService() {
        var expectedResponse = new TaskResponse(taskId, individualId, null, staffId, assigneeId, "Desc", "To Do", null, LocalDateTime.now());
        when(taskService.findByStaffId(staffId)).thenReturn(List.of(expectedResponse));

        List<TaskResponse> result = controller.findByStaffId(staffId);

        assertThat(result).hasSize(1);
        verify(taskService).findByStaffId(staffId);
    }

    @Test
    void findTasks_shouldDelegateToServiceWithParsedParams() {
        var expectedResult = new TaskQueryResult(List.of(), 0L, 0, 50);
        when(taskService.findTasks(any(TaskQueryParams.class))).thenReturn(expectedResult);

        TaskQueryResult result = controller.findTasks(
                null, null, null, null, null, null, null, null,
                "createdDate", "desc", 0, 50);

        assertThat(result.totalCount()).isZero();
        verify(taskService).findTasks(any(TaskQueryParams.class));
    }
}
