package com.melodyshop.auth.config.feign;

import com.melodyshop.auth.client.NotificationServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationServiceClientFallbackFactory implements FallbackFactory<NotificationServiceClient> {

    @Override
    public NotificationServiceClient create(Throwable cause) {
        log.error("Fallback factory activated for NotificationServiceClient: {}", cause.getMessage());
        return new NotificationServiceClientFallback();
    }
}
