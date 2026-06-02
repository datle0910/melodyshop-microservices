package com.melodyshop.ai.service;

import com.melodyshop.ai.application.dto.ChatRequest;
import com.melodyshop.ai.application.dto.ChatResponse;

public interface GeminiService {
    ChatResponse chat(ChatRequest request);
}
