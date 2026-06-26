package com.staffengagement.task.service;

import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.interaction.service.InteractionService;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.TaskQueryParams;
import com.staffengagement.task.dto.TaskQueryResult;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TaskServiceImpl.findTasks() edge cases.
 *
 * Validates: Requirements 7.4, 7.5, 7.8
 */
@ExtendWith(MockitoExtension.class)
class TaskQueryEdgeCaseTest {

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

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Captor
    private ArgumentCaptor<Specification<Task>> specCaptor;

    // --- Requirement 7.8: Partial date ranges ---

    @Test
    void findTasks_withOnlyDueDateFrom_shouldBuildSpecWithoutError() {
        // Arrange: only dueDateFrom set, dueDateTo is null (open upper bound)
        LocalDate from = LocalDate.of(2024, 6, 1);
        var params = new TaskQueryParams(
                null, null, null, null,
                from, null,  // dueDateFrom set, dueDateTo null
                null, null,
                null, null, 0, 50
        );
        Page<Task> emptyPage = new PageImpl<>(Collections.emptyList());
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        TaskQueryResult result = taskService.findTasks(params);

        // Assert: no error, valid result returned
        assertThat(result).isNotNull();
        assertThat(result.tasks()).isEmpty();
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findTasks_withOnlyDueDateTo_shouldBuildSpecWithoutError() {
        // Arrange: only dueDateTo set, dueDateFrom is null (open lower bound)
        LocalDate to = LocalDate.of(2024, 12, 31);
        var params = new TaskQueryParams(
                null, null, null, null,
                null, to,  // dueDateFrom null, dueDateTo set
                null, null,
                null, null, 0, 50
        );
        Page<Task> emptyPage = new PageImpl<>(Collections.emptyList());
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        TaskQueryResult result = taskService.findTasks(params);

        // Assert: no error, valid result returned
        assertThat(result).isNotNull();
        assertThat(result.tasks()).isEmpty();
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    // --- Requirement 7.8: Empty result returns empty list not null ---

    @Test
    void findTasks_whenRepositoryReturnsEmptyPage_shouldReturnEmptyListNotNull() {
        // Arrange: repository returns a page with no content
        var params = new TaskQueryParams(
                UUID.randomUUID(), null, null, null,
                null, null, null, null,
                null, null, 0, 50
        );
        // Use PageImpl with Pageable to simulate realistic empty page with proper size metadata
        Pageable pageable = PageRequest.of(0, 50);
        Page<Task> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        TaskQueryResult result = taskService.findTasks(params);

        // Assert: tasks is an empty list (not null), totalCount is 0
        assertThat(result.tasks()).isNotNull();
        assertThat(result.tasks()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(0L);
        assertThat(result.currentPage()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(50);
    }

    // --- Requirement 7.4, 7.5: Default sort is createdDate desc ---

    @Test
    void findTasks_withNullSortByAndSortOrder_shouldDefaultToCreatedAtDesc() {
        // Arrange: sortBy=null, sortOrder=null → should default to createdAt desc
        var params = new TaskQueryParams(
                null, null, null, null,
                null, null, null, null,
                null, null,  // sortBy=null, sortOrder=null
                0, 50
        );
        Page<Task> emptyPage = new PageImpl<>(Collections.emptyList());
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        taskService.findTasks(params);

        // Assert: verify Pageable sort is createdAt DESC
        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        Sort sort = captured.getSort();

        Sort.Order primaryOrder = sort.iterator().next();
        assertThat(primaryOrder.getProperty()).isEqualTo("createdAt");
        assertThat(primaryOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    // --- Requirement 7.4: Status enum value applied correctly ---

    @Test
    void findTasks_withStatusEnum_shouldApplySpecificationCorrectly() {
        // The TaskQueryParams uses TaskStatus enum directly; case handling is at controller level.
        // When a status enum value is provided, the spec should be applied without error.
        var params = new TaskQueryParams(
                null, null, null, TaskStatus.IN_PROGRESS,
                null, null, null, null,
                null, null, 0, 50
        );
        Page<Task> emptyPage = new PageImpl<>(Collections.emptyList());
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        TaskQueryResult result = taskService.findTasks(params);

        // Assert: spec built and passed to repository, result returned without error
        assertThat(result).isNotNull();
        verify(repository).findAll(specCaptor.capture(), any(Pageable.class));
        // The specification was composed (non-null) — status filter was applied
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    void findTasks_withEachStatusEnumValue_shouldWorkForAllStatuses() {
        // Verify all three TaskStatus enum values work correctly
        for (TaskStatus status : TaskStatus.values()) {
            var params = new TaskQueryParams(
                    null, null, null, status,
                    null, null, null, null,
                    null, null, 0, 50
            );
            Page<Task> emptyPage = new PageImpl<>(Collections.emptyList());
            when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

            // Act & Assert: no error for any status value
            TaskQueryResult result = taskService.findTasks(params);
            assertThat(result).isNotNull();
            assertThat(result.tasks()).isEmpty();
        }
    }
}
