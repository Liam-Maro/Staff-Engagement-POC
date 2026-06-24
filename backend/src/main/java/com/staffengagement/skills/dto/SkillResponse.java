package com.staffengagement.skills.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SkillResponse(
        UUID id,
        UUID employeeId,
        String name,
        int yearsExperience,
        int projectCount,
        String proficiency,
        LocalDateTime createdAt
) {}
