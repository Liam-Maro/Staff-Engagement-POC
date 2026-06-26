package com.staffengagement.task.repository;

import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskSpecificationsTest {

    @Mock
    private Root<Task> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> path;

    @Mock
    private Predicate predicate;

    @BeforeEach
    void setUp() {
        lenient().when(root.get(anyString())).thenReturn(path);
    }

    @Test
    void hasAssignee_createsEqualPredicate() {
        UUID assigneeId = UUID.randomUUID();
        when(cb.equal(any(), eq(assigneeId))).thenReturn(predicate);

        Specification<Task> spec = TaskSpecifications.hasAssignee(assigneeId);
        Predicate result = spec.toPredicate(root, query, cb);

        verify(root).get("assigneeId");
        verify(cb).equal(path, assigneeId);
        assertThat(result).isEqualTo(predicate);
    }

    @Test
    void hasCreator_createsEqualPredicate() {
        UUID creatorId = UUID.randomUUID();
        when(cb.equal(any(), eq(creatorId))).thenReturn(predicate);

        Specification<Task> spec = TaskSpecifications.hasCreator(creatorId);
        Predicate result = spec.toPredicate(root, query, cb);

        verify(root).get("creatorId");
        verify(cb).equal(path, creatorId);
        assertThat(result).isEqualTo(predicate);
    }

    @Test
    void excludeSelfAssigned_createsNotEqualPredicate() {
        Path<Object> assigneePath = mock(Path.class);
        Path<Object> creatorPath = mock(Path.class);
        when(root.get("assigneeId")).thenReturn(assigneePath);
        when(root.get("creatorId")).thenReturn(creatorPath);
        when(cb.notEqual(assigneePath, creatorPath)).thenReturn(predicate);

        Specification<Task> spec = TaskSpecifications.excludeSelfAssigned();
        Predicate result = spec.toPredicate(root, query, cb);

        verify(cb).notEqual(assigneePath, creatorPath);
        assertThat(result).isEqualTo(predicate);
    }

    @Test
    void hasStatus_createsEqualPredicate() {
        when(cb.equal(any(), eq(TaskStatus.TODO))).thenReturn(predicate);

        Specification<Task> spec = TaskSpecifications.hasStatus(TaskStatus.TODO);
        Predicate result = spec.toPredicate(root, query, cb);

        verify(root).get("status");
        verify(cb).equal(path, TaskStatus.TODO);
        assertThat(result).isEqualTo(predicate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void dueDateFrom_createsGreaterThanOrEqualPredicate() {
        LocalDate from = LocalDate.of(2024, 6, 1);
        Path<LocalDate> datePath = mock(Path.class);
        when(root.<LocalDate>get("dueDate")).thenReturn(datePath);
        when(cb.greaterThanOrEqualTo(eq(datePath), eq(from))).thenReturn(predicate);

        Specification<Task> spec = TaskSpecifications.dueDateFrom(from);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isEqualTo(predicate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void dueDateTo_createsLessThanOrEqualPredicate() {
        LocalDate to = LocalDate.of(2024, 12, 31);
        Path<LocalDate> datePath = mock(Path.class);
        when(root.<LocalDate>get("dueDate")).thenReturn(datePath);
        when(cb.lessThanOrEqualTo(eq(datePath), eq(to))).thenReturn(predicate);

        Specification<Task> spec = TaskSpecifications.dueDateTo(to);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isEqualTo(predicate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createdFrom_usesStartOfDay() {
        LocalDate from = LocalDate.of(2024, 3, 15);
        LocalDateTime expectedStart = from.atStartOfDay();
        Path<LocalDateTime> dateTimePath = mock(Path.class);
        when(root.<LocalDateTime>get("createdAt")).thenReturn(dateTimePath);
        when(cb.greaterThanOrEqualTo(eq(dateTimePath), eq(expectedStart))).thenReturn(predicate);

        Specification<Task> spec = TaskSpecifications.createdFrom(from);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isEqualTo(predicate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createdTo_usesEndOfDay() {
        LocalDate to = LocalDate.of(2024, 3, 15);
        LocalDateTime expectedEnd = to.atTime(23, 59, 59, 999_999_999);
        Path<LocalDateTime> dateTimePath = mock(Path.class);
        when(root.<LocalDateTime>get("createdAt")).thenReturn(dateTimePath);
        when(cb.lessThanOrEqualTo(eq(dateTimePath), eq(expectedEnd))).thenReturn(predicate);

        Specification<Task> spec = TaskSpecifications.createdTo(to);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isEqualTo(predicate);
    }

    @Test
    void specificationIsNotNull() {
        assertThat(TaskSpecifications.hasAssignee(UUID.randomUUID())).isNotNull();
        assertThat(TaskSpecifications.hasCreator(UUID.randomUUID())).isNotNull();
        assertThat(TaskSpecifications.excludeSelfAssigned()).isNotNull();
        assertThat(TaskSpecifications.hasStatus(TaskStatus.IN_PROGRESS)).isNotNull();
        assertThat(TaskSpecifications.dueDateFrom(LocalDate.now())).isNotNull();
        assertThat(TaskSpecifications.dueDateTo(LocalDate.now())).isNotNull();
        assertThat(TaskSpecifications.createdFrom(LocalDate.now())).isNotNull();
        assertThat(TaskSpecifications.createdTo(LocalDate.now())).isNotNull();
    }
}
