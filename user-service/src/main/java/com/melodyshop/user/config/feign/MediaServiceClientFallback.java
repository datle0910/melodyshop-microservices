package com.melodyshop.user.config.feign;

import com.melodyshop.user.client.MediaServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class MediaServiceClientFallback implements MediaServiceClient {

    @Override
    public Map<String, Object> uploadFile(String type, MultipartFile file) {
        log.warn("Fallback activated: Media service temporarily unavailable, returning empty response for type={}", type);
        return new HashMap<>();
    }
}
