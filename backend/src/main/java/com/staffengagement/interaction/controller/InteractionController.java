package com.staffengagement.interaction.controller;

import com.staffengagement.interaction.dto.CreateFollowUpTaskRequest;
import com.staffengagement.interaction.dto.CreateInteractionRequest;
import com.staffengagement.interaction.dto.InteractionResponse;
import com.staffengagement.interaction.dto.UpdateInteractionRequest;
import com.staffengagement.interaction.model.InteractionType;
import com.staffengagement.interaction.service.InteractionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/interactions")
class InteractionController {

    private final InteractionService service;

    InteractionController(InteractionService service) {
        this.service = service;
    }

    @GetMapping
    Page<InteractionResponse> findAll(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) InteractionType type,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        return service.findAll(employeeId, type, fromDate, toDate, pageable);
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

    @PutMapping("/{id}")
    InteractionResponse update(@PathVariable UUID id,
                               @Valid @RequestBody UpdateInteractionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @PostMapping("/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    Object createFollowUpTask(@PathVariable UUID id,
                              @Valid @RequestBody CreateFollowUpTaskRequest request) {
        return service.createFollowUpTask(id, request);
    }
}
