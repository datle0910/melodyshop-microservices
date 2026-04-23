package com.melodyshop.engagement.service;

import java.util.List;

public interface PurchaseEligibilityService {
    boolean hasPurchased(String userId, String productId);
    int handleOrderCompletedEvent(String orderId, String userId, List<String> productIds);
}
