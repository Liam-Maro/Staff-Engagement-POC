package com.staffengagement.employee.dto;

import com.staffengagement.employee.model.Employee;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String department,
        String jobTitle,
        LocalDate hireDate,
        boolean active
) {
    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(e.getId(), e.getFirstName(), e.getLastName(),
                e.getEmail(), e.getDepartment(), e.getJobTitle(), e.getHireDate(), e.isActive());
    }
}
