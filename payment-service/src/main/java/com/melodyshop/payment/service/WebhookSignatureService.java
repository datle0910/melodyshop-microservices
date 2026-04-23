package com.melodyshop.payment.service;

import com.melodyshop.payment.dto.PaymentWebhookRequest;

public interface WebhookSignatureService {
    boolean isValid(PaymentWebhookRequest request);
}
