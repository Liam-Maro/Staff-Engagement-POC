package com.staffengagement.portfolio.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ProficiencyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidProficiency {

    String message() default "Proficiency must be one of: BEGINNER, INTERMEDIATE, ADVANCED, EXPERT";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
