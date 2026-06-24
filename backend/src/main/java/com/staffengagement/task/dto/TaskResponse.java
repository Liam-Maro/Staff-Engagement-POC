package com.staffengagement.task.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID employeeId,
        UUID interactionId,
        String title,
        String description,
        String status,
        LocalDate dueDate,
        LocalDateTime createdAt
) {}
