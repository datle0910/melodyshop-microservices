package com.melodyshop.user.client.fallback;

import com.melodyshop.user.client.AuthClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthClientFallbackFactory implements FallbackFactory<AuthClient> {

    @Override
    public AuthClient create(Throwable cause) {
        log.error("AuthClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new AuthClientFallback();
    }
}
