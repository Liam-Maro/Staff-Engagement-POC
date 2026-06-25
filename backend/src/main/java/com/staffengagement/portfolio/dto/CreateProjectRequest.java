package com.staffengagement.portfolio.dto;

import com.staffengagement.portfolio.validation.DateRangeValid;
import com.staffengagement.portfolio.validation.DateRangeValidatable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@DateRangeValid
public record CreateProjectRequest(
        @NotBlank @Size(max = 255) String projectName,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 255) String role,
        @NotEmpty @Size(max = 20) List<@Size(max = 100) String> technologies,
        @NotNull LocalDate startDate,
        LocalDate endDate
) implements DateRangeValidatable {

    @Override
    public LocalDate getStartDate() {
        return startDate;
    }

    @Override
    public LocalDate getEndDate() {
        return endDate;
    }
}
