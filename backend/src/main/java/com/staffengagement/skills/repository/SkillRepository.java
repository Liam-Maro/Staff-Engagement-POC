package com.staffengagement.skills.repository;

import com.staffengagement.skills.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findByEmployeeIdOrderByNameAsc(UUID employeeId);
    List<Skill> findByName(String name);
    List<Skill> findByNameContainingIgnoreCase(String name);
    List<Skill> findByEmployeeIdAndSource(UUID employeeId, String source);
    Optional<Skill> findByEmployeeIdAndNameAndSource(UUID employeeId, String name, String source);
}
