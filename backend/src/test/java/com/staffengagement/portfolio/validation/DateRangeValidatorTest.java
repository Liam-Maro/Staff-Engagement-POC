package com.staffengagement.portfolio.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DateRangeValidatorTest {

    private DateRangeValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new DateRangeValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    @Test
    void shouldAcceptWhenEndDateIsNull() {
        DateRangeValidatable dto = createDto(LocalDate.of(2023, 1, 1), null);
        assertThat(validator.isValid(dto, context)).isTrue();
    }

    @Test
    void shouldAcceptWhenStartDateEqualsEndDate() {
        LocalDate date = LocalDate.of(2023, 6, 15);
        DateRangeValidatable dto = createDto(date, date);
        assertThat(validator.isValid(dto, context)).isTrue();
    }

    @Test
    void shouldAcceptWhenStartDateIsBeforeEndDate() {
        DateRangeValidatable dto = createDto(
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 12, 31)
        );
        assertThat(validator.isValid(dto, context)).isTrue();
    }

    @Test
    void shouldRejectWhenStartDateIsAfterEndDate() {
        DateRangeValidatable dto = createDto(
                LocalDate.of(2023, 12, 31),
                LocalDate.of(2023, 1, 1)
        );
        assertThat(validator.isValid(dto, context)).isFalse();
    }

    @Test
    void shouldAcceptWhenStartDateIsNull() {
        DateRangeValidatable dto = createDto(null, LocalDate.of(2023, 6, 15));
        assertThat(validator.isValid(dto, context)).isTrue();
    }

    @Test
    void shouldAcceptWhenObjectIsNull() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    private DateRangeValidatable createDto(LocalDate startDate, LocalDate endDate) {
        return new DateRangeValidatable() {
            @Override
            public LocalDate getStartDate() {
                return startDate;
            }

            @Override
            public LocalDate getEndDate() {
                return endDate;
            }
        };
    }
}
