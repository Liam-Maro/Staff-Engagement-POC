package com.staffengagement.employee.service;

import com.staffengagement.employee.dto.CreateEmployeeRequest;
import com.staffengagement.employee.dto.EmployeeResponse;
import com.staffengagement.employee.dto.UpdateEmployeeRequest;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {
    List<EmployeeResponse> findAll();
    EmployeeResponse findById(UUID id);
    boolean existsById(UUID id);
    EmployeeResponse create(CreateEmployeeRequest request);
    EmployeeResponse update(UUID id, UpdateEmployeeRequest request);
    void delete(UUID id);
}
