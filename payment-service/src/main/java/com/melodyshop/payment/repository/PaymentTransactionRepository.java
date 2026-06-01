package com.melodyshop.payment.repository;

import com.melodyshop.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentTransaction> findByGatewayTransactionId(String gatewayTransactionId);
    Optional<PaymentTransaction> findByActivePaymentKey(String activePaymentKey);
    boolean existsBySuccessfulPaymentKey(String successfulPaymentKey);
    List<PaymentTransaction> findByProviderAndStatusInOrderByCreatedAtAsc(String provider, List<com.melodyshop.payment.enums.PaymentStatus> statuses);
    List<PaymentTransaction> findByProviderAndStatusInAndExpiredAtBefore(
            String provider,
            List<com.melodyshop.payment.enums.PaymentStatus> statuses,
            LocalDateTime expiredAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentTransaction p WHERE p.id = :id")
    Optional<PaymentTransaction> findByIdForUpdate(@Param("id") String id);
}
