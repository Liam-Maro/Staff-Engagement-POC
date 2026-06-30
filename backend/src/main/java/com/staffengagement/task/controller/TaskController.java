package com.staffengagement.task.controller;

import com.staffengagement.shared.exception.InvalidParameterException;
import com.staffengagement.staff.model.Staff;
import com.staffengagement.task.dto.*;
import com.staffengagement.task.model.TaskStatus;
import com.staffengagement.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
class TaskController {

    private static final Set<String> VALID_SORT_BY = Set.of("dueDate", "createdDate");
    private static final Set<String> VALID_SORT_ORDER = Set.of("asc", "desc");

    private final TaskService taskService;

    TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    TaskQueryResult findTasks(
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) String creatorId,
            @RequestParam(required = false) Boolean excludeSelfAssigned,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dueDateFrom,
            @RequestParam(required = false) String dueDateTo,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        // Validate UUID params
        UUID parsedAssigneeId = parseUuidParam(assigneeId, "assigneeId");
        UUID parsedCreatorId = parseUuidParam(creatorId, "creatorId");

        // Validate sortBy
        if (!VALID_SORT_BY.contains(sortBy)) {
            throw new InvalidParameterException(
                    "Invalid sortBy value: '" + sortBy + "'. Valid values are: dueDate, createdDate");
        }

        // Validate sortOrder (case-insensitive)
        if (!VALID_SORT_ORDER.contains(sortOrder.toLowerCase())) {
            throw new InvalidParameterException(
                    "Invalid sortOrder value: '" + sortOrder + "'. Valid values are: asc, desc");
        }

        // Validate status (case-insensitive)
        TaskStatus parsedStatus = null;
        if (status != null) {
            try {
                parsedStatus = TaskStatus.fromValue(status);
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterException(
                        "Invalid status value: '" + status + "'. Valid values are: To Do, In Progress, Done");
            }
        }

        // Validate date formats
        LocalDate parsedDueDateFrom = parseDateParam(dueDateFrom, "dueDateFrom");
        LocalDate parsedDueDateTo = parseDateParam(dueDateTo, "dueDateTo");
        LocalDate parsedCreatedFrom = parseDateParam(createdFrom, "createdFrom");
        LocalDate parsedCreatedTo = parseDateParam(createdTo, "createdTo");

        TaskQueryParams params = new TaskQueryParams(
                parsedAssigneeId,
                parsedCreatorId,
                excludeSelfAssigned,
                parsedStatus,
                parsedDueDateFrom,
                parsedDueDateTo,
                parsedCreatedFrom,
                parsedCreatedTo,
                sortBy,
                sortOrder.toLowerCase(),
                page,
                size
        );

        return taskService.findTasks(params);
    }

    @GetMapping("/{id}")
    TaskResponse findById(@PathVariable String id) {
        UUID taskId = parseUuidPathVariable(id);
        return taskService.findById(taskId);
    }

    @GetMapping(params = "staffId")
    List<TaskResponse> findByStaffId(@RequestParam UUID staffId) {
        return taskService.findByStaffId(staffId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TaskResponse create(@Valid @RequestBody CreateTaskRequest request,
                        @AuthenticationPrincipal Staff staff) {
        UUID creatorId = staff.getId();
        return taskService.create(request, creatorId);
    }

    @PutMapping("/{id}")
    TaskResponse update(@PathVariable String id,
                        @Valid @RequestBody UpdateTaskRequest request,
                        @AuthenticationPrincipal Staff staff) {
        UUID taskId = parseUuidPathVariable(id);
        UUID requesterId = staff.getId();
        return taskService.update(taskId, request, requesterId);
    }

    @PatchMapping("/{id}/status")
    TaskResponse updateStatus(@PathVariable String id,
                              @Valid @RequestBody UpdateStatusRequest request,
                              @AuthenticationPrincipal Staff staff) {
        UUID taskId = parseUuidPathVariable(id);
        UUID requesterId = staff.getId();
        return taskService.updateStatus(taskId, request, requesterId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id,
                @AuthenticationPrincipal Staff staff) {
        UUID taskId = parseUuidPathVariable(id);
        UUID requesterId = staff.getId();
        taskService.delete(taskId, requesterId);
    }

    @GetMapping("/interactions")
    List<InteractionResponse> getInteractionsForIndividual(@RequestParam String individualId) {
        UUID parsedIndividualId = parseUuidParam(individualId, "individualId");
        if (parsedIndividualId == null) {
            throw new InvalidParameterException(
                    "individualId is required and must be a valid UUID format");
        }
        return taskService.getInteractionsForIndividual(parsedIndividualId);
    }

    private UUID parseUuidParam(String value, String paramName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterException(
                    "Invalid UUID format for " + paramName + ": '" + value + "'");
        }
    }

    private UUID parseUuidPathVariable(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterException(
                    "Invalid UUID format for task ID: '" + value + "'");
        }
    }

    private LocalDate parseDateParam(String value, String paramName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidParameterException(
                    "Invalid date format for " + paramName + ": '" + value + "'. Expected format: yyyy-MM-dd");
        }
    }
}
