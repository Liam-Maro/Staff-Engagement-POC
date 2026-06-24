package com.staffengagement.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateInteractionRequest(
        @NotNull UUID employeeId,
        @NotNull UUID staffId,
        @NotBlank String type,
        String notes,
        @NotNull LocalDateTime occurredAt
) {}
