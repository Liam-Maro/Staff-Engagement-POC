package com.staffengagement.interaction.dto;

import com.staffengagement.interaction.model.InteractionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateInteractionRequest(
        @NotNull InteractionType type,
        @Size(max = 5000) String notes,
        @NotNull @PastOrPresent LocalDateTime occurredAt
) {}
