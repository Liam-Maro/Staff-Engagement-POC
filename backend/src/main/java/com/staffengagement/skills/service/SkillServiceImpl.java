package com.staffengagement.skills.service;

import com.staffengagement.employee.model.Employee;
import com.staffengagement.employee.repository.EmployeeRepository;
import com.staffengagement.shared.exception.EntityNotFoundException;
import com.staffengagement.skills.dto.CreateSkillRequest;
import com.staffengagement.skills.dto.SkillResponse;
import com.staffengagement.skills.dto.SkillSearchResult;
import com.staffengagement.skills.dto.UpdateSkillRequest;
import com.staffengagement.skills.model.Skill;
import com.staffengagement.skills.repository.SkillRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
class SkillServiceImpl implements SkillService {

    private final SkillRepository repository;
    private final EmployeeRepository employeeRepository;

    SkillServiceImpl(SkillRepository repository, EmployeeRepository employeeRepository) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<SkillResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<SkillResponse> findByEmployeeId(UUID employeeId) {
        return repository.findByEmployeeIdOrderByNameAsc(employeeId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<SkillResponse> findByName(String name) {
        return repository.findByName(name).stream().map(this::toResponse).toList();
    }

    @Override
    public List<SkillSearchResult> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<Skill> skills = repository.findByNameContainingIgnoreCase(query);

        return skills.stream()
                .map(skill -> {
                    Employee employee = employeeRepository.findById(skill.getEmployeeId()).orElse(null);
                    if (employee == null) return null;
                    return new SkillSearchResult(
                            employee.getFirstName(),
                            employee.getLastName(),
                            employee.getEmail(),
                            skill.getName(),
                            skill.getYearsExperience(),
                            skill.getProjectCount(),
                            skill.getProficiency()
                    );
                })
                .filter(result -> result != null)
                .sorted(Comparator
                        .<SkillSearchResult>comparingInt(SkillSearchResult::yearsExperience).reversed()
                        .thenComparing(Comparator.<SkillSearchResult>comparingInt(SkillSearchResult::projectCount).reversed()))
                .limit(50)
                .toList();
    }

    @Override
    public SkillResponse create(CreateSkillRequest request) {
        if (!employeeRepository.existsById(request.employeeId())) {
            throw new EntityNotFoundException("Employee not found: " + request.employeeId());
        }

        var skill = new Skill();
        skill.setEmployeeId(request.employeeId());
        skill.setName(request.name());
        skill.setYearsExperience(request.yearsExperience());
        skill.setProjectCount(request.projectCount());
        skill.setProficiency(request.proficiency());
        return toResponse(repository.save(skill));
    }

    @Override
    public SkillResponse update(UUID id, UpdateSkillRequest request) {
        Skill skill = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Skill not found: " + id));
        skill.setName(request.name());
        skill.setYearsExperience(request.yearsExperience());
        skill.setProjectCount(request.projectCount());
        skill.setProficiency(request.proficiency());
        return toResponse(repository.save(skill));
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Skill not found: " + id);
        }
        repository.deleteById(id);
    }

    private SkillResponse toResponse(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getEmployeeId(), skill.getName(),
                skill.getYearsExperience(), skill.getProjectCount(), skill.getProficiency(), skill.getCreatedAt());
    }
}
