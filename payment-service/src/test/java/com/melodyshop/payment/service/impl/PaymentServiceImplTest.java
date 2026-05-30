package com.melodyshop.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.payment.dto.CreatePaymentRequest;
import com.melodyshop.payment.dto.PaymentWebhookRequest;
import com.melodyshop.payment.entity.OutboxEvent;
import com.melodyshop.payment.entity.PaymentTransaction;
import com.melodyshop.payment.enums.PaymentStatus;
import com.melodyshop.payment.repository.OutboxEventRepository;
import com.melodyshop.payment.repository.PaymentTransactionRepository;
import com.melodyshop.payment.service.WebhookSignatureService;
import com.melodyshop.payment.service.gateway.PaymentGateway;
import com.melodyshop.payment.service.gateway.PaymentGatewayFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private PaymentGatewayFactory paymentGatewayFactory;

    @Mock
    private WebhookSignatureService webhookSignatureService;

    @Mock
    private PaymentGateway paymentGateway;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentTransactionRepository,
                outboxEventRepository,
                paymentGatewayFactory,
                webhookSignatureService,
                new ObjectMapper()
        );
    }

    @Test
    void shouldReturnExistingPaymentWhenIdempotencyKeyRepeats() {
        PaymentTransaction existingPayment = payment("pay-1", "order-1", "idem-1", "VNPAY", PaymentStatus.PENDING);
        when(paymentTransactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existingPayment));
        when(paymentGatewayFactory.getGateway("VNPAY")).thenReturn(paymentGateway);
        when(paymentGateway.buildPaymentUrl(existingPayment)).thenReturn("http://fake-gateway/pay/pay-1");

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order-1");
        request.setAmount(new BigDecimal("49.90"));
        request.setCurrency("USD");
        request.setProvider("VNPAY");

        var response = paymentService.createPayment("idem-1", request);

        assertEquals("pay-1", response.getPaymentId());
        assertEquals("http://fake-gateway/pay/pay-1", response.getRedirectUrl());
        verify(paymentTransactionRepository, never()).saveAndFlush(any(PaymentTransaction.class));
    }

    @Test
    void shouldRejectCreatePaymentWhenAnotherActivePaymentExists() {
        PaymentTransaction activePayment = payment("pay-2", "order-2", "idem-old", "VNPAY", PaymentStatus.PENDING);
        when(paymentTransactionRepository.findByIdempotencyKey("idem-new")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.existsBySuccessfulPaymentKey("order-2")).thenReturn(false);
        when(paymentTransactionRepository.findByActivePaymentKey("order-2")).thenReturn(Optional.of(activePayment));

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("order-2");
        request.setAmount(new BigDecimal("199.00"));
        request.setCurrency("VND");
        request.setProvider("VNPAY");

        assertThrows(BadRequestException.class, () -> paymentService.createPayment("idem-new", request));
        verify(paymentTransactionRepository, never()).saveAndFlush(any(PaymentTransaction.class));
    }

    @Test
    void shouldPublishOutboxOnlyOnceWhenWebhookIsDuplicated() {
        PaymentTransaction paymentTransaction = payment("pay-3", "order-3", "idem-3", "VNPAY", PaymentStatus.PENDING);
        PaymentWebhookRequest webhookRequest = new PaymentWebhookRequest();
        webhookRequest.setGatewayTransactionId("GW-3");
        webhookRequest.setOrderId("order-3");
        webhookRequest.setStatus("SUCCESS");
        webhookRequest.setSignature("valid-signature");
        webhookRequest.setAmount(new BigDecimal("49.90"));
        webhookRequest.setCurrency("USD");

        when(webhookSignatureService.isValid(webhookRequest)).thenReturn(true);
        when(paymentTransactionRepository.findByGatewayTransactionId("GW-3")).thenReturn(Optional.of(paymentTransaction));
        when(paymentTransactionRepository.saveAndFlush(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var firstResponse = paymentService.handleWebhook(webhookRequest);
        var secondResponse = paymentService.handleWebhook(webhookRequest);

        assertTrue(firstResponse.isProcessed());
        assertFalse(secondResponse.isProcessed());
        assertEquals("SUCCESS", paymentTransaction.getStatus().name());
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertEquals("payment_succeeded", outboxCaptor.getValue().getEventType());
    }

    private PaymentTransaction payment(String id,
                                       String orderId,
                                       String idempotencyKey,
                                       String provider,
                                       PaymentStatus status) {
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setId(id);
        paymentTransaction.setOrderId(orderId);
        paymentTransaction.setAmount(new BigDecimal("49.90"));
        paymentTransaction.setCurrency("USD");
        paymentTransaction.setIdempotencyKey(idempotencyKey);
        paymentTransaction.setGatewayTransactionId("GW-" + id);
        paymentTransaction.setProvider(provider);
        paymentTransaction.transitionTo(status);
        return paymentTransaction;
    }
}
