package com.staffengagement.portfolio.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class DateRangeValidator implements ConstraintValidator<DateRangeValid, DateRangeValidatable> {

    @Override
    public boolean isValid(DateRangeValidatable value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDate startDate = value.getStartDate();
        LocalDate endDate = value.getEndDate();

        // If endDate is null, it means ongoing project — valid
        if (endDate == null) {
            return true;
        }

        // If startDate is null, let other validators handle it
        if (startDate == null) {
            return true;
        }

        return !startDate.isAfter(endDate);
    }
}
