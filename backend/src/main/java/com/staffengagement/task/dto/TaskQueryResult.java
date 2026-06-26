package com.staffengagement.task.dto;

import java.util.List;

public record TaskQueryResult(
        List<TaskResponse> tasks,
        long totalCount,
        int currentPage,
        int pageSize
) {}
