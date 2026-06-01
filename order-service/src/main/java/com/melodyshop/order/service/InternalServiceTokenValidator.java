package com.melodyshop.order.service;

import com.melodyshop.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalServiceTokenValidator {

    @Value("${internal.service-token}")
    private String expectedToken;

    public void requireValid(String suppliedToken) {
        if (suppliedToken == null || !MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new BadRequestException("Internal service token không hợp lệ");
        }
    }
}
