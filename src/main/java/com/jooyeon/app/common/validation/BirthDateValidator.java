package com.jooyeon.app.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class BirthDateValidator implements ConstraintValidator<BirthDate, String> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void initialize(BirthDate constraintAnnotation) {
    }

    @Override
    public boolean isValid(String birthDate, ConstraintValidatorContext context) {
        if (birthDate == null || birthDate.trim().isEmpty()) {
            return false;
        }

        try {
            LocalDate date = LocalDate.parse(birthDate, DATE_FORMATTER);
            return !date.isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}