package com.staffengagement.staff.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record StaffResponse(
        UUID id,
        UUID employeeId,
        String role,
        boolean active,
        LocalDateTime createdAt
) {}
