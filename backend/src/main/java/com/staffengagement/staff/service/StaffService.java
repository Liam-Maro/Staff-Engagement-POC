package com.staffengagement.staff.service;

import com.staffengagement.staff.dto.CreateStaffRequest;
import com.staffengagement.staff.dto.StaffResponse;

import java.util.List;
import java.util.UUID;

public interface StaffService {
    List<StaffResponse> findAll();
    StaffResponse findById(UUID id);
    StaffResponse create(CreateStaffRequest request);
    void delete(UUID id);
}
