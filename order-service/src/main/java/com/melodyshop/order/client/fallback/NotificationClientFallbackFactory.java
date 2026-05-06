package com.melodyshop.order.client.fallback;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.client.NotificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationClientFallbackFactory implements FallbackFactory<NotificationClient> {

    @Override
    public NotificationClient create(Throwable cause) {
        log.error("NotificationClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new NotificationClientFallback();
    }
}
