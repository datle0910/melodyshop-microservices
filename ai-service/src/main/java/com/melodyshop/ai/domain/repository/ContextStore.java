package com.melodyshop.ai.domain.repository;

import com.melodyshop.ai.domain.model.ShoppingContext;

public interface ContextStore {
    ShoppingContext getContext(String userId);
    void saveContext(String userId, ShoppingContext context);
    void updateLastViewedProduct(String userId, com.melodyshop.ai.domain.model.ProductSummary product);
    void updateLastAction(String userId, String action);
}
