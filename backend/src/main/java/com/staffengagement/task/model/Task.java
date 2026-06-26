package com.staffengagement.task.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tsk_tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "individual_id", nullable = false)
    private UUID individualId;

    @Column(name = "interaction_id")
    private UUID interactionId;

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(name = "assignee_id", nullable = false)
    private UUID assigneeId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Task() {}

    public Task(UUID individualId, UUID interactionId, UUID creatorId, UUID assigneeId,
                String description, TaskStatus status, LocalDate dueDate) {
        this.individualId = individualId;
        this.interactionId = interactionId;
        this.creatorId = creatorId;
        this.assigneeId = assigneeId;
        this.description = description;
        this.status = status;
        this.dueDate = dueDate;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getIndividualId() { return individualId; }
    public UUID getInteractionId() { return interactionId; }
    public UUID getCreatorId() { return creatorId; }
    public UUID getAssigneeId() { return assigneeId; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setIndividualId(UUID individualId) { this.individualId = individualId; }
    public void setInteractionId(UUID interactionId) { this.interactionId = interactionId; }
    public void setAssigneeId(UUID assigneeId) { this.assigneeId = assigneeId; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
