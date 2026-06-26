package com.staffengagement.interaction.repository;

import com.staffengagement.interaction.model.Interaction;
import com.staffengagement.interaction.model.InteractionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public class InteractionSpecifications {

    private InteractionSpecifications() {
        // Utility class — prevent instantiation
    }

    public static Specification<Interaction> hasEmployeeId(UUID employeeId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("employeeId"), employeeId);
    }

    public static Specification<Interaction> hasType(InteractionType type) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("type"), type);
    }

    public static Specification<Interaction> occurredAfter(LocalDateTime fromDate) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), fromDate);
    }

    public static Specification<Interaction> occurredBefore(LocalDateTime toDate) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("occurredAt"), toDate);
    }
}
