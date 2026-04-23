package com.melodyshop.engagement.service.impl;

import com.melodyshop.engagement.entity.PurchasedProduct;
import com.melodyshop.engagement.repository.PurchasedProductRepository;
import com.melodyshop.engagement.service.PurchaseEligibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class PurchaseEligibilityServiceImpl implements PurchaseEligibilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PurchaseEligibilityServiceImpl.class);

    private final PurchasedProductRepository purchasedProductRepository;

    @Value("${engagement.review.assume-purchased:true}")
    private boolean assumePurchased;

    public PurchaseEligibilityServiceImpl(PurchasedProductRepository purchasedProductRepository) {
        this.purchasedProductRepository = purchasedProductRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPurchased(String userId, String productId) {
        String normalizedUserId = normalize(userId);
        String normalizedProductId = normalize(productId);

        if (assumePurchased) {
            LOGGER.info("Purchase validation in mock mode, allow review for userId={} productId={}",
                    normalizedUserId, normalizedProductId);
            return true;
        }

        boolean purchased = purchasedProductRepository.existsByUserIdAndProductId(normalizedUserId, normalizedProductId);
        LOGGER.info("Purchase validation via purchased_products for userId={} productId={} result={}",
                normalizedUserId, normalizedProductId, purchased);
        return purchased;
    }

    @Override
    @Transactional
    public int handleOrderCompletedEvent(String orderId, String userId, List<String> productIds) {
        String normalizedUserId = normalize(userId);
        Set<String> uniqueProductIds = new LinkedHashSet<>();
        for (String productId : productIds) {
            if (StringUtils.hasText(productId)) {
                uniqueProductIds.add(productId.trim());
            }
        }

        int inserted = 0;
        for (String productId : uniqueProductIds) {
            if (purchasedProductRepository.existsByUserIdAndProductId(normalizedUserId, productId)) {
                LOGGER.info("Purchased product already exists for userId={} productId={}, skip", normalizedUserId, productId);
                continue;
            }

            PurchasedProduct purchasedProduct = new PurchasedProduct();
            purchasedProduct.setUserId(normalizedUserId);
            purchasedProduct.setProductId(productId);
            purchasedProductRepository.save(purchasedProduct);
            inserted++;
        }

        LOGGER.info("Handled order_completed event orderId={} userId={} insertedProducts={}",
                orderId, normalizedUserId, inserted);
        return inserted;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
