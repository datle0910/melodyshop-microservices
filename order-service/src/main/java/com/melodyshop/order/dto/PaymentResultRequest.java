package com.melodyshop.order.dto;

import lombok.Data;

@Data
public class PaymentResultRequest {
    private String paymentId;
    private String changedBy;
}
