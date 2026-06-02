package com.melodyshop.ai.infrastructure.redis;

import com.melodyshop.ai.domain.model.ProductSummary;
import com.melodyshop.ai.domain.model.ShoppingContext;
import com.melodyshop.ai.domain.repository.ContextStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RedisContextStore implements ContextStore {

    private static final Logger log = LoggerFactory.getLogger(RedisContextStore.class);

    // In-memory store keyed by userId (since Redis is excluded)
    private final Map<String, ShoppingContext> contexts = new ConcurrentHashMap<>();

    @Override
    public ShoppingContext getContext(String userId) {
        ShoppingContext ctx = contexts.get(userId);
        if (ctx == null) {
            ctx = new ShoppingContext();
            ctx.setUserId(userId);
            ctx.setSessionId("session-" + userId);
            ctx.setLastMentionedProducts(new ArrayList<>());
            ctx.setLastUpdated(LocalDateTime.now());
        }
        return ctx;
    }

    @Override
    public void saveContext(String userId, ShoppingContext context) {
        log.debug("Saving context for user: {}", userId);
        contexts.put(userId, context);
    }

    @Override
    public void updateLastViewedProduct(String userId, ProductSummary product) {
        log.debug("Updating last viewed product for user {}: {}", userId, product != null ? product.name() : "null");
        ShoppingContext ctx = getContext(userId);
        ctx.setLastViewedProduct(product);
        ctx.setLastAction("viewed_product");
        ctx.setLastUpdated(LocalDateTime.now());
        contexts.put(userId, ctx);
    }

    @Override
    public void updateLastAction(String userId, String action) {
        log.debug("Updating last action for user {}: {}", userId, action);
        ShoppingContext ctx = getContext(userId);
        ctx.setLastAction(action);
        ctx.setLastUpdated(LocalDateTime.now());
        contexts.put(userId, ctx);
    }

    @Override
    public void updateLastCategory(String userId, String category) {
        log.debug("Updating last category for user {}: {}", userId, category);
        ShoppingContext ctx = getContext(userId);
        ctx.setLastCategory(category);
        ctx.setLastUpdated(LocalDateTime.now());
        contexts.put(userId, ctx);
    }

    /**
     * Adds a product to the last-mentioned-products list for context tracking.
     */
    public void addMentionedProduct(String userId, ProductSummary product) {
        if (product == null) return;
        ShoppingContext ctx = getContext(userId);
        List<ProductSummary> mentioned = ctx.getLastMentionedProducts();
        if (mentioned == null) {
            mentioned = new ArrayList<>();
            ctx.setLastMentionedProducts(mentioned);
        }
        // Keep only last 5 mentioned
        if (mentioned.size() >= 5) {
            mentioned.remove(0);
        }
        mentioned.add(product);
        ctx.setLastUpdated(LocalDateTime.now());
        contexts.put(userId, ctx);
    }

    /**
     * Clears context for a user (e.g., on logout or new session).
     */
    public void clearContext(String userId) {
        log.info("Clearing context for user: {}", userId);
        contexts.remove(userId);
    }
}
