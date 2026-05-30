package com.melodyshop.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class NullablePhoneValidator implements ConstraintValidator<NullablePhone, String> {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9,10}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return PHONE_PATTERN.matcher(value).matches();
    }
}
