package com.staffengagement.portfolio.dto;

import java.util.List;

public record FullPortfolioResponse(
        List<PortfolioSkillResponse> skills,
        List<EducationResponse> education,
        List<ProjectResponse> projects,
        List<LinkResponse> links
) {}
