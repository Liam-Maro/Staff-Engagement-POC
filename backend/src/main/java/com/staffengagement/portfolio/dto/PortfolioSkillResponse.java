package com.staffengagement.portfolio.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PortfolioSkillResponse(
        UUID id,
        UUID employeeId,
        String name,
        int yearsExperience,
        int projectCount,
        String proficiency,
        LocalDateTime createdAt
) {}
