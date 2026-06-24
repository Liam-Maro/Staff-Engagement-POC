package com.staffengagement.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateStaffRequest(
        @NotNull UUID employeeId,
        @NotBlank String role
) {}
