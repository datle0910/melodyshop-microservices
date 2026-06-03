package com.melodyshop.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.payment.client.OrderClient;
import com.melodyshop.payment.dto.*;
import com.melodyshop.payment.entity.OutboxEvent;
import com.melodyshop.payment.entity.PaymentTransaction;
import com.melodyshop.payment.enums.OutboxStatus;
import com.melodyshop.payment.enums.PaymentStatus;
import com.melodyshop.payment.repository.OutboxEventRepository;
import com.melodyshop.payment.repository.PaymentTransactionRepository;
import com.melodyshop.payment.service.PaymentService;
import com.melodyshop.payment.service.VietQrService;
import com.melodyshop.payment.service.WebhookSignatureService;
import com.melodyshop.payment.service.gateway.PaymentGateway;
import com.melodyshop.payment.service.gateway.PaymentGatewayFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String VIETQR = "VIETQR";

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final WebhookSignatureService webhookSignatureService;
    private final ObjectMapper objectMapper;
    private final VietQrService vietQrService;
    private final OrderClient orderClient;

    @Value("${internal.service-token}")
    private String internalServiceToken;

    @Override
    @Transactional
    public CreatePaymentResponse createPayment(String userId, String idempotencyKey, CreatePaymentRequest request) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        String normalizedOrderId = request.getOrderId().trim();
        String normalizedCurrency = request.getCurrency().trim().toUpperCase(Locale.ROOT);

        Optional<PaymentTransaction> existingByKey = paymentTransactionRepository.findByIdempotencyKey(normalizedIdempotencyKey);
        if (existingByKey.isPresent()) {
            PaymentTransaction existingPayment = existingByKey.get();
            validateRepeatedCreateRequest(existingPayment, normalizedOrderId, request.getAmount(), normalizedCurrency, userId);
            return toCreatePaymentResponse(existingPayment, null);
        }

        if (paymentTransactionRepository.existsBySuccessfulPaymentKey(normalizedOrderId)) {
            throw new BadRequestException("Đơn hàng đã được thanh toán thành công");
        }

        paymentTransactionRepository.findByActivePaymentKey(normalizedOrderId)
                .ifPresent(payment -> {
                    throw new BadRequestException("Đơn hàng đang có giao dịch thanh toán chờ xử lý");
                });

        boolean vietQr = isVietQr(request);
        if (vietQr && !StringUtils.hasText(userId)) {
            throw new BadRequestException("VietQR chỉ hỗ trợ người dùng đã đăng nhập");
        }

        PaymentGateway gateway = vietQr ? null : paymentGatewayFactory.getGateway(request.getProvider());
        PaymentTransaction payment = new PaymentTransaction();
        payment.setOrderId(normalizedOrderId);
        payment.setUserId(normalizeNullable(userId));
        payment.setAmount(request.getAmount());
        payment.setCurrency(normalizedCurrency);
        payment.setMethod(normalizeNullable(request.getPaymentMethod()));
        payment.setIdempotencyKey(normalizedIdempotencyKey);
        payment.setGatewayTransactionId(vietQr
                ? "VIETQR_" + System.currentTimeMillis() + "_" + normalizedOrderId.substring(0, Math.min(8, normalizedOrderId.length()))
                : gateway.generateGatewayTransactionId());
        payment.setProvider(vietQr ? VIETQR : gateway.getProviderName());
        payment.transitionTo(PaymentStatus.PENDING);

        try {
            payment = paymentTransactionRepository.saveAndFlush(payment);
            if (vietQr) {
                vietQrService.populateVietQrDetails(payment);
                payment = paymentTransactionRepository.saveAndFlush(payment);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Không thể tạo payment mới vì đơn hàng đang có giao dịch active hoặc đã được thanh toán");
        }

        String paymentUrl = vietQr ? null : gateway.buildPaymentUrl(payment);
        return toCreatePaymentResponse(payment, paymentUrl);
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

        if (incomingStatus == PaymentStatus.PENDING || incomingStatus == PaymentStatus.WAITING_CONFIRMATION) {
            throw new BadRequestException("Webhook không chấp nhận trạng thái chờ xử lý");
        }

        PaymentTransaction payment = paymentTransactionRepository
                .findByGatewayTransactionId(request.getGatewayTransactionId().trim())
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", "gatewayTransactionId", request.getGatewayTransactionId()));

        if (!payment.getOrderId().equals(request.getOrderId().trim())) {
            throw new BadRequestException("orderId trong webhook không khớp với payment");
        }
        if (request.getAmount() != null && payment.getAmount().compareTo(request.getAmount()) != 0) {
            throw new BadRequestException("amount trong webhook không khớp với payment");
        }
        if (StringUtils.hasText(request.getCurrency())
                && !payment.getCurrency().equalsIgnoreCase(request.getCurrency().trim())) {
            throw new BadRequestException("currency trong webhook không khớp với payment");
        }
        if (payment.getStatus() == incomingStatus) {
            return acknowledgement(payment, false, "Webhook trùng lặp, trạng thái đã được xử lý trước đó");
        }
        if (payment.getStatus().isTerminal()) {
            return acknowledgement(payment, false, "Payment đã ở trạng thái terminal, bỏ qua webhook xung đột");
        }

        payment.transitionTo(incomingStatus);
        payment = paymentTransactionRepository.saveAndFlush(payment);
        synchronizeOrder(payment, null);
        saveOutboxEvent(payment, incomingStatus);
        return acknowledgement(payment, true, "Webhook đã được xử lý thành công");
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPayment(String paymentId, String userId, String role) {
        PaymentTransaction payment = getPaymentOrThrow(paymentId);
        if (!isAdmin(role) && !Objects.equals(payment.getUserId(), userId)) {
            throw new BadRequestException("Bạn không có quyền xem payment này");
        }
        return toDTO(payment);
    }

    @Override
    @Transactional
    public PaymentDTO markTransferred(String paymentId, String userId) {
        PaymentTransaction payment = getPaymentForUpdate(paymentId);
        requireOwner(payment, userId);
        if (!VIETQR.equals(payment.getProvider())) {
            throw new BadRequestException("API này chỉ áp dụng cho thanh toán VietQR");
        }
        if (payment.getStatus() == PaymentStatus.WAITING_CONFIRMATION) {
            return toDTO(payment);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Không thể báo chuyển khoản ở trạng thái " + payment.getStatus());
        }
        if (isExpired(payment)) {
            expireLockedPayment(payment);
            return toDTO(payment);
        }
        payment.transitionTo(PaymentStatus.WAITING_CONFIRMATION);
        return toDTO(paymentTransactionRepository.saveAndFlush(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDTO> getAdminVietQrPayments(PaymentStatus status) {
        List<PaymentStatus> statuses = status == null
                ? List.of(PaymentStatus.PENDING, PaymentStatus.WAITING_CONFIRMATION)
                : List.of(status);
        return paymentTransactionRepository.findByProviderAndStatusInOrderByCreatedAtAsc(VIETQR, statuses)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public PaymentDTO confirmPayment(String paymentId, String adminId) {
        PaymentTransaction payment = getPaymentForUpdate(paymentId);
        requireVietQr(payment);
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            synchronizeOrder(payment, adminId);
            return toDTO(payment);
        }
        if (isExpired(payment)) {
            expireLockedPayment(payment);
            return toDTO(payment);
        }
        if (payment.getStatus() != PaymentStatus.PENDING
                && payment.getStatus() != PaymentStatus.WAITING_CONFIRMATION) {
            throw new BadRequestException("Không thể xác nhận payment ở trạng thái " + payment.getStatus());
        }

        payment.transitionTo(PaymentStatus.SUCCESS);
        payment.setConfirmedBy(adminId);
        payment.setConfirmedAt(LocalDateTime.now());
        payment = paymentTransactionRepository.saveAndFlush(payment);
        synchronizeOrder(payment, adminId);
        saveOutboxEvent(payment, PaymentStatus.SUCCESS);
        return toDTO(payment);
    }

    @Override
    @Transactional
    public PaymentDTO rejectPayment(String paymentId, String adminId) {
        PaymentTransaction payment = getPaymentForUpdate(paymentId);
        requireVietQr(payment);
        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.CANCELLED) {
            return toDTO(payment);
        }
        if (payment.getStatus().isTerminal()) {
            throw new BadRequestException("Không thể từ chối payment ở trạng thái " + payment.getStatus());
        }

        payment.transitionTo(PaymentStatus.FAILED);
        payment.setConfirmedBy(adminId);
        payment.setConfirmedAt(LocalDateTime.now());
        payment = paymentTransactionRepository.saveAndFlush(payment);
        synchronizeOrder(payment, adminId);
        saveOutboxEvent(payment, PaymentStatus.FAILED);
        return toDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findExpiredVietQrPaymentIds() {
        return paymentTransactionRepository.findByProviderAndStatusInAndExpiredAtBefore(
                        VIETQR,
                        List.of(PaymentStatus.PENDING, PaymentStatus.WAITING_CONFIRMATION),
                        LocalDateTime.now())
                .stream()
                .map(PaymentTransaction::getId)
                .toList();
    }

    @Override
    @Transactional
    public void expirePayment(String paymentId) {
        PaymentTransaction payment = getPaymentForUpdate(paymentId);
        if (!VIETQR.equals(payment.getProvider())
                || payment.getStatus().isTerminal()
                || !isExpired(payment)) {
            return;
        }
        expireLockedPayment(payment);
    }

    private void expireLockedPayment(PaymentTransaction payment) {
        payment.transitionTo(PaymentStatus.EXPIRED);
        payment = paymentTransactionRepository.saveAndFlush(payment);
        synchronizeOrder(payment, null);
        saveOutboxEvent(payment, PaymentStatus.EXPIRED);
    }

    private void synchronizeOrder(PaymentTransaction payment, String changedBy) {
        PaymentOrderRequest request = new PaymentOrderRequest(payment.getId(), changedBy);
        ApiResponse<Void> response = switch (payment.getStatus()) {
            case SUCCESS -> orderClient.markPaymentSuccess(internalServiceToken, payment.getOrderId(), request);
            case EXPIRED -> orderClient.markPaymentExpired(internalServiceToken, payment.getOrderId(), request);
            case FAILED, CANCELLED -> orderClient.markPaymentFailed(internalServiceToken, payment.getOrderId(), request);
            default -> null;
        };
        if (response != null && !response.isSuccess()) {
            throw new BadRequestException("Order service không thể đồng bộ trạng thái payment: " + response.getMessage());
        }
    }

    private WebhookAcknowledgementResponse acknowledgement(PaymentTransaction payment, boolean processed, String message) {
        return new WebhookAcknowledgementResponse(processed, payment.getId(), payment.getStatus().name(), message);
    }

    private CreatePaymentResponse toCreatePaymentResponse(PaymentTransaction payment, String redirectUrl) {
        String resolvedRedirectUrl = redirectUrl;
        if (resolvedRedirectUrl == null && !VIETQR.equals(payment.getProvider())) {
            resolvedRedirectUrl = paymentGatewayFactory.getGateway(payment.getProvider()).buildPaymentUrl(payment);
        }
        return CreatePaymentResponse.builder()
                .orderId(payment.getOrderId())
                .paymentId(payment.getId())
                .paymentStatus(payment.getStatus().name())
                .paymentMethod(payment.getMethod())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .redirectUrl(resolvedRedirectUrl)
                .bankCode(payment.getBankCode())
                .bankName(payment.getBankName())
                .accountNumber(payment.getAccountNumber())
                .accountName(payment.getAccountName())
                .transferContent(payment.getTransferContent())
                .qrCode(payment.getQrCode())
                .qrUrl(payment.getQrUrl())
                .expiredAt(payment.getExpiredAt())
                .build();
    }

    private PaymentDTO toDTO(PaymentTransaction payment) {
        return PaymentDTO.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .method(payment.getMethod())
                .provider(payment.getProvider())
                .status(payment.getStatus())
                .bankCode(payment.getBankCode())
                .bankName(payment.getBankName())
                .accountNumber(payment.getAccountNumber())
                .accountName(payment.getAccountName())
                .transferContent(payment.getTransferContent())
                .qrCode(payment.getQrCode())
                .qrUrl(payment.getQrUrl())
                .expiredAt(payment.getExpiredAt())
                .confirmedBy(payment.getConfirmedBy())
                .confirmedAt(payment.getConfirmedAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private void validateRepeatedCreateRequest(PaymentTransaction payment, String orderId,
                                               BigDecimal amount, String currency, String userId) {
        boolean sameRequest = Objects.equals(payment.getOrderId(), orderId)
                && payment.getAmount().compareTo(amount) == 0
                && payment.getCurrency().equalsIgnoreCase(currency)
                && Objects.equals(payment.getUserId(), normalizeNullable(userId));
        if (!sameRequest) {
            throw new BadRequestException("Idempotency-Key đã được dùng cho request khác");
        }
    }

    private PaymentTransaction getPaymentOrThrow(String paymentId) {
        return paymentTransactionRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", "id", paymentId));
    }

    private PaymentTransaction getPaymentForUpdate(String paymentId) {
        return paymentTransactionRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", "id", paymentId));
    }

    private void requireOwner(PaymentTransaction payment, String userId) {
        if (!StringUtils.hasText(userId) || !Objects.equals(payment.getUserId(), userId)) {
            throw new BadRequestException("Bạn không có quyền cập nhật payment này");
        }
    }

    private void requireVietQr(PaymentTransaction payment) {
        if (!VIETQR.equals(payment.getProvider())) {
            throw new BadRequestException("API admin manual-confirm chỉ áp dụng cho VietQR");
        }
    }

    private boolean isVietQr(CreatePaymentRequest request) {
        return VIETQR.equalsIgnoreCase(request.getProvider())
                || VIETQR.equalsIgnoreCase(request.getPaymentMethod());
    }

    private boolean isExpired(PaymentTransaction payment) {
        return payment.getExpiredAt() != null && !payment.getExpiredAt().isAfter(LocalDateTime.now());
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BadRequestException("Idempotency-Key không được để trống");
        }
        return idempotencyKey.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void saveOutboxEvent(PaymentTransaction payment, PaymentStatus status) {
        String eventType = status == PaymentStatus.SUCCESS ? "payment_succeeded" : "payment_failed";
        PaymentStatusChangedEvent eventPayload = new PaymentStatusChangedEvent(
                payment.getId(),
                payment.getOrderId(),
                status.name(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getGatewayTransactionId(),
                LocalDateTime.now().toString()
        );
        outboxEventRepository.save(new OutboxEvent(eventType, toJson(eventPayload), OutboxStatus.NEW));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể serialize payload outbox", ex);
        }
    }
}
