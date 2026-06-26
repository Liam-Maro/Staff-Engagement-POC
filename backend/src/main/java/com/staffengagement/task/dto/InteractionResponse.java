package com.staffengagement.task.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InteractionResponse(
        UUID id,
        UUID employeeId,
        UUID staffId,
        String type,
        String notes,
        LocalDateTime occurredAt,
        LocalDateTime createdAt
) {}
