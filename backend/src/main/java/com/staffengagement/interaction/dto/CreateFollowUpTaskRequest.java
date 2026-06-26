package com.staffengagement.interaction.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateFollowUpTaskRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description,
        @FutureOrPresent LocalDate dueDate
) {}
