package com.staffengagement.task.service;

import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.model.Task;
import com.staffengagement.task.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
class TaskServiceImpl implements TaskService {

    private final TaskRepository repository;

    TaskServiceImpl(TaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TaskResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public TaskResponse findById(UUID id) {
        return toResponse(repository.findById(id).orElseThrow());
    }

    @Override
    public List<TaskResponse> findByEmployeeId(UUID employeeId) {
        return repository.findByEmployeeId(employeeId).stream().map(this::toResponse).toList();
    }

    @Override
    public TaskResponse create(CreateTaskRequest request) {
        var task = new Task(request.employeeId(), request.interactionId(), request.title(),
                request.description(), "OPEN", request.dueDate());
        return toResponse(repository.save(task));
    }

    @Override
    public TaskResponse updateStatus(UUID id, String status) {
        var task = repository.findById(id).orElseThrow();
        task.setStatus(status);
        return toResponse(repository.save(task));
    }

    private TaskResponse toResponse(Task t) {
        return new TaskResponse(t.getId(), t.getEmployeeId(), t.getInteractionId(),
                t.getTitle(), t.getDescription(), t.getStatus(), t.getDueDate(), t.getCreatedAt());
    }
}
