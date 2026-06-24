package com.staffengagement.staff.service;

import com.staffengagement.staff.dto.CreateStaffRequest;
import com.staffengagement.staff.dto.StaffResponse;
import com.staffengagement.staff.dto.UpdateStaffRequest;

import java.util.List;
import java.util.UUID;

public interface StaffService {
    List<StaffResponse> findAll();
    StaffResponse findById(UUID id);
    StaffResponse create(CreateStaffRequest request);
    StaffResponse update(UUID id, UpdateStaffRequest request);
    void deactivate(UUID id);
    void delete(UUID id);
}
