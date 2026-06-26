package com.staffengagement.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateTaskRequest(
        @NotNull(message = "Individual ID is required")
        UUID individualId,

        UUID interactionId,

        @NotNull(message = "Assignee ID is required")
        UUID assigneeId,

        @NotBlank(message = "Description must not be blank")
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        LocalDate dueDate
) {}
