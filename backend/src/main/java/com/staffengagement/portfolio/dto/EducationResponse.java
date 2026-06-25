package com.staffengagement.portfolio.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EducationResponse(
        UUID id,
        UUID employeeId,
        String institution,
        String degree,
        String fieldOfStudy,
        LocalDate graduationDate,
        LocalDateTime createdAt
) {}
