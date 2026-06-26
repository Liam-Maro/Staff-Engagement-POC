package com.staffengagement.interaction.repository;

import com.staffengagement.interaction.model.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface InteractionRepository extends JpaRepository<Interaction, UUID>, JpaSpecificationExecutor<Interaction> {
    List<Interaction> findByEmployeeId(UUID employeeId);
    List<Interaction> findByStaffId(UUID staffId);
}
