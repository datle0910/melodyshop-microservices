package com.melodyshop.payment.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.payment.dto.*;
import com.melodyshop.payment.service.InternalServiceTokenValidator;
import com.melodyshop.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final InternalServiceTokenValidator internalTokenValidator;

    @PostMapping
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(
            @RequestHeader("X-Internal-Service-Token") String internalToken,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        internalTokenValidator.requireValid(internalToken);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = "auto_" + System.currentTimeMillis();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(paymentService.createPayment(userId, idempotencyKey, request)));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPayment(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String paymentId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPayment(paymentId, userId, role)));
    }

    @PostMapping("/{paymentId}/mark-transferred")
    public ResponseEntity<ApiResponse<PaymentDTO>> markTransferred(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String paymentId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Shop đang kiểm tra thanh toán. Đơn hàng sẽ được xác nhận sau khi nhận được tiền.",
                paymentService.markTransferred(paymentId, userId)));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<WebhookAcknowledgementResponse>> handleWebhook(
            @Valid @RequestBody PaymentWebhookRequest request) {
        WebhookAcknowledgementResponse response = paymentService.handleWebhook(request);
        return ResponseEntity.ok(ApiResponse.ok(response.getMessage(), response));
    }

    @PostMapping("/vnpay/callback")
    public ResponseEntity<Void> handleVnpayCallback(
            @RequestParam(value = "vnp_ResponseCode", required = false) String responseCode,
            @RequestParam(value = "vnp_TxnRef", required = false) String txnRef) {
        return redirectToCheckout("00".equals(responseCode), txnRef);
    }

    @PostMapping("/momo/callback")
    public ResponseEntity<Void> handleMomoCallback(
            @RequestParam(value = "resultCode", required = false) String resultCode,
            @RequestParam(value = "orderId", required = false) String orderId) {
        return redirectToCheckout("0".equals(resultCode), orderId);
    }

    private ResponseEntity<Void> redirectToCheckout(boolean success, String orderId) {
        String status = success ? "success" : "failed";
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/checkout?payment=" + status + "&orderId=" + orderId)
                .build();
    }
}
