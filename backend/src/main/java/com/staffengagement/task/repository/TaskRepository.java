package com.staffengagement.task.repository;

import com.staffengagement.task.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByEmployeeId(UUID employeeId);
    List<Task> findByInteractionIdIn(List<UUID> interactionIds);
}
