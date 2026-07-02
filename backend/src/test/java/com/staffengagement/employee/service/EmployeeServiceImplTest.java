package com.staffengagement.employee.service;

import com.staffengagement.employee.dto.CreateEmployeeRequest;
import com.staffengagement.employee.dto.EmployeeResponse;
import com.staffengagement.employee.dto.UpdateEmployeeRequest;
import com.staffengagement.employee.model.Employee;
import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.shared.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    void create_shouldReturnEmployeeResponse() {
        var request = new CreateEmployeeRequest("John", "Doe", "john@example.com", "Engineering", "Developer", LocalDate.of(2024, 1, 15));
        var saved = new Employee();
        saved.setId(UUID.randomUUID());
        saved.setFirstName("John");
        saved.setLastName("Doe");
        saved.setEmail("john@example.com");
        saved.setDepartment("Engineering");
        saved.setJobTitle("Developer");
        saved.setHireDate(LocalDate.of(2024, 1, 15));

        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);

        EmployeeResponse result = employeeService.create(request);

        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.email()).isEqualTo("john@example.com");
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.findById(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findById_shouldReturnEmployee_whenExists() {
        UUID id = UUID.randomUUID();
        var employee = new Employee();
        employee.setId(id);
        employee.setFirstName("Jane");
        employee.setLastName("Smith");
        employee.setEmail("jane@example.com");
        employee.setDepartment("HR");
        employee.setJobTitle("Manager");
        employee.setHireDate(LocalDate.of(2023, 3, 10));

        when(employeeRepository.findById(id)).thenReturn(Optional.of(employee));

        EmployeeResponse result = employeeService.findById(id);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.firstName()).isEqualTo("Jane");
        assertThat(result.lastName()).isEqualTo("Smith");
    }

    @Test
    void findAll_shouldReturnAllEmployees() {
        var emp1 = new Employee();
        emp1.setId(UUID.randomUUID());
        emp1.setFirstName("Alice");
        emp1.setLastName("A");
        emp1.setEmail("alice@example.com");
        emp1.setDepartment("Eng");
        emp1.setJobTitle("Dev");
        emp1.setHireDate(LocalDate.of(2022, 1, 1));

        var emp2 = new Employee();
        emp2.setId(UUID.randomUUID());
        emp2.setFirstName("Bob");
        emp2.setLastName("B");
        emp2.setEmail("bob@example.com");
        emp2.setDepartment("Eng");
        emp2.setJobTitle("QA");
        emp2.setHireDate(LocalDate.of(2022, 6, 1));

        when(employeeRepository.findAll()).thenReturn(List.of(emp1, emp2));

        List<EmployeeResponse> result = employeeService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).firstName()).isEqualTo("Alice");
        assertThat(result.get(1).firstName()).isEqualTo("Bob");
    }

    @Test
    void existsById_shouldReturnTrue_whenExists() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.existsById(id)).thenReturn(true);

        assertThat(employeeService.existsById(id)).isTrue();
    }

    @Test
    void existsById_shouldReturnFalse_whenNotExists() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.existsById(id)).thenReturn(false);

        assertThat(employeeService.existsById(id)).isFalse();
    }

    @Test
    void update_shouldUpdateAndReturnEmployee() {
        UUID id = UUID.randomUUID();
        var existing = new Employee();
        existing.setId(id);
        existing.setFirstName("Old");
        existing.setLastName("Name");
        existing.setEmail("old@example.com");
        existing.setDepartment("OldDept");
        existing.setJobTitle("OldTitle");
        existing.setHireDate(LocalDate.of(2020, 1, 1));

        var request = new UpdateEmployeeRequest("New", "Name", "new@example.com", "NewDept", "NewTitle", LocalDate.of(2024, 6, 1));

        when(employeeRepository.findById(id)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse result = employeeService.update(id, request);

        assertThat(result.firstName()).isEqualTo("New");
        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.department()).isEqualTo("NewDept");
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();
        var request = new UpdateEmployeeRequest("New", "Name", "new@example.com", "Dept", "Title", LocalDate.now());
        when(employeeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.update(id, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_shouldDeleteSuccessfully_whenExists() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.existsById(id)).thenReturn(true);

        employeeService.delete(id);

        verify(employeeRepository).deleteById(id);
    }

    @Test
    void delete_shouldThrow_whenNotExists() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> employeeService.delete(id))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
