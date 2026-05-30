package com.melodyshop.order.dto;

import com.melodyshop.order.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {
    @NotBlank
    private String orderId;

    @NotBlank
    private String orderNumber;

    @NotNull
    @DecimalMin(value = "1000")
    private BigDecimal amount;

    @NotNull
    private PaymentMethod paymentMethod;

    private String provider;

    private String currency;
}
