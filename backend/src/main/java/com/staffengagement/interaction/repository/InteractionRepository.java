package com.staffengagement.interaction.repository;

import com.staffengagement.interaction.model.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InteractionRepository extends JpaRepository<Interaction, UUID> {
    List<Interaction> findByEmployeeId(UUID employeeId);
}
