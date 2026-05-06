package com.melodyshop.ai.controller;

import com.melodyshop.ai.dto.ChatRequest;
import com.melodyshop.ai.dto.ChatResponse;
import com.melodyshop.ai.service.GeminiService;
import com.melodyshop.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final GeminiService geminiService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getQuestion());
        ChatResponse response = geminiService.chat(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.ok("AI service is running"));
    }
}
