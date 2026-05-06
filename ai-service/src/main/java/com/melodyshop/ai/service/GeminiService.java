package com.melodyshop.ai.service;

import com.melodyshop.ai.dto.ChatRequest;
import com.melodyshop.ai.dto.ChatResponse;

public interface GeminiService {

    ChatResponse chat(ChatRequest request);
}
