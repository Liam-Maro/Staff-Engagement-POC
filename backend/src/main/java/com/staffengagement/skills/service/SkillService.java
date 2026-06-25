package com.staffengagement.skills.service;

import com.staffengagement.skills.dto.CreateSkillRequest;
import com.staffengagement.skills.dto.SkillResponse;
import com.staffengagement.skills.dto.SkillSearchResult;
import com.staffengagement.skills.dto.UpdateSkillRequest;

import java.util.List;
import java.util.UUID;

public interface SkillService {
    List<SkillResponse> findAll();
    List<SkillResponse> findByEmployeeId(UUID employeeId);
    List<SkillResponse> findByName(String name);
    List<SkillSearchResult> search(String query);
    SkillResponse create(CreateSkillRequest request);
    SkillResponse update(UUID id, UpdateSkillRequest request);
    void delete(UUID id);
}
