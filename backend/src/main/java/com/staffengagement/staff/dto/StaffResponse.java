package com.staffengagement.staff.dto;

import com.staffengagement.staff.model.StaffRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record StaffResponse(
        UUID id,
        String email,
        StaffRole role,
        boolean active,
        LocalDateTime createdAt
) {}
