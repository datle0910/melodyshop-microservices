package com.melodyshop.notification.service;

import com.melodyshop.notification.dto.EmailRequest;

public interface EmailService {

    /**
     * Send a generic template-based email.
     */
    void sendEmail(EmailRequest request);

    /**
     * Generate a 6-digit OTP and send it to the given email.
     * @param externalOtp if provided, uses this OTP instead of generating a new one
     * @return the OTP that was sent
     */
    String sendOtp(String toEmail, String recipientName, String externalOtp);

    /**
     * Send welcome email after successful registration.
     */
    void sendWelcomeEmail(String toEmail, String fullName);

    /**
     * Send order confirmation email.
     */
    void sendOrderConfirmationEmail(String toEmail, String fullName, String orderCode, String totalAmount);
}
