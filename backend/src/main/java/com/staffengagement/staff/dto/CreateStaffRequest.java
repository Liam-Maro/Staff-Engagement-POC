package com.staffengagement.staff.dto;

import com.staffengagement.staff.model.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStaffRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull StaffRole role
) {}
