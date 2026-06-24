package com.staffengagement.staff.service;

import com.staffengagement.staff.dto.CreateStaffRequest;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.model.Staff;
import com.staffengagement.staff.repository.StaffRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
class StaffServiceImpl implements StaffService {

    private final StaffRepository repository;

    StaffServiceImpl(StaffRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<StaffResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public StaffResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Staff not found: " + id));
    }

    @Override
    public StaffResponse create(CreateStaffRequest request) {
        var staff = new Staff();
        staff.setEmployeeId(request.employeeId());
        staff.setRole(request.role());
        return toResponse(repository.save(staff));
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private StaffResponse toResponse(Staff s) {
        return new StaffResponse(s.getId(), s.getEmployeeId(), s.getRole(), s.isActive(), s.getCreatedAt());
    }
}
