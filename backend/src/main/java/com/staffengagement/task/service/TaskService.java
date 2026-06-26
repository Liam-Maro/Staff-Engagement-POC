package com.staffengagement.task.service;

import com.staffengagement.task.dto.*;

import java.util.List;
import java.util.UUID;

public interface TaskService {
    TaskResponse findById(UUID id);
    TaskQueryResult findTasks(TaskQueryParams params);
    TaskResponse create(CreateTaskRequest request, UUID creatorId);
    TaskResponse update(UUID taskId, UpdateTaskRequest request, UUID requesterId);
    TaskResponse updateStatus(UUID taskId, UpdateStatusRequest request, UUID requesterId);
    void delete(UUID taskId, UUID requesterId);
    List<InteractionResponse> getInteractionsForIndividual(UUID individualId);
}
