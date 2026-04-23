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
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        CreatePaymentResponse response = paymentService.createPayment(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<WebhookAcknowledgementResponse>> handleWebhook(
            @Valid @RequestBody PaymentWebhookRequest request) {
        WebhookAcknowledgementResponse response = paymentService.handleWebhook(request);
        return ResponseEntity.ok(ApiResponse.ok(response.getMessage(), response));
    }
}
