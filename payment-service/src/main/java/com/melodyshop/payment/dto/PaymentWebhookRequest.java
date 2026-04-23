package com.melodyshop.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class PaymentWebhookRequest {

    @NotBlank(message = "gatewayTransactionId không được để trống")
    @Size(max = 100)
    private String gatewayTransactionId;

    @NotBlank(message = "orderId không được để trống")
    @Size(max = 64)
    private String orderId;

    @NotBlank(message = "status không được để trống")
    @Size(max = 20)
    private String status;

    @NotBlank(message = "signature không được để trống")
    private String signature;

    @Size(max = 100)
    private String eventId;

    private BigDecimal amount;

    @Size(max = 10)
    private String currency;

    public PaymentWebhookRequest() {
    }

    public PaymentWebhookRequest(String gatewayTransactionId,
                                 String orderId,
                                 String status,
                                 String signature,
                                 String eventId,
                                 BigDecimal amount,
                                 String currency) {
        this.gatewayTransactionId = gatewayTransactionId;
        this.orderId = orderId;
        this.status = status;
        this.signature = signature;
        this.eventId = eventId;
        this.amount = amount;
        this.currency = currency;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
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

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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
}
