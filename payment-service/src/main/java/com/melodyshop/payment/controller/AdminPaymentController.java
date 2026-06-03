package com.melodyshop.payment.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.payment.dto.PaymentDTO;
import com.melodyshop.payment.enums.PaymentStatus;
import com.melodyshop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentDTO>>> getVietQrPayments(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) PaymentStatus status) {
        requireAdmin(role);
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getAdminVietQrPayments(status)));
    }

    @PostMapping("/{paymentId}/confirm")
    public ResponseEntity<ApiResponse<PaymentDTO>> confirmPayment(
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String paymentId) {
        requireAdmin(role);
        return ResponseEntity.ok(ApiResponse.ok("Đã xác nhận nhận tiền", paymentService.confirmPayment(paymentId, adminId)));
    }

    @PostMapping("/{paymentId}/reject")
    public ResponseEntity<ApiResponse<PaymentDTO>> rejectPayment(
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String paymentId) {
        requireAdmin(role);
        return ResponseEntity.ok(ApiResponse.ok("Đã từ chối thanh toán", paymentService.rejectPayment(paymentId, adminId)));
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equalsIgnoreCase(role) && !"ROLE_ADMIN".equalsIgnoreCase(role)) {
            throw new BadRequestException("Chỉ admin mới được xác nhận thanh toán");
        }
    }
}
