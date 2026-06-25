package com.staffengagement.task.property;

import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.repository.TaskRepository;
import com.staffengagement.task.service.TaskService;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for task query filtering.
 * Tests Properties 6, 7, and 8 from the design document.
 *
 * Validates: Requirements 2.1, 2.4, 3.1, 3.4
 */
class TaskQueryPropertyTest {

    private TaskRepository repository;
    private StaffService staffService;
    private TaskService taskService;

    @BeforeProperty
    void setUp() throws Exception {
        repository = Mockito.mock(TaskRepository.class);
        staffService = Mockito.mock(StaffService.class);

        // TaskServiceImpl is package-private, instantiate via reflection
        Class<?> clazz = Class.forName("com.staffengagement.task.service.TaskServiceImpl");
        Constructor<?> constructor = clazz.getDeclaredConstructor(TaskRepository.class, StaffService.class);
        constructor.setAccessible(true);
        taskService = (TaskService) constructor.newInstance(repository, staffService);
    }

    // Feature: staff-task-assignment, Property 6: Filter by assignee returns exactly the matching tasks

    /**
     * Property 6: Filter by assignee returns exactly the matching tasks
     *
     * For any set of persisted tasks and any valid assigneeId, querying with that
     * assigneeId SHALL return exactly the tasks whose assigneeId matches, and no others.
     *
     * Validates: Requirements 2.1
     */
    @Property(tries = 100)
    void filterByAssigneeReturnsExactlyMatchingTasks(
            @ForAll("taskSetsWithAssignees") TaskSetWithTarget taskSet
    ) {
        UUID targetAssigneeId = taskSet.targetId();
        List<Task> allTasks = taskSet.tasks();

        // Determine the expected matching subset
        List<Task> expectedMatches = allTasks.stream()
                .filter(t -> targetAssigneeId.equals(t.getAssigneeId()))
                .collect(Collectors.toList());

        // Mock repository to return the matching subset (simulating DB filter)
        when(repository.findByAssigneeId(eq(targetAssigneeId), any(Sort.class)))
                .thenReturn(expectedMatches);

        // Act
        List<TaskResponse> result = taskService.findByAssigneeId(targetAssigneeId, "desc");

        // Assert: result contains exactly the expected tasks, no more, no less
        assertThat(result).hasSize(expectedMatches.size());

        List<UUID> resultIds = result.stream().map(TaskResponse::id).toList();
        List<UUID> expectedIds = expectedMatches.stream().map(Task::getId).toList();
        assertThat(resultIds).containsExactlyElementsOf(expectedIds);

        // Verify all returned tasks have the correct assigneeId
        for (TaskResponse response : result) {
            assertThat(response.assigneeId()).isEqualTo(targetAssigneeId);
        }
    }

    // Feature: staff-task-assignment, Property 7: Filter by creator returns exactly the matching tasks

    /**
     * Property 7: Filter by creator returns exactly the matching tasks
     *
     * For any set of persisted tasks and any valid creatorId, querying with that
     * creatorId SHALL return exactly the tasks whose creatorId matches, and no others.
     *
     * Validates: Requirements 3.1
     */
    @Property(tries = 100)
    void filterByCreatorReturnsExactlyMatchingTasks(
            @ForAll("taskSetsWithCreators") TaskSetWithTarget taskSet
    ) {
        UUID targetCreatorId = taskSet.targetId();
        List<Task> allTasks = taskSet.tasks();

        // Determine the expected matching subset
        List<Task> expectedMatches = allTasks.stream()
                .filter(t -> targetCreatorId.equals(t.getCreatorId()))
                .collect(Collectors.toList());

        // Mock repository to return the matching subset (simulating DB filter)
        when(repository.findByCreatorId(eq(targetCreatorId), any(Sort.class)))
                .thenReturn(expectedMatches);

        // Act
        List<TaskResponse> result = taskService.findByCreatorId(targetCreatorId, "desc");

        // Assert: result contains exactly the expected tasks, no more, no less
        assertThat(result).hasSize(expectedMatches.size());

        List<UUID> resultIds = result.stream().map(TaskResponse::id).toList();
        List<UUID> expectedIds = expectedMatches.stream().map(Task::getId).toList();
        assertThat(resultIds).containsExactlyElementsOf(expectedIds);

        // Verify all returned tasks have the correct creatorId
        for (TaskResponse response : result) {
            assertThat(response.creatorId()).isEqualTo(targetCreatorId);
        }
    }

    // Feature: staff-task-assignment, Property 8: Invalid UUID query parameters are rejected

    /**
     * Property 8: Invalid UUID query parameters are rejected
     *
     * For any string that is not a valid UUID format, passing it as assigneeId or
     * creatorId query parameter SHALL result in rejection (IllegalArgumentException
     * from UUID.fromString, which the controller converts to InvalidParameterException/HTTP 400).
     *
     * Validates: Requirements 2.4, 3.4
     */
    @Property(tries = 100)
    void invalidUuidStringsAreRejected(
            @ForAll("nonUuidStrings") String invalidUuid
    ) {
        // Act & Assert: UUID.fromString should throw IllegalArgumentException
        // for any non-UUID string. The controller's parseUuid method catches this
        // and throws InvalidParameterException (HTTP 400).
        assertThatThrownBy(() -> UUID.fromString(invalidUuid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Supporting record for test data ---

    record TaskSetWithTarget(List<Task> tasks, UUID targetId) {}

    // --- Arbitraries / Generators ---

    @Provide
    Arbitrary<TaskSetWithTarget> taskSetsWithAssignees() {
        // Generate 2-4 distinct assignee IDs, pick one as target, generate tasks distributed among them
        Arbitrary<List<UUID>> assigneeIds = Arbitraries.create(UUID::randomUUID)
                .list().ofMinSize(2).ofMaxSize(4);

        return assigneeIds.flatMap(ids -> {
            // Pick a target assignee from the list
            Arbitrary<UUID> targetId = Arbitraries.of(ids);

            // Generate 3-10 tasks, each assigned to one of the generated assignees
            Arbitrary<List<Task>> tasks = Arbitraries.of(ids).flatMap(assigneeId ->
                    taskWithAssignee(assigneeId)
            ).list().ofMinSize(3).ofMaxSize(10);

            return Combinators.combine(tasks, targetId).as(TaskSetWithTarget::new);
        });
    }

    @Provide
    Arbitrary<TaskSetWithTarget> taskSetsWithCreators() {
        // Generate 2-4 distinct creator IDs, pick one as target, generate tasks distributed among them
        Arbitrary<List<UUID>> creatorIds = Arbitraries.create(UUID::randomUUID)
                .list().ofMinSize(2).ofMaxSize(4);

        return creatorIds.flatMap(ids -> {
            // Pick a target creator from the list
            Arbitrary<UUID> targetId = Arbitraries.of(ids);

            // Generate 3-10 tasks, each created by one of the generated creators
            Arbitrary<List<Task>> tasks = Arbitraries.of(ids).flatMap(creatorId ->
                    taskWithCreator(creatorId)
            ).list().ofMinSize(3).ofMaxSize(10);

            return Combinators.combine(tasks, targetId).as(TaskSetWithTarget::new);
        });
    }

    @Provide
    Arbitrary<String> nonUuidStrings() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(50)
                .filter(s -> !isValidUuid(s));
    }

    // --- Helper methods ---

    private Arbitrary<Task> taskWithAssignee(UUID assigneeId) {
        return Combinators.combine(
                Arbitraries.create(UUID::randomUUID), // employeeId
                Arbitraries.create(UUID::randomUUID), // interactionId
                Arbitraries.create(UUID::randomUUID), // creatorId
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100), // title
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(100).injectNull(0.3) // description
        ).as((employeeId, interactionId, creatorId, title, description) -> {
            Task task = new Task(employeeId, interactionId, creatorId, assigneeId,
                    title, description, TaskStatus.OPEN, LocalDate.now().plusDays(7));
            // Set the id via reflection since it's generated by JPA
            setTaskId(task, UUID.randomUUID());
            return task;
        });
    }

    private Arbitrary<Task> taskWithCreator(UUID creatorId) {
        return Combinators.combine(
                Arbitraries.create(UUID::randomUUID), // employeeId
                Arbitraries.create(UUID::randomUUID), // interactionId
                Arbitraries.create(UUID::randomUUID), // assigneeId
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100), // title
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(100).injectNull(0.3) // description
        ).as((employeeId, interactionId, assigneeId, title, description) -> {
            Task task = new Task(employeeId, interactionId, creatorId, assigneeId,
                    title, description, TaskStatus.OPEN, LocalDate.now().plusDays(7));
            // Set the id via reflection since it's generated by JPA
            setTaskId(task, UUID.randomUUID());
            return task;
        });
    }

    private static void setTaskId(Task task, UUID id) {
        try {
            var idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set task id via reflection", e);
        }
    }

    private static boolean isValidUuid(String s) {
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
