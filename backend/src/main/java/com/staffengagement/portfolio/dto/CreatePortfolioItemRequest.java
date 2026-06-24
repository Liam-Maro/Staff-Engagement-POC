package com.staffengagement.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreatePortfolioItemRequest(
        @NotNull UUID employeeId,
        @NotBlank String type,
        @NotBlank String title,
        String description,
        String url,
        LocalDate dateObtained
) {}
