package com.melodyshop.ai.controller;

import com.melodyshop.ai.application.dto.ChatRequest;
import com.melodyshop.ai.application.dto.ChatResponse;
import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.ai.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getMessage());
        ChatResponse response = geminiService.chat(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.ok("AI service is running"));
    }
}
