package com.staffengagement.task.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateTaskRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private UpdateTaskRequest validRequest() {
        return new UpdateTaskRequest(
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                "A valid updated description",
                LocalDate.now().plusDays(7)
        );
    }

    @Test
    void validRequest_shouldPassValidation() {
        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @Test
    void nullIndividualId_shouldProduceViolation() {
        var request = new UpdateTaskRequest(null, null, UUID.randomUUID(), "Description", null);

        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Individual ID is required");
    }

    @Test
    void nullAssigneeId_shouldProduceViolation() {
        var request = new UpdateTaskRequest(UUID.randomUUID(), null, null, "Description", null);

        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Assignee ID is required");
    }

    @Test
    void nullDescription_shouldProduceViolation() {
        var request = new UpdateTaskRequest(UUID.randomUUID(), null, UUID.randomUUID(), null, null);

        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Description must not be blank"));
    }

    @Test
    void emptyDescription_shouldProduceViolation() {
        var request = new UpdateTaskRequest(UUID.randomUUID(), null, UUID.randomUUID(), "", null);

        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Description must not be blank"));
    }

    @Test
    void whitespaceOnlyDescription_shouldProduceViolation() {
        var request = new UpdateTaskRequest(UUID.randomUUID(), null, UUID.randomUUID(), "   \t\n  ", null);

        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Description must not be blank"));
    }

    @Test
    void descriptionExceeding2000Characters_shouldProduceViolation() {
        String oversized = "x".repeat(2001);
        var request = new UpdateTaskRequest(UUID.randomUUID(), null, UUID.randomUUID(), oversized, null);

        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Description must not exceed 2000 characters");
    }

    @Test
    void descriptionExactly2000Characters_shouldPassValidation() {
        String exact = "x".repeat(2000);
        var request = new UpdateTaskRequest(UUID.randomUUID(), null, UUID.randomUUID(), exact, null);

        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void multipleInvalidFields_shouldProduceMultipleViolations() {
        var request = new UpdateTaskRequest(null, null, null, "", null);

        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void allNullFields_shouldProduceMultipleViolations() {
        var request = new UpdateTaskRequest(null, null, null, null, null);

        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(request);

        assertThat(violations).hasSizeGreaterThanOrEqualTo(3);
    }
}
