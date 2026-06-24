package com.staffengagement.skills.repository;

import com.staffengagement.skills.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findByEmployeeId(UUID employeeId);
    List<Skill> findByName(String name);
    List<Skill> findByNameContainingIgnoreCase(String name);
}
