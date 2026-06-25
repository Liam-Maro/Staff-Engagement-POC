package com.staffengagement.portfolio.repository;

import com.staffengagement.portfolio.model.PortfolioLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PortfolioLinkRepository extends JpaRepository<PortfolioLink, UUID> {

    List<PortfolioLink> findByEmployeeId(UUID employeeId);
}
