package com.melodyshop.ai.infrastructure.langchain;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

@Component
public class SimpleChatAgent {

    private final ChatLanguageModel chatModel;

    public SimpleChatAgent(ChatLanguageModel chatLanguageModel) {
        this.chatModel = chatLanguageModel;
    }

    public String chat(String systemPrompt, String userMessage) {
        String fullPrompt = systemPrompt + "\n\nUser: " + userMessage + "\n\nAssistant:";
        return chatModel.generate(fullPrompt);
    }
}
