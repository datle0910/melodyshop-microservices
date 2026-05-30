package com.melodyshop.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.payment.dto.CreatePaymentRequest;
import com.melodyshop.payment.dto.CreatePaymentResponse;
import com.melodyshop.payment.dto.PaymentStatusChangedEvent;
import com.melodyshop.payment.dto.PaymentWebhookRequest;
import com.melodyshop.payment.dto.WebhookAcknowledgementResponse;
import com.melodyshop.payment.entity.OutboxEvent;
import com.melodyshop.payment.entity.PaymentTransaction;
import com.melodyshop.payment.enums.OutboxStatus;
import com.melodyshop.payment.enums.PaymentStatus;
import com.melodyshop.payment.repository.OutboxEventRepository;
import com.melodyshop.payment.repository.PaymentTransactionRepository;
import com.melodyshop.payment.service.gateway.PaymentGateway;
import com.melodyshop.payment.service.gateway.PaymentGatewayFactory;
import com.melodyshop.payment.service.PaymentService;
import com.melodyshop.payment.service.WebhookSignatureService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final WebhookSignatureService webhookSignatureService;
    private final ObjectMapper objectMapper;

    public PaymentServiceImpl(PaymentTransactionRepository paymentTransactionRepository,
                              OutboxEventRepository outboxEventRepository,
                              PaymentGatewayFactory paymentGatewayFactory,
                              WebhookSignatureService webhookSignatureService,
                              ObjectMapper objectMapper) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentGatewayFactory = paymentGatewayFactory;
        this.webhookSignatureService = webhookSignatureService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public CreatePaymentResponse createPayment(String idempotencyKey, CreatePaymentRequest request) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        String normalizedOrderId = request.getOrderId().trim();
        String normalizedCurrency = request.getCurrency().trim().toUpperCase(Locale.ROOT);

        Optional<PaymentTransaction> existingByKey = paymentTransactionRepository.findByIdempotencyKey(normalizedIdempotencyKey);
        if (existingByKey.isPresent()) {
            PaymentTransaction existingPayment = existingByKey.get();
            validateRepeatedCreateRequest(existingPayment, normalizedOrderId, request.getAmount(), normalizedCurrency);
            return toCreatePaymentResponse(existingPayment);
        }

        if (paymentTransactionRepository.existsBySuccessfulPaymentKey(normalizedOrderId)) {
            throw new BadRequestException("Đơn hàng đã được thanh toán thành công");
        }

        paymentTransactionRepository.findByActivePaymentKey(normalizedOrderId)
                .ifPresent(payment -> {
                    throw new BadRequestException("Đơn hàng đang có giao dịch thanh toán chờ xử lý");
                });

        PaymentGateway gateway = paymentGatewayFactory.getGateway(request.getProvider());

        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setOrderId(normalizedOrderId);
        paymentTransaction.setAmount(request.getAmount());
        paymentTransaction.setCurrency(normalizedCurrency);
        paymentTransaction.setIdempotencyKey(normalizedIdempotencyKey);
        paymentTransaction.setGatewayTransactionId(gateway.generateGatewayTransactionId());
        paymentTransaction.setProvider(gateway.getProviderName());
        paymentTransaction.transitionTo(PaymentStatus.PENDING);

        try {
            paymentTransaction = paymentTransactionRepository.saveAndFlush(paymentTransaction);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Không thể tạo payment mới vì đơn hàng đang có giao dịch active hoặc đã được thanh toán");
        }

        String paymentUrl = gateway.buildPaymentUrl(paymentTransaction);

        return new CreatePaymentResponse(
                paymentTransaction.getId(),
                paymentUrl
        );
    }

    @Override
    @Transactional
    public WebhookAcknowledgementResponse handleWebhook(PaymentWebhookRequest request) {
        if (!webhookSignatureService.isValid(request)) {
            throw new BadRequestException("Chữ ký webhook không hợp lệ");
        }

        PaymentStatus incomingStatus;
        try {
            incomingStatus = PaymentStatus.fromWebhookStatus(request.getStatus());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }

        if (incomingStatus == PaymentStatus.PENDING) {
            throw new BadRequestException("Webhook không chấp nhận trạng thái PENDING");
        }

        PaymentTransaction paymentTransaction = paymentTransactionRepository
                .findByGatewayTransactionId(request.getGatewayTransactionId().trim())
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", "gatewayTransactionId", request.getGatewayTransactionId()));

        if (!paymentTransaction.getOrderId().equals(request.getOrderId().trim())) {
            throw new BadRequestException("orderId trong webhook không khớp với payment");
        }

        if (request.getAmount() != null && paymentTransaction.getAmount().compareTo(request.getAmount()) != 0) {
            throw new BadRequestException("amount trong webhook không khớp với payment");
        }

        if (StringUtils.hasText(request.getCurrency())
                && !paymentTransaction.getCurrency().equalsIgnoreCase(request.getCurrency().trim())) {
            throw new BadRequestException("currency trong webhook không khớp với payment");
        }

        if (paymentTransaction.getStatus() == incomingStatus) {
            return new WebhookAcknowledgementResponse(
                    false,
                    paymentTransaction.getId(),
                    paymentTransaction.getStatus().name(),
                    "Webhook trùng lặp, trạng thái đã được xử lý trước đó"
            );
        }

        if (paymentTransaction.getStatus().isTerminal()) {
            return new WebhookAcknowledgementResponse(
                    false,
                    paymentTransaction.getId(),
                    paymentTransaction.getStatus().name(),
                    "Payment đã ở trạng thái terminal, bỏ qua webhook xung đột"
            );
        }

        paymentTransaction.transitionTo(incomingStatus);

        try {
            paymentTransaction = paymentTransactionRepository.saveAndFlush(paymentTransaction);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Không thể cập nhật payment vì order đã có giao dịch thành công khác");
        }

        saveOutboxEvent(paymentTransaction, incomingStatus);

        return new WebhookAcknowledgementResponse(
                true,
                paymentTransaction.getId(),
                paymentTransaction.getStatus().name(),
                "Webhook đã được xử lý thành công"
        );
    }

    private CreatePaymentResponse toCreatePaymentResponse(PaymentTransaction paymentTransaction) {
        PaymentGateway gateway = paymentGatewayFactory.getGateway(paymentTransaction.getProvider());
        String paymentUrl = gateway.buildPaymentUrl(paymentTransaction);
        return new CreatePaymentResponse(
                paymentTransaction.getId(),
                paymentUrl
        );
    }

    private void validateRepeatedCreateRequest(PaymentTransaction paymentTransaction,
                                               String orderId,
                                               BigDecimal amount,
                                               String currency) {
        boolean sameRequest = Objects.equals(paymentTransaction.getOrderId(), orderId)
                && paymentTransaction.getAmount().compareTo(amount) == 0
                && paymentTransaction.getCurrency().equalsIgnoreCase(currency);
        if (!sameRequest) {
            throw new BadRequestException("Idempotency-Key đã được dùng cho request khác");
        }
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BadRequestException("Idempotency-Key không được để trống");
        }
        return idempotencyKey.trim();
    }

    private void saveOutboxEvent(PaymentTransaction paymentTransaction, PaymentStatus status) {
        String eventType = status == PaymentStatus.SUCCESS ? "payment_succeeded" : "payment_failed";
        PaymentStatusChangedEvent eventPayload = new PaymentStatusChangedEvent(
                paymentTransaction.getId(),
                paymentTransaction.getOrderId(),
                status.name(),
                paymentTransaction.getAmount(),
                paymentTransaction.getCurrency(),
                paymentTransaction.getGatewayTransactionId(),
                LocalDateTime.now().toString()
        );

        OutboxEvent outboxEvent = new OutboxEvent(eventType, toJson(eventPayload), OutboxStatus.NEW);
        outboxEventRepository.save(outboxEvent);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể serialize payload outbox", ex);
        }
    }
}
