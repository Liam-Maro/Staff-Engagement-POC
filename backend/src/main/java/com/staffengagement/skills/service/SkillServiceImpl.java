package com.staffengagement.skills.service;

import com.staffengagement.skills.dto.CreateSkillRequest;
import com.staffengagement.skills.dto.SkillResponse;
import com.staffengagement.skills.model.Skill;
import com.staffengagement.skills.repository.SkillRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
class SkillServiceImpl implements SkillService {

    private final SkillRepository repository;

    SkillServiceImpl(SkillRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SkillResponse> findByEmployeeId(UUID employeeId) {
        return repository.findByEmployeeId(employeeId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<SkillResponse> findByName(String name) {
        return repository.findByName(name).stream().map(this::toResponse).toList();
    }

    @Override
    public SkillResponse create(CreateSkillRequest request) {
        var skill = new Skill();
        skill.setEmployeeId(request.employeeId());
        skill.setName(request.name());
        skill.setYearsExperience(request.yearsExperience());
        skill.setProjectCount(request.projectCount());
        skill.setProficiency(request.proficiency());
        return toResponse(repository.save(skill));
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private SkillResponse toResponse(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getEmployeeId(), skill.getName(),
                skill.getYearsExperience(), skill.getProjectCount(), skill.getProficiency(), skill.getCreatedAt());
    }
}
