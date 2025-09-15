package com.jooyeon.app.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BirthDateValidator.class)
@Documented
public @interface BirthDate {
    String message() default "Invalid birth date format (YYYY-MM-DD) or future date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}