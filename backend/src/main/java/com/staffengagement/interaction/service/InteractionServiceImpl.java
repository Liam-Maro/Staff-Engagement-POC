package com.staffengagement.interaction.service;

import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.interaction.dto.CreateFollowUpTaskRequest;
import com.staffengagement.interaction.dto.CreateInteractionRequest;
import com.staffengagement.interaction.dto.InteractionResponse;
import com.staffengagement.interaction.dto.UpdateInteractionRequest;
import com.staffengagement.interaction.exception.InteractionNotFoundException;
import com.staffengagement.interaction.exception.InvalidDateRangeException;
import com.staffengagement.interaction.exception.TaskCreationFailedException;
import com.staffengagement.interaction.model.Interaction;
import com.staffengagement.interaction.model.InteractionType;
import com.staffengagement.interaction.repository.InteractionRepository;
import com.staffengagement.interaction.repository.InteractionSpecifications;
import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
class InteractionServiceImpl implements InteractionService {

    private final InteractionRepository repository;
    private final EmployeeService employeeService;
    private final TaskService taskService;

    InteractionServiceImpl(InteractionRepository repository,
                           EmployeeService employeeService,
                           TaskService taskService) {
        this.repository = repository;
        this.employeeService = employeeService;
        this.taskService = taskService;
    }

    @Override
    public InteractionResponse create(CreateInteractionRequest request) {
        // Validate employee exists (throws if not found)
        employeeService.findById(request.employeeId());

        var interaction = new Interaction();
        interaction.setEmployeeId(request.employeeId());
        interaction.setStaffId(request.staffId());
        interaction.setType(request.type());
        interaction.setNotes(request.notes());
        interaction.setOccurredAt(request.occurredAt());

        return toResponse(repository.save(interaction));
    }

    @Override
    public InteractionResponse update(UUID id, UpdateInteractionRequest request) {
        var interaction = repository.findById(id)
                .orElseThrow(() -> new InteractionNotFoundException(id));

        // Update only type, notes, and occurredAt — preserve employeeId and staffId
        interaction.setType(request.type());
        interaction.setNotes(request.notes());
        interaction.setOccurredAt(request.occurredAt());

        return toResponse(repository.save(interaction));
    }

    @Override
    public InteractionResponse findById(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new InteractionNotFoundException(id));
    }

    @Override
    public Page<InteractionResponse> findAll(UUID employeeId, InteractionType type,
                                              LocalDateTime fromDate, LocalDateTime toDate,
                                              Pageable pageable) {
        // Validate date range
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new InvalidDateRangeException();
        }

        // Cap page size at 100
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100,
                    Sort.by(Sort.Direction.DESC, "occurredAt"));
        }

        // Compose specifications dynamically
        Specification<Interaction> spec = Specification.where(null);

        if (employeeId != null) {
            spec = spec.and(InteractionSpecifications.hasEmployeeId(employeeId));
        }
        if (type != null) {
            spec = spec.and(InteractionSpecifications.hasType(type));
        }
        if (fromDate != null) {
            spec = spec.and(InteractionSpecifications.occurredAfter(fromDate));
        }
        if (toDate != null) {
            spec = spec.and(InteractionSpecifications.occurredBefore(toDate));
        }

        return repository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    public void delete(UUID id) {
        var interaction = repository.findById(id)
                .orElseThrow(() -> new InteractionNotFoundException(id));
        repository.delete(interaction);
    }

    @Override
    public Object createFollowUpTask(UUID interactionId, CreateFollowUpTaskRequest request) {
        var interaction = repository.findById(interactionId)
                .orElseThrow(() -> new InteractionNotFoundException(interactionId));

        var taskRequest = new CreateTaskRequest(
                interaction.getEmployeeId(),
                interactionId,
                request.title(),
                request.description(),
                request.dueDate()
        );

        try {
            return taskService.create(taskRequest);
        } catch (Exception e) {
            throw new TaskCreationFailedException(e.getMessage());
        }
    }

    private InteractionResponse toResponse(Interaction i) {
        return new InteractionResponse(i.getId(), i.getEmployeeId(), i.getStaffId(),
                i.getType(), i.getNotes(), i.getOccurredAt(), i.getCreatedAt(), i.getUpdatedAt());
    }
}
