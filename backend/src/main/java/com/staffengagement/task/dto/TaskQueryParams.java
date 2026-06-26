package com.staffengagement.task.dto;

import com.staffengagement.task.model.TaskStatus;

import java.time.LocalDate;
import java.util.UUID;

public record TaskQueryParams(
        UUID assigneeId,
        UUID creatorId,
        Boolean excludeSelfAssigned,
        TaskStatus status,
        LocalDate dueDateFrom,
        LocalDate dueDateTo,
        LocalDate createdFrom,
        LocalDate createdTo,
        String sortBy,
        String sortOrder,
        int page,
        int size
) {}
