package com.melodyshop.ai.service.impl;

import com.melodyshop.ai.application.dto.ChatRequest;
import com.melodyshop.ai.application.dto.ChatResponse;
import com.melodyshop.ai.application.usecase.ChatInteractionUseCase;
import com.melodyshop.ai.domain.model.ProductSummary;
import com.melodyshop.ai.domain.repository.ContextStore;
import com.melodyshop.ai.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GeminiServiceImpl implements GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiServiceImpl.class);

    private final ChatInteractionUseCase chatInteractionUseCase;
    private final ContextStore contextStore;

    public GeminiServiceImpl(
            ChatInteractionUseCase chatInteractionUseCase,
            ContextStore contextStore
    ) {
        this.chatInteractionUseCase = chatInteractionUseCase;
        this.contextStore = contextStore;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String message = request.getMessage();
        String userId = request.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            userId = "anonymous";
        }

        log.info("Processing chat request - userId: {}, message: {}",
                userId,
                message != null && message.length() > 50 ? message.substring(0, 50) + "..." : message);

        // If frontend sends product context, inject it into context store
        if (request.getProductContext() != null) {
            ChatRequest.ProductContextDto ctx = request.getProductContext();
            if (ctx.getId() != null && ctx.getName() != null) {
                ProductSummary ps = new ProductSummary(
                    ctx.getId(),
                    ctx.getName(),
                    ctx.getPrice(),
                    ctx.getInStock()
                );
                contextStore.updateLastViewedProduct(userId, ps);
                contextStore.addMentionedProduct(userId, ps);
                log.info("Injected product context from frontend - userId: {}, product: {}", userId, ps.name());
            }
        }

        return chatInteractionUseCase.processUserMessage(userId, message);
    }
}
