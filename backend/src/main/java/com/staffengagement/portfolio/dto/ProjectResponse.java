package com.staffengagement.portfolio.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID employeeId,
        String projectName,
        String description,
        String role,
        List<String> technologies,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt
) {}
