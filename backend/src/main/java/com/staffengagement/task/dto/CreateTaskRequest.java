package com.staffengagement.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskRequest(
        @NotNull UUID employeeId,
        UUID interactionId,
        @NotBlank String title,
        String description,
        LocalDate dueDate
) {}
