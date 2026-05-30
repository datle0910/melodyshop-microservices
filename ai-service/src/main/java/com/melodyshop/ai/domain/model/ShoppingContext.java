package com.melodyshop.ai.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record ShoppingContext(
    String userId,
    String sessionId,
    ProductSummary lastViewedProduct,
    List<ProductSummary> lastMentionedProducts,
    String lastCategory,
    CartSummary currentCart,
    String lastAction,
    LocalDateTime lastUpdated
) {}
