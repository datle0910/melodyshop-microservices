package com.melodyshop.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentResponse {
    private String paymentId;
    private String redirectUrl;
    private String qrCode;
    private String qrUrl;
    private String orderId;
    private String paymentStatus;
    private String paymentMethod;
    private BigDecimal amount;
    private String currency;
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String transferContent;
    private LocalDateTime expiredAt;
}
