package com.staffengagement.task.controller;

import com.staffengagement.task.dto.CreateTaskRequest;
import com.staffengagement.task.dto.TaskResponse;
import com.staffengagement.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
class TaskController {

    private final TaskService taskService;

    TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    List<TaskResponse> findAll() {
        return taskService.findAll();
    }

    @GetMapping("/{id}")
    TaskResponse findById(@PathVariable UUID id) {
        return taskService.findById(id);
    }

    @GetMapping(params = "employeeId")
    List<TaskResponse> findByEmployeeId(@RequestParam UUID employeeId) {
        return taskService.findByEmployeeId(employeeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TaskResponse create(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(request);
    }

    @PatchMapping("/{id}/status")
    TaskResponse updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return taskService.updateStatus(id, body.get("status"));
    }
}
