package com.staffengagement.interaction.repository;

import com.staffengagement.interaction.model.InteractionType;
import com.staffengagement.interaction.model.Interaction;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InteractionSpecificationsTest {

    @Test
    void hasEmployeeId_returnsNonNullSpecification() {
        UUID employeeId = UUID.randomUUID();
        Specification<Interaction> spec = InteractionSpecifications.hasEmployeeId(employeeId);
        assertThat(spec).isNotNull();
    }

    @Test
    void hasType_returnsNonNullSpecification() {
        Specification<Interaction> spec = InteractionSpecifications.hasType(InteractionType.CHECK_IN);
        assertThat(spec).isNotNull();
    }

    @Test
    void occurredAfter_returnsNonNullSpecification() {
        LocalDateTime fromDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        Specification<Interaction> spec = InteractionSpecifications.occurredAfter(fromDate);
        assertThat(spec).isNotNull();
    }

    @Test
    void occurredBefore_returnsNonNullSpecification() {
        LocalDateTime toDate = LocalDateTime.of(2024, 12, 31, 23, 59);
        Specification<Interaction> spec = InteractionSpecifications.occurredBefore(toDate);
        assertThat(spec).isNotNull();
    }

    @Test
    void specifications_canBeComposedWithAndLogic() {
        UUID employeeId = UUID.randomUUID();
        LocalDateTime fromDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime toDate = LocalDateTime.of(2024, 12, 31, 23, 59);

        Specification<Interaction> combined = Specification
                .where(InteractionSpecifications.hasEmployeeId(employeeId))
                .and(InteractionSpecifications.hasType(InteractionType.MENTORING))
                .and(InteractionSpecifications.occurredAfter(fromDate))
                .and(InteractionSpecifications.occurredBefore(toDate));

        assertThat(combined).isNotNull();
    }

    @Test
    void specifications_canBeComposedSelectivelyWithAndLogic() {
        // Simulates the service layer composing only provided filters
        UUID employeeId = UUID.randomUUID();
        InteractionType type = null; // not provided
        LocalDateTime fromDate = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime toDate = null; // not provided

        Specification<Interaction> spec = Specification.where(null);

        spec = spec.and(InteractionSpecifications.hasEmployeeId(employeeId));

        if (type != null) {
            spec = spec.and(InteractionSpecifications.hasType(type));
        }

        spec = spec.and(InteractionSpecifications.occurredAfter(fromDate));

        if (toDate != null) {
            spec = spec.and(InteractionSpecifications.occurredBefore(toDate));
        }

        assertThat(spec).isNotNull();
    }
}
