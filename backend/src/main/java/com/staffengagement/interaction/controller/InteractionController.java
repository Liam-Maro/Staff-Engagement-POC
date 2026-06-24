package com.staffengagement.interaction.controller;

import com.staffengagement.interaction.dto.CreateInteractionRequest;
import com.staffengagement.interaction.dto.InteractionResponse;
import com.staffengagement.interaction.service.InteractionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interactions")
class InteractionController {

    private final InteractionService service;

    InteractionController(InteractionService service) {
        this.service = service;
    }

    @GetMapping
    List<InteractionResponse> findAll(@RequestParam(required = false) UUID employeeId) {
        if (employeeId != null) {
            return service.findByEmployeeId(employeeId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    InteractionResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    InteractionResponse create(@Valid @RequestBody CreateInteractionRequest request) {
        return service.create(request);
    }
}
