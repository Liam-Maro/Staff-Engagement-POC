package com.staffengagement.task.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID individualId,
        UUID interactionId,
        UUID creatorId,
        UUID assigneeId,
        String description,
        String status,
        LocalDate dueDate,
        LocalDateTime createdAt
) {}
