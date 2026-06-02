package com.melodyshop.ai.domain.repository;

import com.melodyshop.ai.domain.model.ShoppingContext;
import com.melodyshop.ai.domain.model.ProductSummary;

public interface ContextStore {
    ShoppingContext getContext(String userId);
    void saveContext(String userId, ShoppingContext context);
    void updateLastViewedProduct(String userId, ProductSummary product);
    void addMentionedProduct(String userId, ProductSummary product);
    void updateLastAction(String userId, String action);
    void updateLastCategory(String userId, String category);
}
