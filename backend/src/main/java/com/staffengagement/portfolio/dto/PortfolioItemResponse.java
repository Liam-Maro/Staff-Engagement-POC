package com.staffengagement.portfolio.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PortfolioItemResponse(
        UUID id,
        UUID employeeId,
        String type,
        String title,
        String description,
        String url,
        LocalDate dateObtained,
        LocalDateTime createdAt
) {}
