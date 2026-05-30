package com.melodyshop.ai.infrastructure.redis;

import com.melodyshop.ai.domain.model.ProductSummary;
import com.melodyshop.ai.domain.model.ShoppingContext;
import com.melodyshop.ai.domain.repository.ContextStore;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

// Simplified stub for the skeleton
@Repository
public class RedisContextStore implements ContextStore {

    @Override
    public ShoppingContext getContext(String userId) {
        // In real impl: fetch from RedisTemplate
        return new ShoppingContext(userId, "session-" + userId, null, null, null, null, null, LocalDateTime.now());
    }

    @Override
    public void saveContext(String userId, ShoppingContext context) {
        // In real impl: save to RedisTemplate
    }

    @Override
    public void updateLastViewedProduct(String userId, ProductSummary product) {
        // In real impl: fetch, update, save
    }

    @Override
    public void updateLastAction(String userId, String action) {
         // In real impl: fetch, update, save
    }
}
