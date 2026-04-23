package com.melodyshop.payment.dto;

public class CreatePaymentResponse {
    private String paymentId;
    private String redirectUrl;

    public CreatePaymentResponse() {
    }

    public CreatePaymentResponse(String paymentId, String redirectUrl) {
        this.paymentId = paymentId;
        this.redirectUrl = redirectUrl;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}
