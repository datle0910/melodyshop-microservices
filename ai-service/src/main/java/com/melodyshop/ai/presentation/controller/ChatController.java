package com.melodyshop.ai.presentation.controller;

import com.melodyshop.ai.application.dto.ChatRequest;
import com.melodyshop.ai.application.dto.ChatResponse;
import com.melodyshop.ai.application.usecase.ChatInteractionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatInteractionUseCase chatUseCase;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatResponse response = chatUseCase.processUserMessage(request.getUserId(), request.getMessage());
        return ResponseEntity.ok(response);
    }
}
