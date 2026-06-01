package com.melodyshop.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vietqr")
public class VietQrProperties {
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String qrBaseUrl;
    private long paymentExpireMinutes = 30;
}
