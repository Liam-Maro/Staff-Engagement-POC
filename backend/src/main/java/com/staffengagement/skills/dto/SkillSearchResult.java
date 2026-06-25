package com.staffengagement.skills.dto;

public record SkillSearchResult(
        String employeeFirstName,
        String employeeLastName,
        String employeeEmail,
        String skillName,
        int yearsExperience,
        int projectCount,
        String proficiency
) {}
