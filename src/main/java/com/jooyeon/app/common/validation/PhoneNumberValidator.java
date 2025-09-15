package com.jooyeon.app.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    private static final String PHONE_PATTERN = "^01[0-9]-[0-9]{4}-[0-9]{4}$";
    private static final Pattern pattern = Pattern.compile(PHONE_PATTERN);

    @Override
    public void initialize(PhoneNumber constraintAnnotation) {
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return pattern.matcher(phoneNumber).matches();
    }
}