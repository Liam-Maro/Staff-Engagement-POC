package com.staffengagement.staff.controller;

import com.staffengagement.staff.dto.CreateStaffRequest;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @GetMapping
    List<StaffResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    StaffResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StaffResponse create(@Valid @RequestBody CreateStaffRequest request) {
        return service.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
