package com.staffengagement.employee.service;

import com.staffengagement.employee.dto.CreateEmployeeRequest;
import com.staffengagement.employee.dto.EmployeeResponse;
import com.staffengagement.employee.dto.UpdateEmployeeRequest;
import com.staffengagement.employee.model.Employee;
import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.shared.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository repository;

    EmployeeServiceImpl(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<EmployeeResponse> findAll() {
        return repository.findAll().stream().map(EmployeeResponse::from).toList();
    }

    @Override
    public EmployeeResponse findById(UUID id) {
        return EmployeeResponse.from(findOrThrow(id));
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public EmployeeResponse create(CreateEmployeeRequest request) {
        var employee = new Employee();
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setDepartment(request.department());
        employee.setJobTitle(request.jobTitle());
        employee.setHireDate(request.hireDate());
        return EmployeeResponse.from(repository.save(employee));
    }

    @Override
    public EmployeeResponse update(UUID id, UpdateEmployeeRequest request) {
        var employee = findOrThrow(id);
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setDepartment(request.department());
        employee.setJobTitle(request.jobTitle());
        employee.setHireDate(request.hireDate());
        return EmployeeResponse.from(repository.save(employee));
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Employee not found with id: " + id);
        }
        repository.deleteById(id);
    }

    private Employee findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));
    }
}
