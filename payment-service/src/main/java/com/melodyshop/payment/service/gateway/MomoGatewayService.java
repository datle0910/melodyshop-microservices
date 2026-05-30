package com.melodyshop.payment.service.gateway;

import com.melodyshop.payment.entity.PaymentTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class MomoGatewayService implements PaymentGateway {

    @Value("${momo.partner-code:MOMO_PARTNER_CODE}")
    private String partnerCode;

    @Value("${momo.access-key:MOMO_ACCESS_KEY}")
    private String accessKey;

    @Value("${momo.secret-key:MOMO_SECRET_KEY}")
    private String secretKey;

    @Value("${momo.return-url:http://localhost:5173/checkout}")
    private String returnUrl;

    @Value("${momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String momoEndpoint;

    @Override
    public String generateGatewayTransactionId() {
        return "MOMO_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public String buildPaymentUrl(PaymentTransaction transaction) {
        String orderId = transaction.getId();
        String requestId = UUID.randomUUID().toString();
        long amount = transaction.getAmount().longValue();
        String orderInfo = "MelodyShop Order: " + transaction.getOrderId();
        String redirectUrl = returnUrl;
        String ipnUrl = returnUrl + "/ipn";

        Map<String, String> params = new LinkedHashMap<>();
        params.put("partnerCode", partnerCode);
        params.put("accessKey", accessKey);
        params.put("requestId", requestId);
        params.put("amount", String.valueOf(amount));
        params.put("orderId", orderId);
        params.put("orderInfo", orderInfo);
        params.put("redirectUrl", redirectUrl);
        params.put("ipnUrl", ipnUrl);
        params.put("requestType", "payWithATM");
        params.put("extraData", "");
        params.put("lang", "vi");

        String rawSignature = buildMomoSignature(params);
        params.put("signature", rawSignature);

        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (query.length() > 0) query.append("&");
            query.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        return momoEndpoint + "?" + query.toString();
    }

    @Override
    public String getProviderName() {
        return "MOMO";
    }

    private String buildMomoSignature(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!entry.getKey().equals("signature")) {
                if (sb.length() > 0) sb.append("&");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating MoMo signature: {}", e.getMessage());
            return "";
        }
    }
}
