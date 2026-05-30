package com.melodyshop.payment.service.gateway;

import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.payment.entity.PaymentTransaction;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class StripeGatewayService implements PaymentGateway {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @Override
    public String generateGatewayTransactionId() {
        return "STRIPE_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public String buildPaymentUrl(PaymentTransaction transaction) {
        Stripe.apiKey = secretKey;
        
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .setClientReferenceId(transaction.getOrderId())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(transaction.getCurrency().toLowerCase())
                                                    .setUnitAmount(transaction.getAmount().longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Đơn hàng #" + transaction.getOrderId())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            
            // We can update gateway transaction id to Stripe Session ID here
            // but the interface doesn't allow returning a modified transaction easily.
            // Stripe will return the url
            return session.getUrl();
        } catch (Exception e) {
            log.error("Stripe Error: {}", e.getMessage());
            throw new BadRequestException("Không thể tạo phiên thanh toán Stripe: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "STRIPE";
    }
}
