package com.melodyshop.user.config.feign;

import com.melodyshop.user.client.MediaServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {

    @Override
    public MediaServiceClient create(Throwable cause) {
        log.error("Fallback factory activated for MediaServiceClient: {}", cause.getMessage());
        return new MediaServiceClientFallback();
    }
}
