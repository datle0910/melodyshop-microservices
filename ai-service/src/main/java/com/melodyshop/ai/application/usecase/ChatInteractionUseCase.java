package com.melodyshop.ai.application.usecase;

import com.melodyshop.ai.application.dto.ChatResponse;
import com.melodyshop.ai.application.dto.ChatResponse.ProductInfoDto;
import com.melodyshop.ai.application.handler.*;
import com.melodyshop.ai.domain.model.IntentType;
import com.melodyshop.ai.domain.model.ProductSummary;
import com.melodyshop.ai.domain.model.ShoppingContext;
import com.melodyshop.ai.domain.repository.ContextStore;
import com.melodyshop.ai.domain.service.IntentClassifier;
import com.melodyshop.ai.domain.service.IntentHandler;
import com.melodyshop.ai.infrastructure.langchain.SimpleChatAgent;
import com.melodyshop.ai.infrastructure.redis.RedisContextStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatInteractionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChatInteractionUseCase.class);

    private final List<IntentHandler> handlers;
    private final IntentClassifier classifier;
    private final SimpleChatAgent chatAgent;
    private final RedisContextStore contextStore;

    public ChatInteractionUseCase(
            List<IntentHandler> handlers,
            IntentClassifier classifier,
            SimpleChatAgent chatAgent,
            RedisContextStore contextStore
    ) {
        this.handlers = handlers;
        this.classifier = classifier;
        this.chatAgent = chatAgent;
        this.contextStore = contextStore;

        // Sort so more specific handlers get priority
        // We'll evaluate per-message in handle()
    }

    public ChatResponse processUserMessage(String userId, String message) {
        String sessionId = userId != null ? userId : "anonymous";

        try {
            ShoppingContext context = contextStore.getContext(sessionId);
            IntentType intent = classifier.classify(message, context);

            log.info("Chat message from {} - intent: {}, message: {}",
                sessionId, intent, message.length() > 50 ? message.substring(0, 50) + "..." : message);

            // Find the best handler for this intent
            IntentHandler handler = findHandler(intent, message, context);

            if (handler != null) {
                log.debug("Using handler: {}", handler.getClass().getSimpleName());
                ChatResponse response = handler.handle(userId, message, context);

                // Update context after successful handling
                updateContextAfterResponse(sessionId, message, intent, response);

                return response;
            }

            // Fallback: use general chat
            log.debug("No handler matched, falling back to general chat");
            return handleGeneralChat(userId, message, context);

        } catch (Exception e) {
            log.error("Error processing chat message for user {}: {}", sessionId, e.getMessage(), e);
            return ChatResponse.error("Xin loi, da xay ra loi khi xu ly yeu cau cua ban. Vui long thu lai sau.");
        }
    }

    private IntentHandler findHandler(IntentType intent, String message, ShoppingContext context) {
        // Find all handlers that can handle this intent
        List<IntentHandler> candidates = handlers.stream()
            .filter(h -> h.canHandle(intent, message, context))
            .toList();

        if (candidates.isEmpty()) {
            // Fall back to general handler
            return handlers.stream()
                .filter(h -> h.getIntentType() == IntentType.GENERAL)
                .findFirst()
                .orElse(null);
        }

        // Prefer specific handlers over general
        // ORDER: ADD_TO_CART > PRODUCT_DETAIL > STOCK_CHECK > PRODUCT_LIST > CATEGORY_LIST > GENERAL
        Map<IntentType, Integer> priority = Map.of(
            IntentType.ADD_TO_CART, 1,
            IntentType.PRODUCT_DETAIL, 2,
            IntentType.STOCK_CHECK, 3,
            IntentType.PRODUCT_LIST, 4,
            IntentType.CATEGORY_LIST, 5,
            IntentType.PRODUCT_SEARCH, 6,
            IntentType.GENERAL, 100
        );

        return candidates.stream()
            .min(Comparator.comparingInt(h -> priority.getOrDefault(h.getIntentType(), 50)))
            .orElse(null);
    }

    private void updateContextAfterResponse(String userId, String message, IntentType intent, ChatResponse response) {
        try {
            // Update category from message
            updateCategoryFromMessage(userId, message);

            // If response has products, add to context
            if (response.getProducts() != null && !response.getProducts().isEmpty()) {
                for (ProductInfoDto p : response.getProducts()) {
                    ProductSummary ps = new ProductSummary(p.getId(), p.getName(), p.getPrice(), p.getInStock());
                    contextStore.addMentionedProduct(userId, ps);
                }
            }

            // If response has product detail, set as last viewed
            if (response.getProduct() != null) {
                ChatResponse.ProductDetailDto p = response.getProduct();
                ProductSummary ps = new ProductSummary(p.getId(), p.getName(), p.getBasePrice(), p.getInStock());
                contextStore.updateLastViewedProduct(userId, ps);
                contextStore.addMentionedProduct(userId, ps);
            }

            // Track last action
            String action = intent.name().toLowerCase();
            contextStore.updateLastAction(userId, action);

        } catch (Exception e) {
            log.warn("Failed to update context: {}", e.getMessage());
        }
    }

    private void updateCategoryFromMessage(String userId, String message) {
        String[] categories = {"guitar", "piano", "drum", "violin", "amplifier", "microphone", "ukulele", "accessories"};
        String lower = message.toLowerCase();
        for (String cat : categories) {
            if (lower.contains(cat)) {
                contextStore.updateLastCategory(userId, cat);
                break;
            }
        }
    }

    private ChatResponse handleGeneralChat(String userId, String message, ShoppingContext context) {
        try {
            String systemPrompt = buildSystemMessage(context);
            String responseText = chatAgent.chat(systemPrompt, message);
            return ChatResponse.text(responseText);
        } catch (Exception e) {
            log.error("General chat error: {}", e.getMessage(), e);
            return ChatResponse.text("Xin loi, da xay ra loi. Vui long thu lai.");
        }
    }

    private String buildSystemMessage(ShoppingContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Ban la tro ly mua sam thong minh cua MelodyShop - cua hang nhac cu truc tuyen hang dau Viet Nam.\n\n");
        prompt.append("## NGUYEN TAC HOAT DONG\n");
        prompt.append("1. Tra loi cac cau hoi ve san pham nhac cu\n");
        prompt.append("2. Chi tra loi ve cac san pham trong he thong MelodyShop\n");
        prompt.append("3. Neu khong biet, noi 'Toi chua co thong tin ve dieu nay'\n");
        prompt.append("4. Chi dung tieng Viet\n\n");

        prompt.append("## CAC DANH MUC SAN PHAM\n");
        prompt.append("- Guitar (acoustic, electric, classical)\n");
        prompt.append("- Piano (grand, upright, digital)\n");
        prompt.append("- Drum (acoustic, electronic)\n");
        prompt.append("- Violin\n");
        prompt.append("- Amplifier\n");
        prompt.append("- Microphone\n");
        prompt.append("- Ukulele\n");
        prompt.append("- Accessories (pick, strap, cable, etc.)\n\n");

        prompt.append("## VI DU TRA LOI\n");
        prompt.append("- 'Cho xem guitar' -> 'MelodyShop co nhieu loai guitar: acoustic Yamaha F310, electric Fender Stratocaster, classical Valencia VC-204... Ban muon tim hieu them ve loai nao?'\n");
        prompt.append("- 'Gia piano' -> 'Gia piano tai MelodyShop tu 15 trieu (digital) den 500 trieu (grand). Ban quan tam loai nao?'\n");
        prompt.append("- 'Toi muon mua drum' -> 'Chung toi co acoustic drum Roland TD-17 va electronic Yamaha DTX6. Ban thich loai nao hon?'\n\n");

        if (context != null) {
            if (context.getLastCategory() != null) {
                prompt.append("## NGU CANG GAN NHAT\n");
                prompt.append("- Danh muc dang xem: ").append(context.getLastCategory()).append("\n");
            }
            if (context.getLastViewedProduct() != null) {
                prompt.append("- San pham dang xem: ").append(context.getLastViewedProduct().name()).append("\n");
            }
            if (context.getLastMentionedProducts() != null && !context.getLastMentionedProducts().isEmpty()) {
                prompt.append("- San pham da de cap: ");
                context.getLastMentionedProducts().forEach(p -> prompt.append(p.name()).append(", "));
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        return prompt.toString();
    }
}
