package com.staffengagement.staff.dto;

import com.staffengagement.staff.model.StaffRole;
import jakarta.validation.constraints.NotNull;

public record UpdateStaffRequest(
        @NotNull StaffRole role,
        @NotNull Boolean active
) {}
