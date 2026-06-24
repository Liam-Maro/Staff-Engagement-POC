package com.staffengagement.staff.service;

import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.staff.dto.CreateStaffRequest;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.dto.UpdateStaffRequest;
import com.staffengagement.staff.model.Staff;
import com.staffengagement.staff.repository.StaffRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
class StaffServiceImpl implements StaffService {

    private final StaffRepository repository;
    private final PasswordEncoder passwordEncoder;

    StaffServiceImpl(StaffRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<StaffResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public StaffResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + id));
    }

    @Override
    public StaffResponse create(CreateStaffRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("A staff member with this email already exists.");
        }
        var staff = new Staff();
        staff.setEmployeeId(request.employeeId());
        staff.setEmail(request.email());
        staff.setPassword(passwordEncoder.encode(request.password()));
        staff.setRole(request.role());
        staff.setActive(true);
        return toResponse(repository.save(staff));
    }

    @Override
    public StaffResponse update(UUID id, UpdateStaffRequest request) {
        Staff staff = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + id));
        staff.setRole(request.role());
        staff.setActive(request.active());
        return toResponse(repository.save(staff));
    }

    @Override
    public void deactivate(UUID id) {
        Staff staff = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + id));
        staff.setActive(false);
        repository.save(staff);
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Staff not found: " + id);
        }
        repository.deleteById(id);
    }

    private StaffResponse toResponse(Staff s) {
        return new StaffResponse(s.getId(), s.getEmployeeId(), s.getEmail(), s.getRole(), s.isActive(), s.getCreatedAt());
    }
}
