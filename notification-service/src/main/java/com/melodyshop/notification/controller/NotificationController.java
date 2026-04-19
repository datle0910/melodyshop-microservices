package com.melodyshop.notification.controller;

import com.melodyshop.notification.dto.EmailRequest;
import com.melodyshop.notification.dto.OtpRequest;
import com.melodyshop.notification.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal notification endpoints.
 * These are called by other microservices (auth-service, order-service, etc.)
 * via OpenFeign — NOT exposed to the public internet directly.
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;

    /**
     * Generic email endpoint: send any template-based email.
     * POST /api/notifications/email
     */
    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> sendEmail(
            @Valid @RequestBody EmailRequest request) {

        emailService.sendEmail(request);
        return ResponseEntity.ok(Map.of("status", "sent", "to", request.getTo()));
    }

    /**
     * Send OTP email. Returns the generated OTP so the caller can store/validate it.
     * POST /api/notifications/otp
     */
    @PostMapping("/otp")
    public ResponseEntity<Map<String, String>> sendOtp(
            @Valid @RequestBody OtpRequest request) {

        String otp = emailService.sendOtp(request.getTo(), request.getRecipientName());
        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "to", request.getTo(),
                "otp", otp  // caller stores this and validates later
        ));
    }

    /**
     * Send welcome email after user registration.
     * POST /api/notifications/welcome
     */
    @PostMapping("/welcome")
    public ResponseEntity<Map<String, String>> sendWelcome(
            @RequestParam String email,
            @RequestParam String fullName) {

        emailService.sendWelcomeEmail(email, fullName);
        return ResponseEntity.ok(Map.of("status", "sent", "to", email));
    }

    /**
     * Send order confirmation email.
     * POST /api/notifications/order-confirmed
     */
    @PostMapping("/order-confirmed")
    public ResponseEntity<Map<String, String>> sendOrderConfirmed(
            @RequestParam String email,
            @RequestParam String fullName,
            @RequestParam String orderCode,
            @RequestParam String totalAmount) {

        emailService.sendOrderConfirmationEmail(email, fullName, orderCode, totalAmount);
        return ResponseEntity.ok(Map.of("status", "sent", "orderCode", orderCode));
    }

    /** Health / smoke test */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("service", "notification-service", "status", "UP"));
    }
}
