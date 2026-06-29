package com.staffengagement.task.service;

import com.staffengagement.employee.service.EmployeeService;
import com.staffengagement.interaction.dto.InteractionResponse;
import com.staffengagement.interaction.service.InteractionService;
import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.shared.exception.InactiveStaffException;
import com.staffengagement.shared.exception.InvalidParameterException;
import com.staffengagement.shared.exception.TaskAssignmentForbiddenException;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.service.StaffService;
import com.staffengagement.task.dto.*;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.repository.TaskRepository;
import com.staffengagement.task.repository.TaskSortBuilder;
import com.staffengagement.task.repository.TaskSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
class TaskServiceImpl implements TaskService {

    private final TaskRepository repository;
    private final StaffService staffService;
    private final EmployeeService employeeService;
    private final InteractionService interactionService;

    TaskServiceImpl(TaskRepository repository,
                    StaffService staffService,
                    EmployeeService employeeService,
                    InteractionService interactionService) {
        this.repository = repository;
        this.staffService = staffService;
        this.employeeService = employeeService;
        this.interactionService = interactionService;
    }

    @Override
    public TaskResponse findById(UUID id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id)));
    }

    @Override
    public List<TaskResponse> findByStaffId(UUID staffId) {
        return repository.findByAssigneeId(staffId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TaskQueryResult findTasks(TaskQueryParams params) {
        // 1. Build specification by composing filters with AND logic
        Specification<Task> spec = Specification.where(null);

        if (params.assigneeId() != null) {
            spec = spec.and(TaskSpecifications.hasAssignee(params.assigneeId()));
        }
        if (params.creatorId() != null) {
            spec = spec.and(TaskSpecifications.hasCreator(params.creatorId()));
        }
        if (Boolean.TRUE.equals(params.excludeSelfAssigned())) {
            spec = spec.and(TaskSpecifications.excludeSelfAssigned());
        }
        if (params.status() != null) {
            spec = spec.and(TaskSpecifications.hasStatus(params.status()));
        }
        if (params.dueDateFrom() != null) {
            spec = spec.and(TaskSpecifications.dueDateFrom(params.dueDateFrom()));
        }
        if (params.dueDateTo() != null) {
            spec = spec.and(TaskSpecifications.dueDateTo(params.dueDateTo()));
        }
        if (params.createdFrom() != null) {
            spec = spec.and(TaskSpecifications.createdFrom(params.createdFrom()));
        }
        if (params.createdTo() != null) {
            spec = spec.and(TaskSpecifications.createdTo(params.createdTo()));
        }

        // 2. Build sort with null-dueDate-last handling
        Sort sort = TaskSortBuilder.buildSort(params.sortBy(), params.sortOrder());

        // 3. Build pageable request
        PageRequest pageRequest = PageRequest.of(params.page(), params.size(), sort);

        // 4. Execute query
        Page<Task> page = repository.findAll(spec, pageRequest);

        // 5. Map to response
        List<TaskResponse> tasks = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new TaskQueryResult(tasks, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    public TaskResponse create(CreateTaskRequest request, UUID creatorId) {
        // 1. Validate creator exists and is active
        StaffResponse creator = findStaffOrForbidden(creatorId);
        if (!creator.active()) {
            throw new TaskAssignmentForbiddenException("Creator is not active");
        }

        // 2. Validate assignee exists (404 if not found)
        StaffResponse assignee;
        try {
            assignee = staffService.findById(request.assigneeId());
        } catch (EntityNotFoundException e) {
            throw new EntityNotFoundException("Assignee not found with id: " + request.assigneeId());
        }

        // 3. Validate assignee is active (400 if inactive)
        if (!assignee.active()) {
            throw new InactiveStaffException("Assignee is not active: " + request.assigneeId());
        }

        // 4. Validate individual exists (404 if not found)
        if (!employeeService.existsById(request.individualId())) {
            throw new EntityNotFoundException("Individual not found with id: " + request.individualId());
        }

        // 5. Validate interaction exists and belongs to individual (only when interactionId provided)
        if (request.interactionId() != null) {
            InteractionResponse interaction;
            try {
                interaction = interactionService.findById(request.interactionId());
            } catch (RuntimeException e) {
                throw new InvalidParameterException("Interaction not found with id: " + request.interactionId());
            }
            if (!interaction.employeeId().equals(request.individualId())) {
                throw new InvalidParameterException(
                        "Interaction " + request.interactionId() + " does not belong to individual " + request.individualId());
            }
        }

        // 6. Validate dueDate is not in the past
        if (request.dueDate() != null && request.dueDate().isBefore(LocalDate.now())) {
            throw new InvalidParameterException("Due date must not be in the past");
        }

        // Persist task with status TODO
        Task task = new Task(
                request.individualId(),
                request.interactionId(),
                creatorId,
                request.assigneeId(),
                request.description(),
                TaskStatus.TODO,
                request.dueDate()
        );

        Task saved = repository.save(task);
        return toResponse(saved);
    }

    @Override
    public TaskResponse update(UUID taskId, UpdateTaskRequest request, UUID requesterId) {
        // 1. Validate task exists (404 if not found)
        Task task = repository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + taskId));

        // 2. Validate requester is the creator (403 if not)
        if (!task.getCreatorId().equals(requesterId)) {
            throw new TaskAssignmentForbiddenException("Only the creator can edit the task");
        }

        // 3. Validate new assignee exists (404 if not found) and is active (400 if inactive)
        StaffResponse assignee;
        try {
            assignee = staffService.findById(request.assigneeId());
        } catch (EntityNotFoundException e) {
            throw new EntityNotFoundException("Assignee not found with id: " + request.assigneeId());
        }
        if (!assignee.active()) {
            throw new InactiveStaffException("Assignee is not active: " + request.assigneeId());
        }

        // 4. Validate individual exists (400 if not found — Req 8.6)
        if (!employeeService.existsById(request.individualId())) {
            throw new InvalidParameterException("Individual not found with id: " + request.individualId());
        }

        // 5. Validate interaction only when interactionId is provided; skip entirely when omitted
        if (request.interactionId() != null) {
            InteractionResponse interaction;
            try {
                interaction = interactionService.findById(request.interactionId());
            } catch (RuntimeException e) {
                throw new InvalidParameterException("Interaction not found with id: " + request.interactionId());
            }
            if (!interaction.employeeId().equals(request.individualId())) {
                throw new InvalidParameterException(
                        "Interaction " + request.interactionId() + " does not belong to individual " + request.individualId());
            }
        }

        // 6. Validate dueDate is not in the past (400)
        if (request.dueDate() != null && request.dueDate().isBefore(LocalDate.now())) {
            throw new InvalidParameterException("Due date must not be in the past");
        }

        // 7. Update task fields
        task.setDescription(request.description());
        task.setAssigneeId(request.assigneeId());
        task.setIndividualId(request.individualId());
        task.setDueDate(request.dueDate());
        task.setInteractionId(request.interactionId());

        // 8. Save and return response
        Task saved = repository.save(task);
        return toResponse(saved);
    }

    @Override
    public TaskResponse updateStatus(UUID taskId, UpdateStatusRequest request, UUID requesterId) {
        // 1. Validate task exists (404)
        Task task = repository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + taskId));

        // 2. Validate requester is the assignee (403)
        if (!task.getAssigneeId().equals(requesterId)) {
            throw new TaskAssignmentForbiddenException(
                    "Staff member is not the assignee of this task");
        }

        // 3. Validate status value is a valid enum (400)
        TaskStatus newStatus;
        try {
            newStatus = TaskStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterException(
                    "Invalid status value. Valid values are: TODO, IN_PROGRESS, DONE");
        }

        // 4. Update and persist
        task.setStatus(newStatus);
        Task saved = repository.save(task);

        // 5. Return updated response
        return toResponse(saved);
    }

    @Override
    public void delete(UUID taskId, UUID requesterId) {
        // 1. Validate task exists (404 regardless of requester identity)
        Task task = repository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + taskId));

        // 2. Enforce strict authorization: check requester is creator BEFORE any deletion logic
        if (!task.getCreatorId().equals(requesterId)) {
            throw new TaskAssignmentForbiddenException("Only the creator can delete this task");
        }

        // 3. Delete task and confirm removal before returning
        repository.delete(task);
        repository.flush();
    }

    @Override
    public List<com.staffengagement.task.dto.InteractionResponse> getInteractionsForIndividual(UUID individualId) {
        // 1. Validate individual exists (404 if not — NOT empty list)
        if (!employeeService.existsById(individualId)) {
            throw new EntityNotFoundException("Individual not found with id: " + individualId);
        }

        // 2. Fetch interactions via InteractionService (paginated, sorted by occurredAt DESC)
        var pageable = org.springframework.data.domain.PageRequest.of(0, 50,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "occurredAt"));
        var page = interactionService.findAll(individualId, null, null, null, pageable);

        // 3. Map to task-module DTO
        return page.getContent().stream()
                .map(i -> new com.staffengagement.task.dto.InteractionResponse(
                        i.id(),
                        i.employeeId(),
                        i.staffId(),
                        i.type().name(),
                        i.notes(),
                        i.occurredAt(),
                        i.createdAt()
                ))
                .toList();
    }

    private StaffResponse findStaffOrForbidden(UUID staffId) {
        try {
            return staffService.findById(staffId);
        } catch (EntityNotFoundException e) {
            throw new TaskAssignmentForbiddenException("Staff member not found or not authorized: " + staffId);
        }
    }

    private TaskResponse toResponse(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getIndividualId(),
                t.getInteractionId(),
                t.getCreatorId(),
                t.getAssigneeId(),
                t.getDescription(),
                t.getStatus().getDisplayName(),
                t.getDueDate(),
                t.getCreatedAt()
        );
    }
}
