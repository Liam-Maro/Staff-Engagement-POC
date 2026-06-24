package com.staffengagement.portfolio.controller;

import com.staffengagement.portfolio.dto.CreatePortfolioItemRequest;
import com.staffengagement.portfolio.dto.PortfolioItemResponse;
import com.staffengagement.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolios")
class PortfolioController {

    private final PortfolioService portfolioService;

    PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping(params = "employeeId")
    List<PortfolioItemResponse> findByEmployeeId(@RequestParam UUID employeeId) {
        return portfolioService.findByEmployeeId(employeeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PortfolioItemResponse create(@Valid @RequestBody CreatePortfolioItemRequest request) {
        return portfolioService.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        portfolioService.delete(id);
    }
}
