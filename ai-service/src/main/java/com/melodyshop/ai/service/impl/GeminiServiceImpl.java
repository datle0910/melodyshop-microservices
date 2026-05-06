package com.melodyshop.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyshop.ai.config.GeminiProperties;
import com.melodyshop.ai.dto.ChatRequest;
import com.melodyshop.ai.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements com.melodyshop.ai.service.GeminiService {

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            Bạn là một trợ lý AI thông minh của MelodyShop - một trang web bán nhạc cụ trực tuyến.
            
            MelodyShop là một hệ thống thương mại điện tử microservices bao gồm các dịch vụ:
            - Auth Service: Xác thực người dùng, đăng nhập, đăng ký, quản lý JWT tokens
            - User Service: Quản lý thông tin người dùng, hồ sơ
            - Product Service: Quản lý sản phẩm, danh mục, thương hiệu nhạc cụ
            - Inventory Service: Quản lý tồn kho, kho hàng
            - Order Service: Xử lý đơn hàng, theo dõi trạng thái
            - Cart Service: Quản lý giỏ hàng
            - Payment Service: Xử lý thanh toán
            - Notification Service: Gửi email thông báo
            - Media Service: Upload và quản lý hình ảnh (Cloudinary)
            - Search Service: Tìm kiếm sản phẩm (Elasticsearch)
            - Customer Engagement Service: Đánh giá sản phẩm, wishlist
            
            Công nghệ sử dụng:
            - Backend: Spring Boot 3.2.5, Java 21
            - Database: MariaDB
            - Message Queue: RabbitMQ
            - API Gateway: Spring Cloud Gateway
            - Service Discovery: Eureka
            
            Hãy trả lời các câu hỏi liên quan đến MelodyShop một cách hữu ích và chi tiết.
            Nếu câu hỏi không liên quan đến MelodyShop hoặc nhạc cụ, hãy lịch sự từ chối và gợi ý câu hỏi phù hợp.
            """;

    @Override
    public ChatResponse chat(ChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        try {
            String answer = callGeminiApi(request.getQuestion());
            return ChatResponse.builder()
                    .answer(answer)
                    .conversationId(conversationId)
                    .model(geminiProperties.getModel())
                    .timestamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            return ChatResponse.builder()
                    .answer("Xin lỗi, tôi đang gặp sự cố khi xử lý câu hỏi của bạn. Vui lòng thử lại sau.")
                    .conversationId(conversationId)
                    .model(geminiProperties.getModel())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    private String callGeminiApi(String question) throws Exception {
        String apiUrl = String.format("%s/v1beta/models/%s:generateContent?key=%s",
                geminiProperties.getBaseUrl(),
                geminiProperties.getModel(),
                geminiProperties.getApiKey());

        String requestBody = buildRequestBody(question);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(java.time.Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return extractTextFromResponse(response.body());
        } else {
            log.error("Gemini API error: Status={}, Body={}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini API returned status: " + response.statusCode());
        }
    }

    private String buildRequestBody(String question) {
        try {
            Map<String, Object> systemContent = Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", SYSTEM_PROMPT))
            );
            Map<String, Object> userContent = Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", question))
            );
            Map<String, Object> contents = Map.of("contents", new Object[]{systemContent, userContent});
            Map<String, Object> generationConfig = Map.of(
                    "maxOutputTokens", geminiProperties.getMaxOutputTokens(),
                    "temperature", geminiProperties.getTemperature()
            );
            Map<String, Object> requestMap = Map.of(
                    "contents", new Object[]{
                            Map.of("role", "model", "parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                            Map.of("role", "user", "parts", List.of(Map.of("text", question)))
                    },
                    "generationConfig", generationConfig
            );
            return objectMapper.writeValueAsString(requestMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build request body", e);
        }
    }

    private String extractTextFromResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                return parts.get(0).path("text").asText();
            }
        }
        throw new RuntimeException("Unexpected response format from Gemini API");
    }
}
