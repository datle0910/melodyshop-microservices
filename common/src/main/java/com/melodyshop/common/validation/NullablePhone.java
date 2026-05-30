package com.melodyshop.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NullablePhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NullablePhone {
    String message() default "Số điện thoại không hợp lệ (phải bắt đầu bằng 0, có 10-11 chữ số)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
