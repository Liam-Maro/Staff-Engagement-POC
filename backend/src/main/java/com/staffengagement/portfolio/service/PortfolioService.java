package com.staffengagement.portfolio.service;

import com.staffengagement.portfolio.dto.CreatePortfolioItemRequest;
import com.staffengagement.portfolio.dto.PortfolioItemResponse;

import java.util.List;
import java.util.UUID;

public interface PortfolioService {
    List<PortfolioItemResponse> findByEmployeeId(UUID employeeId);
    PortfolioItemResponse create(CreatePortfolioItemRequest request);
    void delete(UUID id);
}
