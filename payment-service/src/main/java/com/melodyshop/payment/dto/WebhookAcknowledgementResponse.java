package com.melodyshop.payment.dto;

public class WebhookAcknowledgementResponse {
    private boolean processed;
    private String paymentId;
    private String status;
    private String message;

    public WebhookAcknowledgementResponse() {
    }

    public WebhookAcknowledgementResponse(boolean processed, String paymentId, String status, String message) {
        this.processed = processed;
        this.paymentId = paymentId;
        this.status = status;
        this.message = message;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
