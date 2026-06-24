package com.staffengagement.interaction.service;

import com.staffengagement.interaction.dto.CreateInteractionRequest;
import com.staffengagement.interaction.dto.InteractionResponse;
import com.staffengagement.interaction.model.Interaction;
import com.staffengagement.interaction.repository.InteractionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
class InteractionServiceImpl implements InteractionService {

    private final InteractionRepository repository;

    InteractionServiceImpl(InteractionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<InteractionResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public InteractionResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Interaction not found: " + id));
    }

    @Override
    public List<InteractionResponse> findByEmployeeId(UUID employeeId) {
        return repository.findByEmployeeId(employeeId).stream().map(this::toResponse).toList();
    }

    @Override
    public InteractionResponse create(CreateInteractionRequest request) {
        var interaction = new Interaction();
        interaction.setEmployeeId(request.employeeId());
        interaction.setStaffId(request.staffId());
        interaction.setType(request.type());
        interaction.setNotes(request.notes());
        interaction.setOccurredAt(request.occurredAt());
        return toResponse(repository.save(interaction));
    }

    private InteractionResponse toResponse(Interaction i) {
        return new InteractionResponse(i.getId(), i.getEmployeeId(), i.getStaffId(),
                i.getType(), i.getNotes(), i.getOccurredAt(), i.getCreatedAt());
    }
}
