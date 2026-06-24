package com.staffengagement.staff.controller;

import com.staffengagement.staff.dto.CreateStaffRequest;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.dto.UpdateStaffRequest;
import com.staffengagement.staff.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff")
class StaffController {

    private final StaffService service;

    StaffController(StaffService service) {
        this.service = service;
    }

    // Any authenticated staff member can view the staff list
    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    List<StaffResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    StaffResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    // Only admins can create, update, deactivate or delete staff members
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    StaffResponse create(@Valid @RequestBody CreateStaffRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    StaffResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateStaffRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void deactivate(@PathVariable UUID id) {
        service.deactivate(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
