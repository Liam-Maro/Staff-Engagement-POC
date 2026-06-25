package com.staffengagement.portfolio.validation;

import java.time.LocalDate;

/**
 * Interface for DTOs that need date range validation.
 * Classes annotated with @DateRangeValid must implement this interface.
 */
public interface DateRangeValidatable {

    LocalDate getStartDate();

    LocalDate getEndDate();
}
