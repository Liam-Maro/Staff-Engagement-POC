package com.staffengagement.employee.service;

import com.staffengagement.employee.dto.CreateEmployeeRequest;
import com.staffengagement.employee.dto.EmployeeResponse;
import com.staffengagement.employee.model.Employee;
import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.shared.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
}
