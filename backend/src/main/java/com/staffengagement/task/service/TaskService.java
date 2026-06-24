package com.staffengagement.task.service;

import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.dto.TaskResponse;

import java.util.List;
import java.util.UUID;

public interface TaskService {
    List<TaskResponse> findAll();
    TaskResponse findById(UUID id);
    List<TaskResponse> findByEmployeeId(UUID employeeId);
    TaskResponse create(CreateTaskRequest request);
    TaskResponse updateStatus(UUID id, String status);
}
