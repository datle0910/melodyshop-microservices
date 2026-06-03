package com.melodyshop.payment.service;

import com.melodyshop.payment.config.VietQrProperties;
import com.melodyshop.payment.entity.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VietQrService {

    private final VietQrProperties properties;

    public void populateVietQrDetails(PaymentTransaction payment) {
        String transferContent = "MS " + compactId(payment.getOrderId()) + " " + compactId(payment.getId());
        String qrUrl = UriComponentsBuilder.fromUriString(properties.getQrBaseUrl())
                .queryParam("acc", properties.getAccountNumber())
                .queryParam("bank", properties.getBankCode())
                .queryParam("amount", payment.getAmount().stripTrailingZeros().toPlainString())
                .queryParam("des", transferContent)
                .build()
                .encode()
                .toUriString();

        payment.setBankCode(properties.getBankCode());
        payment.setBankName(properties.getBankName());
        payment.setAccountNumber(properties.getAccountNumber());
        payment.setAccountName(properties.getAccountName());
        payment.setTransferContent(transferContent);
        payment.setQrUrl(qrUrl);
        payment.setExpiredAt(LocalDateTime.now().plusMinutes(properties.getPaymentExpireMinutes()));
    }

    private String compactId(String value) {
        String compact = value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return compact.length() <= 8 ? compact : compact.substring(compact.length() - 8);
    }
}
