package com.staffengagement.portfolio.github;

import java.util.UUID;

/**
 * Thrown when an employee exists but is archived/deactivated and cannot
 * receive skill imports. Maps to HTTP 409.
 */
public class EmployeeNotActiveException extends RuntimeException {

    private final UUID employeeId;

    public EmployeeNotActiveException(UUID employeeId) {
        super("Employee cannot receive skill imports in current state");
        this.employeeId = employeeId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }
}
