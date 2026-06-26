package com.staffengagement.task.repository;

import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dynamic JPA Specification builders for filtering Task queries.
 * Each static method returns a Specification that can be composed with AND logic.
 */
public final class TaskSpecifications {

    private TaskSpecifications() {
        // utility class
    }

    /**
     * Filter tasks by assignee ID.
     */
    public static Specification<Task> hasAssignee(UUID assigneeId) {
        return (root, query, cb) -> cb.equal(root.get("assigneeId"), assigneeId);
    }

    /**
     * Filter tasks by creator ID.
     */
    public static Specification<Task> hasCreator(UUID creatorId) {
        return (root, query, cb) -> cb.equal(root.get("creatorId"), creatorId);
    }

    /**
     * Exclude tasks where assignee equals creator (self-assigned tasks).
     */
    public static Specification<Task> excludeSelfAssigned() {
        return (root, query, cb) -> cb.notEqual(root.get("assigneeId"), root.get("creatorId"));
    }

    /**
     * Filter tasks by status.
     */
    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Filter tasks with due date >= from (inclusive).
     */
    public static Specification<Task> dueDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueDate"), from);
    }

    /**
     * Filter tasks with due date <= to (inclusive).
     */
    public static Specification<Task> dueDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueDate"), to);
    }

    /**
     * Filter tasks with created_at >= start of the given day (inclusive).
     */
    public static Specification<Task> createdFrom(LocalDate from) {
        LocalDateTime startOfDay = from.atStartOfDay();
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay);
    }

    /**
     * Filter tasks with created_at <= end of the given day (inclusive).
     */
    public static Specification<Task> createdTo(LocalDate to) {
        LocalDateTime endOfDay = to.atTime(23, 59, 59, 999_999_999);
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endOfDay);
    }
}
