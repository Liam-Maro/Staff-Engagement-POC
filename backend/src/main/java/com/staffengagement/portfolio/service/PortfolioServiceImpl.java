package com.staffengagement.portfolio.service;

import com.staffengagement.portfolio.dto.CreatePortfolioItemRequest;
import com.staffengagement.portfolio.dto.PortfolioItemResponse;
import com.staffengagement.portfolio.model.PortfolioItem;
import com.staffengagement.portfolio.repository.PortfolioItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioItemRepository repository;

    PortfolioServiceImpl(PortfolioItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PortfolioItemResponse> findByEmployeeId(UUID employeeId) {
        return repository.findByEmployeeId(employeeId).stream().map(this::toResponse).toList();
    }

    @Override
    public PortfolioItemResponse create(CreatePortfolioItemRequest request) {
        var item = new PortfolioItem(request.employeeId(), request.type(), request.title(),
                request.description(), request.url(), request.dateObtained());
        return toResponse(repository.save(item));
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private PortfolioItemResponse toResponse(PortfolioItem i) {
        return new PortfolioItemResponse(i.getId(), i.getEmployeeId(), i.getType(),
                i.getTitle(), i.getDescription(), i.getUrl(), i.getDateObtained(), i.getCreatedAt());
    }
}
