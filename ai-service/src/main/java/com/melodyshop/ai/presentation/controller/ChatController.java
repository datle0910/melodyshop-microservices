package com.melodyshop.ai.presentation.controller;

import com.melodyshop.ai.application.dto.ChatRequest;
import com.melodyshop.ai.application.dto.ChatResponse;
import com.melodyshop.ai.application.usecase.ChatInteractionUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatInteractionUseCase chatUseCase;

    public ChatController(ChatInteractionUseCase chatUseCase) {
        this.chatUseCase = chatUseCase;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("Chat request - userId: {}, message: {}",
                request.getUserId(),
                request.getMessage() != null ? request.getMessage().substring(0, Math.min(50, request.getMessage().length())) + "..." : "null");

        ChatResponse response = chatUseCase.processUserMessage(request.getUserId(), request.getMessage());
        return ResponseEntity.ok(response);
    }
}
