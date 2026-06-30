package com.staffengagement.task.property;

import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.interaction.service.InteractionService;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.repository.TaskRepository;
import com.staffengagement.task.service.TaskService;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Feature: staff-task-assignment, Property 1: Task creation round-trip preserves all input data
class TaskCreationPropertyTest {

    private TaskRepository repository;
    private StaffService staffService;
    private EmployeeService employeeService;
    private InteractionService interactionService;
    private TaskService taskService;

    @BeforeProperty
    void setUp() throws Exception {
        repository = mock(TaskRepository.class);
        staffService = mock(StaffService.class);
        employeeService = mock(EmployeeService.class);
        interactionService = mock(InteractionService.class);

        // TaskServiceImpl is package-private, instantiate via reflection
        Class<?> implClass = Class.forName("com.staffengagement.task.service.TaskServiceImpl");
        Constructor<?> constructor = implClass.getDeclaredConstructor(
                TaskRepository.class, StaffService.class,
                EmployeeService.class, InteractionService.class);
        constructor.setAccessible(true);
        taskService = (TaskService) constructor.newInstance(repository, staffService, employeeService, interactionService);

        // Mock repository.save() to return the entity with an id and createdAt set
        when(repository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, UUID.randomUUID());
            return task;
        });
    }

    /**
     * Property 1: Task creation round-trip preserves all input data.
     * For any valid CreateTaskRequest with active staff members,
     * the TaskResponse contains matching fields, status=TODO, non-null id and createdAt.
     *
     * Validates: Requirements 1.1, 1.2, 5.1, 5.2
     */
    @Property(tries = 100)
    void taskCreationRoundTripPreservesAllInputData(
            @ForAll("validCreateTaskRequests") CreateTaskRequest request,
            @ForAll("randomUUIDs") UUID creatorId
    ) {
        // Mock staff service to return active staff for both creator and assignee
        StaffResponse activeCreator = new StaffResponse(
                creatorId, "creator@test.com", StaffRole.STAFF, true, LocalDateTime.now());
        StaffResponse activeAssignee = new StaffResponse(
                request.assigneeId(), "assignee@test.com", StaffRole.STAFF, true, LocalDateTime.now());

        when(staffService.findById(creatorId)).thenReturn(activeCreator);
        when(staffService.findById(request.assigneeId())).thenReturn(activeAssignee);
        when(employeeService.existsById(request.individualId())).thenReturn(true);

        // Act
        TaskResponse response = taskService.create(request, creatorId);

        // Assert round-trip preservation
        assertThat(response.id()).isNotNull();
        assertThat(response.individualId()).isEqualTo(request.individualId());
        assertThat(response.assigneeId()).isEqualTo(request.assigneeId());
        assertThat(response.creatorId()).isEqualTo(creatorId);
        assertThat(response.description()).isEqualTo(request.description());
        assertThat(response.dueDate()).isEqualTo(request.dueDate());
        assertThat(response.status()).isEqualTo(TaskStatus.TODO.getDisplayName());
        assertThat(response.createdAt()).isNotNull();
    }

    @Provide
    Arbitrary<CreateTaskRequest> validCreateTaskRequests() {
        Arbitrary<UUID> individualIds = Arbitraries.create(UUID::randomUUID);
        Arbitrary<UUID> assigneeIds = Arbitraries.create(UUID::randomUUID);
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

        // Use null interactionId to avoid needing interaction service mocking
        return Combinators.combine(individualIds, assigneeIds, descriptions, dueDates)
                .as((individualId, assigneeId, description, dueDate) ->
                        new CreateTaskRequest(individualId, null, assigneeId, description, dueDate));
    }

    @Provide
    Arbitrary<UUID> randomUUIDs() {
        return Arbitraries.create(UUID::randomUUID);
    }
}
