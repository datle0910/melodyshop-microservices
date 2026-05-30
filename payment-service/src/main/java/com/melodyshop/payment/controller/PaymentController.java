package com.melodyshop.payment.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.payment.dto.CreatePaymentRequest;
import com.melodyshop.payment.dto.CreatePaymentResponse;
import com.melodyshop.payment.dto.PaymentWebhookRequest;
import com.melodyshop.payment.dto.WebhookAcknowledgementResponse;
import com.melodyshop.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = "auto_" + System.currentTimeMillis();
        }
        CreatePaymentResponse response = paymentService.createPayment(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
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
            @RequestParam(value = "vnp_TxnRef", required = false) String txnRef,
            @RequestParam(value = "vnp_Amount", required = false) String amount) {
        if ("00".equals(responseCode)) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/checkout?payment=success&orderId=" + txnRef)
                    .build();
        } else {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/checkout?payment=failed&orderId=" + txnRef)
                    .build();
        }
    }

    @PostMapping("/momo/callback")
    public ResponseEntity<Void> handleMomoCallback(
            @RequestParam(value = "resultCode", required = false) String resultCode,
            @RequestParam(value = "orderId", required = false) String orderId) {
        if ("0".equals(resultCode)) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/checkout?payment=success&orderId=" + orderId)
                    .build();
        } else {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/checkout?payment=failed&orderId=" + orderId)
                    .build();
        }
    }
}
