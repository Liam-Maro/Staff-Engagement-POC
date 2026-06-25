package com.staffengagement.portfolio.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record LinkResponse(
        UUID id,
        UUID employeeId,
        String url,
        String label,
        LocalDateTime createdAt
) {}
