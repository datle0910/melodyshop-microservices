package com.melodyshop.ai.domain.service;

import com.melodyshop.ai.application.dto.ChatResponse;
import com.melodyshop.ai.domain.model.IntentType;
import com.melodyshop.ai.domain.model.ShoppingContext;

/**
 * Strategy interface for handling different user intents.
 */
public interface IntentHandler {

    /**
     * Returns the IntentType this handler supports.
     */
    IntentType getIntentType();

    /**
     * Processes the user message and returns a structured ChatResponse.
     * @param userId   the user ID (may be null for anonymous)
     * @param message  the user's message
     * @param context  the current shopping context
     * @return structured ChatResponse with embedded data for rich frontend rendering
     */
    ChatResponse handle(String userId, String message, ShoppingContext context);

    /**
     * Whether this handler can process the given message in the given context.
     * Allows handlers to be more selective than just matching IntentType.
     */
    default boolean canHandle(IntentType intentType, String message, ShoppingContext context) {
        return getIntentType() == intentType;
    }
}
