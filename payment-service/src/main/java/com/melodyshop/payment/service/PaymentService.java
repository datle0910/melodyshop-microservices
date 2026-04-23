package com.melodyshop.payment.service;

import com.melodyshop.payment.dto.CreatePaymentRequest;
import com.melodyshop.payment.dto.CreatePaymentResponse;
import com.melodyshop.payment.dto.PaymentWebhookRequest;
import com.melodyshop.payment.dto.WebhookAcknowledgementResponse;

public interface PaymentService {
    CreatePaymentResponse createPayment(String idempotencyKey, CreatePaymentRequest request);
    WebhookAcknowledgementResponse handleWebhook(PaymentWebhookRequest request);
}
