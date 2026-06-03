package com.melodyshop.payment.enums;

import java.util.Locale;

public enum PaymentStatus {
    PENDING,
    WAITING_CONFIRMATION,
    SUCCESS,
    FAILED,
    CANCELLED,
    EXPIRED;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED || this == EXPIRED;
    }

    public boolean shouldPublishFailureEvent() {
        return this == FAILED || this == CANCELLED || this == EXPIRED;
    }

    public static PaymentStatus fromWebhookStatus(String rawStatus) {
        try {
            return PaymentStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Webhook status không hợp lệ: " + rawStatus);
        }
    }
}
