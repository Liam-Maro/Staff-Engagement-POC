package com.staffengagement.skills.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateSkillRequest(
        @NotBlank String name,
        @Min(0) int yearsExperience,
        @Min(0) int projectCount,
        @NotBlank String proficiency
) {}
