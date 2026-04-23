package com.melodyshop.engagement.service;

import com.melodyshop.engagement.dto.AddWishlistItemRequest;
import com.melodyshop.engagement.dto.WishlistItemResponse;

import java.util.List;

public interface WishlistService {
    WishlistItemResponse addToWishlist(String userId, AddWishlistItemRequest request);
    void removeFromWishlist(String userId, String productId);
    List<WishlistItemResponse> getWishlist(String userId);
}
