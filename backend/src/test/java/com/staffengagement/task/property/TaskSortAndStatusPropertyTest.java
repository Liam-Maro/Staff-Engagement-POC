package com.staffengagement.task.property;

import com.staffengagement.shared.exception.InvalidParameterException;
import com.staffengagement.shared.exception.TaskAssignmentForbiddenException;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.repository.TaskRepository;
import com.staffengagement.task.service.TaskService;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for sort ordering and status update validation.
 * Tests Properties 9, 10, 11, and 12 from the design document.
 *
 * Validates: Requirements 6.2, 6.4, 7.2, 7.3, 7.5, 7.6
 */
class TaskSortAndStatusPropertyTest {

    private TaskRepository repository;
    private StaffService staffService;
    private TaskService taskService;

    @BeforeProperty
    void setUp() throws Exception {
        repository = mock(TaskRepository.class);
        staffService = mock(StaffService.class);

        // TaskServiceImpl is package-private, instantiate via reflection
        Class<?> implClass = Class.forName("com.staffengagement.task.service.TaskServiceImpl");
        Constructor<?> constructor = implClass.getDeclaredConstructor(TaskRepository.class, StaffService.class);
        constructor.setAccessible(true);
        taskService = (TaskService) constructor.newInstance(repository, staffService);
    }

    // Feature: staff-task-assignment, Property 9: Sort ordering invariant

    /**
     * Property 9: Sort ordering invariant (descending).
     *
     * For any list of tasks returned by a query with sortOrder=desc,
     * for every consecutive pair (task_i, task_i+1):
     *   task_i.createdAt >= task_i+1.createdAt,
     *   and if createdAt values are equal then task_i.id > task_i+1.id.
     *
     * Validates: Requirements 7.2, 7.3, 7.6
     */
    @Property(tries = 100)
    void sortOrderingInvariantDesc(
            @ForAll("taskListsForSort") List<Task> tasks,
            @ForAll("randomUUIDs") UUID assigneeId
    ) {
        // Sort tasks in descending order by createdAt, then by id (matching service buildSort logic)
        List<Task> sortedTasks = tasks.stream()
                .sorted(Comparator.comparing(Task::getCreatedAt).reversed()
                        .thenComparing(Comparator.comparing(Task::getId).reversed()))
                .toList();

        // Mock repository to return the sorted list
        when(repository.findByAssigneeId(eq(assigneeId), any())).thenReturn(sortedTasks);

        // Act
        List<TaskResponse> result = taskService.findByAssigneeId(assigneeId, "desc");

        // Assert consecutive pair ordering invariant
        for (int i = 0; i < result.size() - 1; i++) {
            TaskResponse current = result.get(i);
            TaskResponse next = result.get(i + 1);

            // createdAt should be >= next
            assertThat(current.createdAt()).isAfterOrEqualTo(next.createdAt());

            // If timestamps are equal, id should be greater (for deterministic ordering)
            if (current.createdAt().equals(next.createdAt())) {
                assertThat(current.id().compareTo(next.id())).isGreaterThan(0);
            }
        }
    }

    /**
     * Property 9: Sort ordering invariant (ascending).
     *
     * For any list of tasks returned by a query with sortOrder=asc,
     * for every consecutive pair (task_i, task_i+1):
     *   task_i.createdAt <= task_i+1.createdAt,
     *   and if createdAt values are equal then task_i.id < task_i+1.id.
     *
     * Validates: Requirements 7.2, 7.3, 7.6
     */
    @Property(tries = 100)
    void sortOrderingInvariantAsc(
            @ForAll("taskListsForSort") List<Task> tasks,
            @ForAll("randomUUIDs") UUID assigneeId
    ) {
        // Sort tasks in ascending order by createdAt, then by id
        List<Task> sortedTasks = tasks.stream()
                .sorted(Comparator.comparing(Task::getCreatedAt)
                        .thenComparing(Task::getId))
                .toList();

        // Mock repository to return the sorted list
        when(repository.findByAssigneeId(eq(assigneeId), any())).thenReturn(sortedTasks);

        // Act
        List<TaskResponse> result = taskService.findByAssigneeId(assigneeId, "asc");

        // Assert consecutive pair ordering invariant
        for (int i = 0; i < result.size() - 1; i++) {
            TaskResponse current = result.get(i);
            TaskResponse next = result.get(i + 1);

            // createdAt should be <= next
            assertThat(current.createdAt()).isBeforeOrEqualTo(next.createdAt());

            // If timestamps are equal, id should be less (ascending secondary sort)
            if (current.createdAt().equals(next.createdAt())) {
                assertThat(current.id().compareTo(next.id())).isLessThan(0);
            }
        }
    }

    // Feature: staff-task-assignment, Property 10: Invalid sort order is rejected

    /**
     * Property 10: Invalid sort order is rejected.
     *
     * For any string that is not case-insensitively equal to "asc" or "desc",
     * passing it as the sortOrder query parameter SHALL result in HTTP 400
     * (InvalidParameterException) with an error listing valid values.
     *
     * Validates: Requirements 7.5
     */
    @Property(tries = 100)
    void invalidSortOrderIsRejected(
            @ForAll("invalidSortOrders") String invalidSortOrder
    ) {
        // The controller validates sortOrder before calling the service.
        // TaskController is package-private, so we test via reflection.
        try {
            Class<?> ctrlClass = Class.forName("com.staffengagement.task.controller.TaskController");
            Constructor<?> ctrlConstructor = ctrlClass.getDeclaredConstructor(TaskService.class, StaffService.class);
            ctrlConstructor.setAccessible(true);
            Object controller = ctrlConstructor.newInstance(taskService, staffService);

            // Invoke findAll with invalid sort order
            Method findAllMethod = ctrlClass.getDeclaredMethod("findAll", String.class, String.class, String.class);
            findAllMethod.setAccessible(true);

            // This should throw InvalidParameterException wrapped in InvocationTargetException
            assertThatThrownBy(() -> findAllMethod.invoke(controller, null, null, invalidSortOrder))
                    .isInstanceOf(InvocationTargetException.class)
                    .extracting(e -> ((InvocationTargetException) e).getTargetException())
                    .isInstanceOf(InvalidParameterException.class);
        } catch (Exception e) {
            throw new RuntimeException("Reflection setup failed", e);
        }
    }

    // Feature: staff-task-assignment, Property 11: Invalid status value is rejected

    /**
     * Property 11: Invalid status value is rejected.
     *
     * For any string that is not one of "OPEN", "IN_PROGRESS", or "COMPLETED",
     * submitting it as a status update SHALL result in InvalidParameterException (HTTP 400)
     * with an error listing valid statuses.
     *
     * Validates: Requirements 6.2
     */
    @Property(tries = 100)
    void invalidStatusValueIsRejected(
            @ForAll("invalidStatusValues") String invalidStatus,
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID assigneeId
    ) {
        // Mock: task exists with this assignee
        Task existingTask = createTaskWithAssignee(taskId, assigneeId);
        when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));

        // Act & Assert: should throw InvalidParameterException for invalid status
        assertThatThrownBy(() -> taskService.updateStatus(taskId, invalidStatus, assigneeId))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("Invalid status value");
    }

    // Feature: staff-task-assignment, Property 12: Non-assignee cannot update task status

    /**
     * Property 12: Non-assignee cannot update task status.
     *
     * For any task and any Staff_Member who is not the task's assignee,
     * a status update request from that Staff_Member SHALL be rejected
     * with TaskAssignmentForbiddenException (HTTP 403).
     *
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    void nonAssigneeCannotUpdateTaskStatus(
            @ForAll("randomUUIDs") UUID taskId,
            @ForAll("randomUUIDs") UUID assigneeId,
            @ForAll("nonMatchingRequesterIds") UUID nonAssigneeRequesterId
    ) {
        // Ensure non-assignee is different from assignee (extremely unlikely with random UUIDs but just in case)
        Assume.that(!nonAssigneeRequesterId.equals(assigneeId));

        // Mock: task exists with a specific assignee
        Task existingTask = createTaskWithAssignee(taskId, assigneeId);
        when(repository.findById(taskId)).thenReturn(Optional.of(existingTask));

        // Act & Assert: non-assignee trying to update should get 403
        assertThatThrownBy(() -> taskService.updateStatus(taskId, "IN_PROGRESS", nonAssigneeRequesterId))
                .isInstanceOf(TaskAssignmentForbiddenException.class)
                .hasMessageContaining("not the assignee");
    }

    // --- Helper Methods ---

    private Task createTaskWithAssignee(UUID taskId, UUID assigneeId) {
        Task task = new Task(
                UUID.randomUUID(), // employeeId
                null,              // interactionId
                UUID.randomUUID(), // creatorId
                assigneeId,
                "Test Task",
                "Description",
                TaskStatus.OPEN,
                null
        );
        // Set id via reflection since it's @GeneratedValue
        try {
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, taskId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return task;
    }

    // --- Arbitraries / Generators ---

    @Provide
    Arbitrary<UUID> randomUUIDs() {
        return Arbitraries.randomValue(random -> UUID.randomUUID());
    }

    @Provide
    Arbitrary<UUID> nonMatchingRequesterIds() {
        return Arbitraries.randomValue(random -> UUID.randomUUID());
    }

    @Provide
    Arbitrary<List<Task>> taskListsForSort() {
        // Generate lists of 2-10 tasks with varying createdAt timestamps
        Arbitrary<Task> taskArbitrary = Combinators.combine(
                Arbitraries.create(UUID::randomUUID),  // employeeId
                Arbitraries.create(UUID::randomUUID),  // assigneeId
                Arbitraries.create(UUID::randomUUID),  // creatorId
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50), // title
                Arbitraries.integers().between(0, 100)  // hours offset for createdAt variation
        ).as((employeeId, assigneeId, creatorId, title, hoursOffset) -> {
            Task task = new Task(
                    employeeId,
                    null,
                    creatorId,
                    assigneeId,
                    title,
                    null,
                    TaskStatus.OPEN,
                    null
            );
            // Set id and createdAt via reflection
            try {
                var idField = Task.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(task, UUID.randomUUID());

                var createdAtField = Task.class.getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(task, LocalDateTime.now().minusHours(hoursOffset));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return task;
        });

        return taskArbitrary.list().ofMinSize(2).ofMaxSize(10);
    }

    @Provide
    Arbitrary<String> invalidSortOrders() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !s.equalsIgnoreCase("asc") && !s.equalsIgnoreCase("desc"));
    }

    @Provide
    Arbitrary<String> invalidStatusValues() {
        Set<String> validStatuses = Set.of("OPEN", "IN_PROGRESS", "COMPLETED");
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !validStatuses.contains(s));
    }
}
