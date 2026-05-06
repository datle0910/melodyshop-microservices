package com.melodyshop.notification.controller;

import com.melodyshop.notification.dto.EmailRequest;
import com.melodyshop.notification.dto.OtpRequest;
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
        return ResponseEntity.ok(ApiResponse.ok("Gửi email thành công",
                Map.of("to", request.getTo())));
    }

    @PostMapping("/otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendOtp(
            @Valid @RequestBody OtpRequest request) {

        String otp = emailService.sendOtp(request.getTo(), request.getRecipientName());
        return ResponseEntity.ok(ApiResponse.ok("Gửi mã OTP thành công",
                Map.of(
                        "to", request.getTo(),
                        "otp", otp
                )));
    }

    @PostMapping("/welcome")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendWelcome(
            @RequestParam String email,
            @RequestParam String fullName) {

        emailService.sendWelcomeEmail(email, fullName);
        return ResponseEntity.ok(ApiResponse.ok("Gửi email chào mừng thành công",
                Map.of("to", email)));
    }

    @PostMapping("/order-confirmed")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendOrderConfirmed(
            @RequestParam String email,
            @RequestParam String fullName,
            @RequestParam String orderCode,
            @RequestParam String totalAmount) {

        emailService.sendOrderConfirmationEmail(email, fullName, orderCode, totalAmount);
        return ResponseEntity.ok(ApiResponse.ok("Gửi email xác nhận đơn hàng thành công",
                Map.of("orderCode", orderCode)));
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.ok("Notification service đang hoạt động"));
    }
}
