package com.melodyshop.auth.client.fallback;

import com.melodyshop.auth.client.FaceRecognitionClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FaceRecognitionClientFallbackFactory implements FallbackFactory<FaceRecognitionClient> {

    @Override
    public FaceRecognitionClient create(Throwable cause) {
        log.error("FaceRecognitionClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new FaceRecognitionClientFallback();
    }
}
