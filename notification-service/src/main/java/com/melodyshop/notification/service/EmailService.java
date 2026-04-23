package com.melodyshop.notification.service;

import com.melodyshop.notification.dto.EmailRequest;

public interface EmailService {

    /**
     * Send a generic template-based email.
     */
    void sendEmail(EmailRequest request);

    /**
     * Generate a 6-digit OTP and send it to the given email.
     * @return the generated OTP (so caller can store/validate it)
     */
    String sendOtp(String toEmail, String recipientName);

    /**
     * Send welcome email after successful registration.
     */
    void sendWelcomeEmail(String toEmail, String fullName);

    /**
     * Send order confirmation email.
     */
    void sendOrderConfirmationEmail(String toEmail, String fullName, String orderCode, String totalAmount);
}
