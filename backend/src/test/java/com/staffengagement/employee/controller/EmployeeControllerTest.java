package com.staffengagement.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffengagement.config.TestSecurityConfig;
import com.staffengagement.auth.service.JwtService;
import com.staffengagement.employee.dto.CreateEmployeeRequest;
import com.staffengagement.employee.dto.EmployeeResponse;
import com.staffengagement.employee.dto.UpdateEmployeeRequest;
import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.staff.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
@Import(TestSecurityConfig.class)
class EmployeeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private EmployeeService service;
    @MockitoBean private StaffRepository staffRepository;
    @MockitoBean private JwtService jwtService;

    @Test
    void findAll_shouldReturn200WithList() throws Exception {
        var emp = new EmployeeResponse(UUID.randomUUID(), "John", "Doe", "john@test.com",
                "Engineering", "Dev", LocalDate.of(2020, 1, 1), true);
        when(service.findAll()).thenReturn(List.of(emp));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("John"));
    }

    @Test
    void findById_shouldReturn200_whenExists() throws Exception {
        UUID id = UUID.randomUUID();
        var emp = new EmployeeResponse(id, "Jane", "Smith", "jane@test.com",
                "HR", "Manager", LocalDate.of(2019, 3, 15), true);
        when(service.findById(id)).thenReturn(emp);

        mockMvc.perform(get("/api/employees/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        var request = new CreateEmployeeRequest("Bob", "Jones", "bob@test.com", "IT", "Admin", LocalDate.now());
        var response = new EmployeeResponse(UUID.randomUUID(), "Bob", "Jones", "bob@test.com",
                "IT", "Admin", LocalDate.now(), true);
        when(service.create(any(CreateEmployeeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Bob"));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new UpdateEmployeeRequest("Updated", "Name", "up@test.com", "Sales", "Rep", LocalDate.now());
        var response = new EmployeeResponse(id, "Updated", "Name", "up@test.com",
                "Sales", "Rep", LocalDate.now(), true);
        when(service.update(eq(id), any(UpdateEmployeeRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/employees/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(service).delete(id);

        mockMvc.perform(delete("/api/employees/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenThrow(new EntityNotFoundException("Employee not found with id: " + id));

        mockMvc.perform(get("/api/employees/" + id))
                .andExpect(status().isNotFound());
    }
}
