package com.staffengagement.skills.validation;

import com.staffengagement.skills.model.Proficiency;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ProficiencyValidator implements ConstraintValidator<ValidProficiency, String> {

    private static final Set<String> VALID_VALUES = Arrays.stream(Proficiency.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return VALID_VALUES.contains(value);
    }
}
