package com.melodyshop.payment.dto;

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
    private String orderId;
    private String paymentId;
    private String paymentStatus;
    private String paymentMethod;
    private BigDecimal amount;
    private String currency;
    private String redirectUrl;
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String transferContent;
    private String qrCode;
    private String qrUrl;
    private LocalDateTime expiredAt;

    public CreatePaymentResponse(String paymentId, String redirectUrl) {
        this.paymentId = paymentId;
        this.redirectUrl = redirectUrl;
    }
}
