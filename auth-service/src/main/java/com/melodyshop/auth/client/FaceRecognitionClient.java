package com.melodyshop.auth.client;

import com.melodyshop.auth.client.fallback.FaceRecognitionClientFallbackFactory;
import com.melodyshop.auth.config.feign.AuthFeignConfig;
import com.melodyshop.auth.dto.AiExtractEmbeddingResponse;
import com.melodyshop.auth.dto.AiVerifyFaceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
    name = "face-recognition-service",
    url = "${face-service.url:http://localhost:8093}",
    configuration = AuthFeignConfig.class,
    fallbackFactory = FaceRecognitionClientFallbackFactory.class
)
public interface FaceRecognitionClient {

    @PostMapping("/extract-embedding")
    AiExtractEmbeddingResponse extractEmbedding(@RequestBody Map<String, Object> request);

    @PostMapping("/verify-face")
    AiVerifyFaceResponse verifyFace(@RequestBody Map<String, Object> request);
}
