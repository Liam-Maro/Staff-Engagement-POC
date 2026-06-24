package com.staffengagement.skills.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateSkillRequest(
        @NotNull UUID employeeId,
        @NotBlank String name,
        @Min(0) int yearsExperience,
        @Min(0) int projectCount,
        @NotBlank String proficiency
) {}
