package com.melodyshop.notification.controller;

import com.melodyshop.notification.dto.EmailRequest;
import com.melodyshop.notification.dto.OtpRequest;
import com.melodyshop.notification.dto.WelcomeRequest;
import com.melodyshop.notification.service.EmailService;
import com.melodyshop.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendEmail(
            @Valid @RequestBody EmailRequest request) {

        emailService.sendEmail(request);
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("Email sent successfully")
                .data(Map.of(
                        "status", "sent",
                        "to", request.getTo()
                ))
                .build());
    }

    @PostMapping("/otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendOtp(
            @Valid @RequestBody OtpRequest request) {

        String otp = emailService.sendOtp(request.getTo(), request.getRecipientName(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("OTP sent successfully")
                .data(Map.of(
                        "status", "sent",
                        "to", request.getTo(),
                        "otp", otp
                ))
                .build());
    }

    @PostMapping("/welcome")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendWelcome(@RequestBody WelcomeRequest request) {
        emailService.sendWelcomeEmail(request.getEmail(), request.getFullName());
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("Welcome email sent successfully")
                .data(Map.of(
                        "status", "sent",
                        "to", request.getEmail()
                ))
                .build());
    }

    @PostMapping("/order-confirmed")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendOrderConfirmed(
            @RequestParam String email,
            @RequestParam String fullName,
            @RequestParam String orderCode,
            @RequestParam String totalAmount) {

        emailService.sendOrderConfirmationEmail(email, fullName, orderCode, totalAmount);
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("Order confirmation email sent successfully")
                .data(Map.of(
                        "status", "sent",
                        "orderCode", orderCode
                ))
                .build());
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, String>>> ping() {
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("Notification service is running")
                .data(Map.of(
                        "service", "notification-service",
                        "status", "UP"
                ))
                .build());
    }
}
