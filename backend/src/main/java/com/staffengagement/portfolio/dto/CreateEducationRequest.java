package com.staffengagement.portfolio.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateEducationRequest(
        @NotBlank String institution,
        @NotBlank String degree,
        String fieldOfStudy,
        LocalDate graduationDate
) {}
