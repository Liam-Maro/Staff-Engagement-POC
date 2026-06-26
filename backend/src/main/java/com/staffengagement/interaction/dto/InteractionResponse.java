package com.staffengagement.interaction.dto;

import com.staffengagement.interaction.model.InteractionType;

import java.time.LocalDateTime;
import java.util.UUID;

public record InteractionResponse(
        UUID id,
        UUID employeeId,
        UUID staffId,
        InteractionType type,
        String notes,
        LocalDateTime occurredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
