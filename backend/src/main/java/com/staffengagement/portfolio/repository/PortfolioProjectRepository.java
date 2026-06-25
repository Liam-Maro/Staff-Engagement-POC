package com.staffengagement.portfolio.repository;

import com.staffengagement.portfolio.model.PortfolioProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PortfolioProjectRepository extends JpaRepository<PortfolioProject, UUID> {

    List<PortfolioProject> findByEmployeeIdOrderByStartDateDesc(UUID employeeId);
}
