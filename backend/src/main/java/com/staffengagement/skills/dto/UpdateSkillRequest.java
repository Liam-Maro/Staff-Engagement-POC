package com.staffengagement.skills.dto;

import com.staffengagement.skills.validation.ValidProficiency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSkillRequest(
        @NotBlank @Size(max = 100, message = "Skill name must not exceed 100 characters") String name,
        @Min(0) @Max(value = 50, message = "Years of experience must be between 0 and 50") int yearsExperience,
        @Min(0) @Max(value = 500, message = "Project count must be between 0 and 500") int projectCount,
        @ValidProficiency String proficiency
) {}
