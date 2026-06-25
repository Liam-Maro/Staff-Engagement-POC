package com.staffengagement.portfolio.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class ProficiencyValidator implements ConstraintValidator<ValidProficiency, String> {

    private static final Set<String> VALID_VALUES = Set.of(
            "BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return VALID_VALUES.contains(value);
    }
}
