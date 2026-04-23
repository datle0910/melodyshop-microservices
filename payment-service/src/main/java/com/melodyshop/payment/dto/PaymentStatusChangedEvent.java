package com.melodyshop.payment.dto;

import java.math.BigDecimal;

public class PaymentStatusChangedEvent {
    private String paymentId;
    private String orderId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String gatewayTransactionId;
    private String occurredAt;

    public PaymentStatusChangedEvent() {
    }

    public PaymentStatusChangedEvent(String paymentId,
                                     String orderId,
                                     String status,
                                     BigDecimal amount,
                                     String currency,
                                     String gatewayTransactionId,
                                     String occurredAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.gatewayTransactionId = gatewayTransactionId;
        this.occurredAt = occurredAt;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }
}
