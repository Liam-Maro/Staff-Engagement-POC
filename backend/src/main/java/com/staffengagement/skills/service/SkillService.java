package com.staffengagement.skills.service;

import com.staffengagement.skills.dto.CreateSkillRequest;
import com.staffengagement.skills.dto.SkillResponse;

import java.util.List;
import java.util.UUID;

public interface SkillService {
    List<SkillResponse> findByEmployeeId(UUID employeeId);
    List<SkillResponse> findByName(String name);
    SkillResponse create(CreateSkillRequest request);
    void delete(UUID id);
}
