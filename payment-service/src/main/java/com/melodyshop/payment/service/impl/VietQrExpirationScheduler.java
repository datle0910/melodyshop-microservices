package com.melodyshop.payment.service.impl;

import com.melodyshop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VietQrExpirationScheduler {

    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "${vietqr.expire-scan-delay-ms:60000}")
    public void expireDuePayments() {
        for (String paymentId : paymentService.findExpiredVietQrPaymentIds()) {
            try {
                paymentService.expirePayment(paymentId);
            } catch (Exception ex) {
                log.error("Failed to expire VietQR payment {}: {}", paymentId, ex.getMessage());
            }
        }
    }
}
