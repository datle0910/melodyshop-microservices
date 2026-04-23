package com.melodyshop.engagement.service.impl;

import com.melodyshop.engagement.dto.AddWishlistItemRequest;
import com.melodyshop.engagement.dto.WishlistItemResponse;
import com.melodyshop.engagement.entity.WishlistItem;
import com.melodyshop.engagement.repository.WishlistItemRepository;
import com.melodyshop.engagement.service.WishlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistServiceImpl implements WishlistService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WishlistServiceImpl.class);

    private final WishlistItemRepository wishlistItemRepository;

    public WishlistServiceImpl(WishlistItemRepository wishlistItemRepository) {
        this.wishlistItemRepository = wishlistItemRepository;
    }

    @Override
    @Transactional
    public WishlistItemResponse addToWishlist(String userId, AddWishlistItemRequest request) {
        String normalizedUserId = userId.trim();
        String normalizedProductId = request.getProductId().trim();

        WishlistItem existingItem = wishlistItemRepository.findByUserIdAndProductId(normalizedUserId, normalizedProductId)
                .orElse(null);
        if (existingItem != null) {
            LOGGER.info("Wishlist item already exists, ignore duplicate userId={} productId={}",
                    normalizedUserId, normalizedProductId);
            return toResponse(existingItem);
        }

        WishlistItem wishlistItem = new WishlistItem();
        wishlistItem.setUserId(normalizedUserId);
        wishlistItem.setProductId(normalizedProductId);
        wishlistItem = wishlistItemRepository.save(wishlistItem);

        LOGGER.info("Added wishlist item id={} userId={} productId={}",
                wishlistItem.getId(), normalizedUserId, normalizedProductId);
        return toResponse(wishlistItem);
    }

    @Override
    @Transactional
    public void removeFromWishlist(String userId, String productId) {
        String normalizedUserId = userId.trim();
        String normalizedProductId = productId.trim();

        wishlistItemRepository.findByUserIdAndProductId(normalizedUserId, normalizedProductId)
                .ifPresentOrElse(item -> {
                    wishlistItemRepository.delete(item);
                    LOGGER.info("Removed wishlist item id={} userId={} productId={}",
                            item.getId(), normalizedUserId, normalizedProductId);
                }, () -> LOGGER.info("Wishlist item not found, ignore remove userId={} productId={}",
                        normalizedUserId, normalizedProductId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(String userId) {
        String normalizedUserId = userId.trim();
        return wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(normalizedUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        return new WishlistItemResponse(
                item.getId(),
                item.getUserId(),
                item.getProductId(),
                item.getCreatedAt()
        );
    }
}
