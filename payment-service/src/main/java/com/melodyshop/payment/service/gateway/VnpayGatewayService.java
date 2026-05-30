package com.melodyshop.payment.service.gateway;

import com.melodyshop.payment.entity.PaymentTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class VnpayGatewayService implements PaymentGateway {

    @Value("${vnpay.merchant-code:TEST_MERCHANT}")
    private String vnpMerchantCode;

    @Value("${vnpay.secret-key:TEST_SECRET_KEY}")
    private String vnpSecretKey;

    @Value("${vnpay.return-url:http://localhost:5173/checkout}")
    private String vnpReturnUrl;

    @Value("${vnpay.api-url:https://sandbox.vnpayment.vn/apis/docs/hpayinfo/}")
    private String vnpayApiUrl;

    @Override
    public String generateGatewayTransactionId() {
        return "VNPAY_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public String buildPaymentUrl(PaymentTransaction transaction) {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnpMerchantCode);
        params.put("vnp_Amount", String.valueOf(transaction.getAmount().multiply(BigDecimal.valueOf(100)).intValue()));
        params.put("vnp_CurrCode", transaction.getCurrency());
        params.put("vnp_Locale", "vn");
        params.put("vnp_OrderInfo", "MelodyShop Order: " + transaction.getOrderId());
        params.put("vnp_ReturnUrl", vnpReturnUrl);
        params.put("vnp_TxnRef", transaction.getId());
        params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        params.put("vnp_ExpireDate", new SimpleDateFormat("yyyyMMddHHmmss").format(
                new Date(System.currentTimeMillis() + 30 * 60 * 1000)));
        params.put("vnp_OrderType", "other");

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            hashData.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).append("&");
            query.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).append("&");
        }

        String queryString = query.toString();
        if (queryString.endsWith("&")) {
            queryString = queryString.substring(0, queryString.length() - 1);
        }

        String secureHash = hmacSHA512(vnpSecretKey, hashData.toString());
        if (secureHash != null && !secureHash.isEmpty()) {
            query.append("vnp_SecureHash=").append(secureHash);
        }

        return vnpayApiUrl + "?" + queryString + (secureHash != null ? "&vnp_SecureHash=" + secureHash : "");
    }

    @Override
    public String getProviderName() {
        return "VNPAY";
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating HMAC SHA512: {}", e.getMessage());
            return null;
        }
    }
}
