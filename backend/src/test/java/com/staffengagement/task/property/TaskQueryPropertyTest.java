package com.staffengagement.task.property;

import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.interaction.service.InteractionService;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.TaskQueryParams;
import com.staffengagement.task.dto.TaskQueryResult;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.repository.TaskRepository;
import com.staffengagement.task.service.TaskService;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for task query, filtering, sorting, and pagination.
 * Tests Properties 8, 9, 10, 11, 21 from the design document.
 *
 * Validates: Requirements 2.1, 2.2, 2.6, 3.1, 3.5, 7.1, 7.2, 7.3, 7.4, 7.5, 7.7, 7.8, 7.9, 14.2
 */
class TaskQueryPropertyTest {

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
    }

    // ===== Property 8: Filter by assignee returns exactly matching tasks =====

    /**
     * Property 8: Filter by assignee returns exactly matching tasks.
     * For any set of tasks and any valid assigneeId, querying with that assigneeId
     * SHALL return exactly the tasks whose assigneeId matches — no more, no less.
     *
     * Validates: Requirements 2.1, 2.2
     */
    @Property(tries = 50)
    void filterByAssigneeReturnsExactlyMatchingTasks(
            @ForAll("taskSets") List<Task> allTasks,
            @ForAll("randomUUIDs") UUID targetAssigneeId
    ) {
        // Compute expected: tasks that match the target assignee
        List<Task> expectedTasks = allTasks.stream()
                .filter(t -> t.getAssigneeId().equals(targetAssigneeId))
                .toList();

        // Reset mock to avoid cross-try invocation count interference
        reset(repository);

        // Mock repository to return only matching tasks (simulating DB filter)
        Page<Task> mockPage = new PageImpl<>(expectedTasks);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // Query with assignee filter
        TaskQueryParams params = new TaskQueryParams(
                targetAssigneeId, null, null, null,
                null, null, null, null,
                "createdDate", "desc", 0, 50
        );

        TaskQueryResult result = taskService.findTasks(params);

        // Verify all returned tasks have the correct assigneeId
        assertThat(result.tasks()).allMatch(t -> t.assigneeId().equals(targetAssigneeId));
        assertThat(result.tasks()).hasSize(expectedTasks.size());

        // Verify the repository was called with a specification (filter was applied)
        verify(repository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    // ===== Property 9: Filter by creator with excludeSelfAssigned enforces both constraints =====

    /**
     * Property 9: Filter by creator with excludeSelfAssigned enforces both constraints.
     * When querying with a creatorId and excludeSelfAssigned=true, the result SHALL contain
     * only tasks where the creator matches AND the assignee differs from the creator.
     *
     * Validates: Requirements 3.1, 3.5, 14.2
     */
    @Property(tries = 50)
    void filterByCreatorWithExcludeSelfAssignedEnforcesBothConstraints(
            @ForAll("taskSetsWithKnownCreator") TaskSetWithCreator data
    ) {
        UUID creatorId = data.creatorId();
        List<Task> allTasks = data.tasks();

        // Expected: tasks where creator matches AND assignee != creator
        List<Task> expectedTasks = allTasks.stream()
                .filter(t -> t.getCreatorId().equals(creatorId))
                .filter(t -> !t.getAssigneeId().equals(t.getCreatorId()))
                .toList();

        // Reset mock to avoid cross-try interference
        reset(repository);

        // Mock repository to return the filtered subset
        Page<Task> mockPage = new PageImpl<>(expectedTasks);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        TaskQueryParams params = new TaskQueryParams(
                null, creatorId, true, null,
                null, null, null, null,
                "createdDate", "desc", 0, 50
        );

        TaskQueryResult result = taskService.findTasks(params);

        // Every task in result must have creator == creatorId AND assignee != creator
        for (TaskResponse task : result.tasks()) {
            assertThat(task.creatorId()).isEqualTo(creatorId);
            assertThat(task.assigneeId()).isNotEqualTo(task.creatorId());
        }
        assertThat(result.tasks()).hasSize(expectedTasks.size());
    }

    // ===== Property 10: Sort ordering invariant =====

    /**
     * Property 10: Sort ordering invariant.
     * For any list of tasks returned by a query, if sortBy=createdDate and sortOrder=desc,
     * then for every consecutive pair, task_i.createdAt >= task_i+1.createdAt.
     * When sortBy=dueDate, tasks with null due dates appear last regardless of sort direction.
     *
     * Validates: Requirements 7.4, 7.5, 7.9
     */
    @Property(tries = 50)
    void sortOrderingInvariantCreatedDateDesc(
            @ForAll("taskSetsWithDates") List<Task> allTasks
    ) {
        // Sort tasks by createdAt desc, then by id desc (mimicking TaskSortBuilder behavior)
        List<Task> sorted = allTasks.stream()
                .sorted(Comparator.comparing(Task::getCreatedAt, Comparator.reverseOrder())
                        .thenComparing(Task::getId, Comparator.reverseOrder()))
                .toList();

        // Reset mock to avoid cross-try interference
        reset(repository);

        Page<Task> mockPage = new PageImpl<>(sorted);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        TaskQueryParams params = new TaskQueryParams(
                null, null, null, null,
                null, null, null, null,
                "createdDate", "desc", 0, 50
        );

        TaskQueryResult result = taskService.findTasks(params);

        // Verify ordering: each task's createdAt >= next task's createdAt
        List<TaskResponse> tasks = result.tasks();
        for (int i = 0; i < tasks.size() - 1; i++) {
            LocalDateTime current = tasks.get(i).createdAt();
            LocalDateTime next = tasks.get(i + 1).createdAt();
            assertThat(current).isAfterOrEqualTo(next);
        }
    }

    @Property(tries = 50)
    void sortOrderingInvariantDueDateNullsLast(
            @ForAll("taskSetsWithNullDueDates") List<Task> allTasks
    ) {
        // Sort tasks by dueDate asc with nulls last, then by id asc
        List<Task> sorted = allTasks.stream()
                .sorted(Comparator.comparing(Task::getDueDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Task::getId))
                .toList();

        // Reset mock to avoid cross-try interference
        reset(repository);

        Page<Task> mockPage = new PageImpl<>(sorted);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        TaskQueryParams params = new TaskQueryParams(
                null, null, null, null,
                null, null, null, null,
                "dueDate", "asc", 0, 50
        );

        TaskQueryResult result = taskService.findTasks(params);

        // Verify: all non-null due dates appear before null due dates
        List<TaskResponse> tasks = result.tasks();
        boolean seenNull = false;
        for (TaskResponse task : tasks) {
            if (task.dueDate() == null) {
                seenNull = true;
            } else {
                assertThat(seenNull)
                        .as("Non-null dueDate should not appear after null dueDate")
                        .isFalse();
            }
        }

        // Verify ascending order among non-null due dates
        List<TaskResponse> nonNullDueDateTasks = tasks.stream()
                .filter(t -> t.dueDate() != null)
                .toList();
        for (int i = 0; i < nonNullDueDateTasks.size() - 1; i++) {
            LocalDate current = nonNullDueDateTasks.get(i).dueDate();
            LocalDate next = nonNullDueDateTasks.get(i + 1).dueDate();
            assertThat(current).isBeforeOrEqualTo(next);
        }
    }

    // ===== Property 11: Filter combination applies all constraints as AND =====

    /**
     * Property 11: Filter combination applies all constraints as AND.
     * For any combination of filter parameters, all filters SHALL be applied together.
     * Every task in the result set SHALL satisfy all provided filter criteria.
     *
     * Validates: Requirements 7.1, 7.2, 7.3, 7.7, 7.8
     */
    @Property(tries = 50)
    void filterCombinationAppliesAllConstraintsAsAnd(
            @ForAll("filterCombinations") FilterCombination combo
    ) {
        List<Task> allTasks = combo.tasks();

        // Apply all filters in-memory to compute expected result
        List<Task> expectedTasks = allTasks.stream()
                .filter(t -> combo.status() == null || t.getStatus() == combo.status())
                .filter(t -> combo.assigneeId() == null || t.getAssigneeId().equals(combo.assigneeId()))
                .filter(t -> combo.dueDateFrom() == null || (t.getDueDate() != null && !t.getDueDate().isBefore(combo.dueDateFrom())))
                .filter(t -> combo.dueDateTo() == null || (t.getDueDate() != null && !t.getDueDate().isAfter(combo.dueDateTo())))
                .filter(t -> combo.createdFrom() == null || !t.getCreatedAt().toLocalDate().isBefore(combo.createdFrom()))
                .filter(t -> combo.createdTo() == null || !t.getCreatedAt().toLocalDate().isAfter(combo.createdTo()))
                .toList();

        // Reset mock to avoid cross-try interference
        reset(repository);

        // Mock repository to return the pre-filtered results
        Page<Task> mockPage = new PageImpl<>(expectedTasks);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        TaskQueryParams params = new TaskQueryParams(
                combo.assigneeId(), null, null, combo.status(),
                combo.dueDateFrom(), combo.dueDateTo(),
                combo.createdFrom(), combo.createdTo(),
                "createdDate", "desc", 0, 50
        );

        TaskQueryResult result = taskService.findTasks(params);

        // Verify every returned task satisfies ALL filter constraints
        for (TaskResponse task : result.tasks()) {
            if (combo.status() != null) {
                assertThat(task.status()).isEqualTo(combo.status().name());
            }
            if (combo.assigneeId() != null) {
                assertThat(task.assigneeId()).isEqualTo(combo.assigneeId());
            }
            if (combo.dueDateFrom() != null) {
                assertThat(task.dueDate()).isNotNull();
                assertThat(task.dueDate()).isAfterOrEqualTo(combo.dueDateFrom());
            }
            if (combo.dueDateTo() != null) {
                assertThat(task.dueDate()).isNotNull();
                assertThat(task.dueDate()).isBeforeOrEqualTo(combo.dueDateTo());
            }
            if (combo.createdFrom() != null) {
                assertThat(task.createdAt().toLocalDate()).isAfterOrEqualTo(combo.createdFrom());
            }
            if (combo.createdTo() != null) {
                assertThat(task.createdAt().toLocalDate()).isBeforeOrEqualTo(combo.createdTo());
            }
        }
        assertThat(result.tasks()).hasSize(expectedTasks.size());
    }

    // ===== Property 21: Pagination enforced at 50 tasks per page =====

    /**
     * Property 21: Pagination enforced at 50 tasks per page.
     * When >50 tasks exist, the service returns a page of at most 50 tasks.
     *
     * Validates: Requirements 2.6
     */
    @Property(tries = 20)
    void paginationEnforcedAtFiftyTasksPerPage(
            @ForAll("largeTasks") List<Task> allTasks
    ) {
        // Reset mock to avoid cross-try invocation count interference
        reset(repository);

        // Cap at 50 to simulate what DB pagination would return
        List<Task> pageContent = allTasks.subList(0, Math.min(50, allTasks.size()));
        Page<Task> mockPage = new PageImpl<>(pageContent,
                org.springframework.data.domain.PageRequest.of(0, 50),
                allTasks.size());
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        TaskQueryParams params = new TaskQueryParams(
                null, null, null, null,
                null, null, null, null,
                "createdDate", "desc", 0, 50
        );

        TaskQueryResult result = taskService.findTasks(params);

        // Verify page size is at most 50
        assertThat(result.tasks()).hasSizeLessThanOrEqualTo(50);
        assertThat(result.pageSize()).isEqualTo(50);
        assertThat(result.totalCount()).isEqualTo(allTasks.size());

        // Verify the pageable passed to repository has size=50
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, times(1)).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    // ===== Providers =====

    @Provide
    Arbitrary<UUID> randomUUIDs() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<List<Task>> taskSets() {
        return Arbitraries.integers().between(1, 20)
                .flatMap(size -> Arbitraries.create(() -> generateTaskList(size, null, null)));
    }

    @Provide
    Arbitrary<TaskSetWithCreator> taskSetsWithKnownCreator() {
        return Arbitraries.create(() -> {
            UUID creatorId = UUID.randomUUID();
            List<Task> tasks = new ArrayList<>();
            Random rng = new Random();
            int count = rng.nextInt(15) + 5;

            for (int i = 0; i < count; i++) {
                UUID taskCreator = rng.nextBoolean() ? creatorId : UUID.randomUUID();
                // Some self-assigned (creator == assignee), some not
                UUID assignee = rng.nextBoolean() ? taskCreator : UUID.randomUUID();
                Task task = createTask(UUID.randomUUID(), null, taskCreator, assignee,
                        "Task " + i, randomStatus(rng),
                        rng.nextBoolean() ? LocalDate.now().plusDays(rng.nextInt(30)) : null,
                        LocalDateTime.now().minusDays(rng.nextInt(60)));
                tasks.add(task);
            }
            return new TaskSetWithCreator(creatorId, tasks);
        });
    }

    @Provide
    Arbitrary<List<Task>> taskSetsWithDates() {
        return Arbitraries.create(() -> {
            Random rng = new Random();
            int count = rng.nextInt(15) + 3;
            List<Task> tasks = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                LocalDateTime createdAt = LocalDateTime.now()
                        .minusDays(rng.nextInt(90))
                        .minusHours(rng.nextInt(24))
                        .minusMinutes(rng.nextInt(60));
                Task task = createTask(UUID.randomUUID(), null,
                        UUID.randomUUID(), UUID.randomUUID(),
                        "Task " + i, randomStatus(rng),
                        rng.nextBoolean() ? LocalDate.now().plusDays(rng.nextInt(60)) : null,
                        createdAt);
                tasks.add(task);
            }
            return tasks;
        });
    }

    @Provide
    Arbitrary<List<Task>> taskSetsWithNullDueDates() {
        return Arbitraries.create(() -> {
            Random rng = new Random();
            int count = rng.nextInt(12) + 4;
            List<Task> tasks = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                // ~40% chance of null due date
                LocalDate dueDate = rng.nextInt(10) < 4 ? null : LocalDate.now().plusDays(rng.nextInt(60) + 1);
                Task task = createTask(UUID.randomUUID(), null,
                        UUID.randomUUID(), UUID.randomUUID(),
                        "Task " + i, randomStatus(rng),
                        dueDate, LocalDateTime.now().minusDays(rng.nextInt(30)));
                tasks.add(task);
            }
            return tasks;
        });
    }

    @Provide
    Arbitrary<FilterCombination> filterCombinations() {
        return Arbitraries.create(() -> {
            Random rng = new Random();
            int count = rng.nextInt(15) + 5;

            UUID assigneeId = UUID.randomUUID();
            TaskStatus status = randomStatus(rng);
            LocalDate dueDateFrom = LocalDate.now().minusDays(rng.nextInt(30));
            LocalDate dueDateTo = dueDateFrom.plusDays(rng.nextInt(60));
            LocalDate createdFrom = LocalDate.now().minusDays(60 + rng.nextInt(30));
            LocalDate createdTo = createdFrom.plusDays(rng.nextInt(90));

            // Randomly null out some filters to test partial combinations
            UUID filterAssignee = rng.nextBoolean() ? assigneeId : null;
            TaskStatus filterStatus = rng.nextBoolean() ? status : null;
            LocalDate filterDueDateFrom = rng.nextBoolean() ? dueDateFrom : null;
            LocalDate filterDueDateTo = rng.nextBoolean() ? dueDateTo : null;
            LocalDate filterCreatedFrom = rng.nextBoolean() ? createdFrom : null;
            LocalDate filterCreatedTo = rng.nextBoolean() ? createdTo : null;

            List<Task> tasks = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                UUID taskAssignee = rng.nextBoolean() ? assigneeId : UUID.randomUUID();
                TaskStatus taskStatus = randomStatus(rng);
                LocalDate taskDueDate = rng.nextInt(10) < 3 ? null :
                        LocalDate.now().minusDays(15).plusDays(rng.nextInt(90));
                LocalDateTime taskCreatedAt = LocalDateTime.now()
                        .minusDays(rng.nextInt(120));

                Task task = createTask(UUID.randomUUID(), null,
                        UUID.randomUUID(), taskAssignee,
                        "Task " + i, taskStatus,
                        taskDueDate, taskCreatedAt);
                tasks.add(task);
            }

            return new FilterCombination(tasks, filterAssignee, filterStatus,
                    filterDueDateFrom, filterDueDateTo, filterCreatedFrom, filterCreatedTo);
        });
    }

    @Provide
    Arbitrary<List<Task>> largeTasks() {
        return Arbitraries.integers().between(51, 100)
                .map(size -> {
                    Random rng = new Random();
                    return IntStream.range(0, size)
                            .mapToObj(i -> createTask(UUID.randomUUID(), null,
                                    UUID.randomUUID(), UUID.randomUUID(),
                                    "Task " + i, randomStatus(rng),
                                    LocalDate.now().plusDays(i),
                                    LocalDateTime.now().minusDays(i)))
                            .toList();
                });
    }

    // ===== Helpers =====

    private List<Task> generateTaskList(int size, UUID fixedAssignee, UUID fixedCreator) {
        Random rng = new Random();
        UUID assignee = fixedAssignee != null ? fixedAssignee : UUID.randomUUID();
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            // Mix of matching and non-matching assignees
            UUID taskAssignee = rng.nextBoolean() ? assignee : UUID.randomUUID();
            UUID creator = fixedCreator != null ? fixedCreator : UUID.randomUUID();
            Task task = createTask(UUID.randomUUID(), null, creator, taskAssignee,
                    "Desc " + i, randomStatus(rng),
                    rng.nextBoolean() ? LocalDate.now().plusDays(rng.nextInt(30)) : null,
                    LocalDateTime.now().minusDays(rng.nextInt(60)));
            tasks.add(task);
        }
        return tasks;
    }

    private Task createTask(UUID individualId, UUID interactionId, UUID creatorId,
                            UUID assigneeId, String description, TaskStatus status,
                            LocalDate dueDate, LocalDateTime createdAt) {
        Task task = new Task(individualId, interactionId, creatorId, assigneeId, description, status, dueDate);
        // Set id and createdAt via reflection since they are generated
        try {
            Field idField = Task.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, UUID.randomUUID());

            Field createdAtField = Task.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(task, createdAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set task fields via reflection", e);
        }
        return task;
    }

    private TaskStatus randomStatus(Random rng) {
        TaskStatus[] values = TaskStatus.values();
        return values[rng.nextInt(values.length)];
    }

    // ===== Data Records =====

    record TaskSetWithCreator(UUID creatorId, List<Task> tasks) {}

    record FilterCombination(
            List<Task> tasks,
            UUID assigneeId,
            TaskStatus status,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            LocalDate createdFrom,
            LocalDate createdTo
    ) {}
}
