package com.melodyshop.ai.infrastructure.langchain;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface MelodyShopAiAgent {
    
    @SystemMessage("{{systemPrompt}}")
    String chat(
        @MemoryId String memoryId, 
        @V("systemPrompt") String systemPrompt, 
        @UserMessage String userMessage
    );
}
