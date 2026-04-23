package com.melodyshop.payment.service.impl;

import com.melodyshop.payment.dto.PaymentWebhookRequest;
import com.melodyshop.payment.service.WebhookSignatureService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

@Service
public class WebhookSignatureServiceImpl implements WebhookSignatureService {

    @Value("${payment.gateway.webhook-secret:payment_webhook_secret}")
    private String webhookSecret;

    @Override
    public boolean isValid(PaymentWebhookRequest request) {
        String rawPayload = request.getGatewayTransactionId().trim()
                + "|" + request.getOrderId().trim()
                + "|" + request.getStatus().trim().toUpperCase(Locale.ROOT)
                + "|" + webhookSecret;
        return sha256Hex(rawPayload).equalsIgnoreCase(request.getSignature().trim());
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
