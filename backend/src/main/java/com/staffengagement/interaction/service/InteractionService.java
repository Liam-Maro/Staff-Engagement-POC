package com.staffengagement.interaction.service;

import com.staffengagement.interaction.dto.CreateInteractionRequest;
import com.staffengagement.interaction.dto.InteractionResponse;

import java.util.List;
import java.util.UUID;

public interface InteractionService {
    List<InteractionResponse> findAll();
    InteractionResponse findById(UUID id);
    List<InteractionResponse> findByEmployeeId(UUID employeeId);
    InteractionResponse create(CreateInteractionRequest request);
}
