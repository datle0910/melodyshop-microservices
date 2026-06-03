package com.melodyshop.payment.entity;

import com.melodyshop.common.entity.BaseEntity;
import com.melodyshop.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "payment_transaction", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_idempotency_key", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_payment_gateway_transaction_id", columnNames = "gateway_transaction_id"),
        @UniqueConstraint(name = "uk_payment_active_payment_key", columnNames = "active_payment_key"),
        @UniqueConstraint(name = "uk_payment_successful_payment_key", columnNames = "successful_payment_key")
}, indexes = {
        @Index(name = "idx_payment_order_status", columnList = "order_id, status"),
        @Index(name = "idx_payment_gateway_tx", columnList = "gateway_transaction_id")
})
public class PaymentTransaction extends BaseEntity {

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "gateway_transaction_id", nullable = false, length = 100)
    private String gatewayTransactionId;

    @Column(name = "active_payment_key", length = 64)
    private String activePaymentKey;

    @Column(name = "successful_payment_key", length = 64)
    private String successfulPaymentKey;

    @Column(length = 20)
    private String provider;

    @Column(length = 30)
    private String method;

    @Column(name = "bank_code", length = 50)
    private String bankCode;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_name", length = 150)
    private String accountName;

    @Column(name = "transfer_content", length = 150)
    private String transferContent;

    @Column(name = "qr_code", columnDefinition = "LONGTEXT")
    private String qrCode;

    @Column(name = "qr_url", length = 1000)
    private String qrUrl;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "confirmed_by", length = 36)
    private String confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Version
    private Long version;

    public PaymentTransaction() {
    }

    public PaymentTransaction(String orderId,
                              BigDecimal amount,
                              String currency,
                              PaymentStatus status,
                              String idempotencyKey,
                              String gatewayTransactionId,
                              String activePaymentKey,
                              String successfulPaymentKey) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.gatewayTransactionId = gatewayTransactionId;
        this.activePaymentKey = activePaymentKey;
        this.successfulPaymentKey = successfulPaymentKey;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getActivePaymentKey() {
        return activePaymentKey;
    }

    public void setActivePaymentKey(String activePaymentKey) {
        this.activePaymentKey = activePaymentKey;
    }

    public String getSuccessfulPaymentKey() {
        return successfulPaymentKey;
    }

    public void setSuccessfulPaymentKey(String successfulPaymentKey) {
        this.successfulPaymentKey = successfulPaymentKey;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void transitionTo(PaymentStatus newStatus) {
        this.status = newStatus;
        switch (newStatus) {
            case PENDING, WAITING_CONFIRMATION -> {
                this.activePaymentKey = this.orderId;
                this.successfulPaymentKey = null;
            }
            case SUCCESS -> {
                this.activePaymentKey = null;
                this.successfulPaymentKey = this.orderId;
            }
            case FAILED, CANCELLED, EXPIRED -> {
                this.activePaymentKey = null;
                this.successfulPaymentKey = null;
            }
        }
    }
}
