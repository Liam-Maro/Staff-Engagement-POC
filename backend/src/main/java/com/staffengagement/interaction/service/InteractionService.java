package com.staffengagement.interaction.service;

import com.staffengagement.interaction.dto.CreateFollowUpTaskRequest;
import com.staffengagement.interaction.dto.CreateInteractionRequest;
import com.staffengagement.interaction.dto.InteractionResponse;
import com.staffengagement.interaction.dto.UpdateInteractionRequest;
import com.staffengagement.interaction.model.InteractionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface InteractionService {
    InteractionResponse create(CreateInteractionRequest request);
    InteractionResponse update(UUID id, UpdateInteractionRequest request);
    InteractionResponse findById(UUID id);
    Page<InteractionResponse> findAll(UUID employeeId, InteractionType type,
                                       LocalDateTime fromDate, LocalDateTime toDate,
                                       Pageable pageable);
    void delete(UUID id);
    Object createFollowUpTask(UUID interactionId, CreateFollowUpTaskRequest request);
}
