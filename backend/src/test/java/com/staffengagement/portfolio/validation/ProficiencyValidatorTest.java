package com.staffengagement.portfolio.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProficiencyValidatorTest {

    private ProficiencyValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new ProficiencyValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"})
    void shouldAcceptValidProficiencyValues(String proficiency) {
        assertThat(validator.isValid(proficiency, context)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "beginner", "Beginner", "INVALID", "expert ", " ADVANCED", "MASTER"})
    void shouldRejectInvalidProficiencyValues(String proficiency) {
        assertThat(validator.isValid(proficiency, context)).isFalse();
    }

    @Test
    void shouldRejectNull() {
        assertThat(validator.isValid(null, context)).isFalse();
    }
}
