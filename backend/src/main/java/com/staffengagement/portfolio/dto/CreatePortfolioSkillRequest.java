package com.staffengagement.portfolio.dto;

import com.staffengagement.portfolio.validation.ValidProficiency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePortfolioSkillRequest(
        @NotBlank @Size(max = 100) String name,
        @ValidProficiency String proficiency
) {}
