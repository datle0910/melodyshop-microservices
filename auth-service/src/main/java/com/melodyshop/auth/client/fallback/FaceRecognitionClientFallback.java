package com.melodyshop.auth.client.fallback;

import com.melodyshop.auth.client.FaceRecognitionClient;
import com.melodyshop.auth.dto.AiExtractEmbeddingResponse;
import com.melodyshop.auth.dto.AiVerifyFaceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class FaceRecognitionClientFallback implements FaceRecognitionClient {

    @Override
    public AiExtractEmbeddingResponse extractEmbedding(Map<String, Object> request) {
        log.error("FaceRecognitionClient fallback: extract-embedding failed. Service might be unavailable.");
        return AiExtractEmbeddingResponse.builder()
                .success(false)
                .message("Face recognition service is temporarily unavailable. Please try again later.")
                .build();
    }

    @Override
    public AiVerifyFaceResponse verifyFace(Map<String, Object> request) {
        log.error("FaceRecognitionClient fallback: verify-face failed. Service might be unavailable.");
        return AiVerifyFaceResponse.builder()
                .success(false)
                .message("Face recognition service is temporarily unavailable. Please try again later.")
                .build();
    }
}
