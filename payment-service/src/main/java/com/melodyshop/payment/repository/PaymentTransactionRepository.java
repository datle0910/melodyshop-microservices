package com.melodyshop.payment.repository;

import com.melodyshop.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentTransaction> findByGatewayTransactionId(String gatewayTransactionId);
    Optional<PaymentTransaction> findByActivePaymentKey(String activePaymentKey);
    boolean existsBySuccessfulPaymentKey(String successfulPaymentKey);
}
