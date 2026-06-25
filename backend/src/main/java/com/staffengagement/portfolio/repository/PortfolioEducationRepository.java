package com.staffengagement.portfolio.repository;

import com.staffengagement.portfolio.model.PortfolioEducation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PortfolioEducationRepository extends JpaRepository<PortfolioEducation, UUID> {

    List<PortfolioEducation> findByEmployeeIdOrderByGraduationDateDesc(UUID employeeId);
}
