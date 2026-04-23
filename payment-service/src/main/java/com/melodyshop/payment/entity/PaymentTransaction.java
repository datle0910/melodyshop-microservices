package com.melodyshop.payment.entity;

import com.melodyshop.common.entity.BaseEntity;
import com.melodyshop.payment.enums.PaymentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
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

    public void transitionTo(PaymentStatus newStatus) {
        this.status = newStatus;
        switch (newStatus) {
            case PENDING -> {
                this.activePaymentKey = this.orderId;
                this.successfulPaymentKey = null;
            }
            case SUCCESS -> {
                this.activePaymentKey = null;
                this.successfulPaymentKey = this.orderId;
            }
            case FAILED, EXPIRED -> {
                this.activePaymentKey = null;
                this.successfulPaymentKey = null;
            }
        }
    }
}
