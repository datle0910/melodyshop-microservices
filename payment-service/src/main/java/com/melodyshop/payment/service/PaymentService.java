package com.melodyshop.payment.service;

import com.melodyshop.payment.dto.CreatePaymentRequest;
import com.melodyshop.payment.dto.CreatePaymentResponse;
import com.melodyshop.payment.dto.PaymentWebhookRequest;
import com.melodyshop.payment.dto.WebhookAcknowledgementResponse;
import com.melodyshop.payment.dto.PaymentDTO;
import com.melodyshop.payment.enums.PaymentStatus;

import java.util.List;

public interface PaymentService {
    CreatePaymentResponse createPayment(String userId, String idempotencyKey, CreatePaymentRequest request);
    WebhookAcknowledgementResponse handleWebhook(PaymentWebhookRequest request);
    PaymentDTO getPayment(String paymentId, String userId, String role);
    PaymentDTO markTransferred(String paymentId, String userId);
    List<PaymentDTO> getAdminVietQrPayments(PaymentStatus status);
    PaymentDTO confirmPayment(String paymentId, String adminId);
    PaymentDTO rejectPayment(String paymentId, String adminId);
    List<String> findExpiredVietQrPaymentIds();
    void expirePayment(String paymentId);
}
