package com.melodyshop.cart.service;

import com.melodyshop.cart.dto.AddToCartRequest;
import com.melodyshop.cart.dto.CartDTO;
import com.melodyshop.cart.dto.CartItemDTO;
import com.melodyshop.cart.dto.UpdateCartItemRequest;

public interface CartService {
    CartDTO getCartByUserId(String userId);
    CartItemDTO addToCart(String userId, AddToCartRequest request);
    CartItemDTO updateCartItem(String userId, String itemId, UpdateCartItemRequest request);
    void removeCartItem(String userId, String itemId);
    void clearCart(String userId);
    CartDTO mergeCart(String userId, java.util.List<AddToCartRequest> items);
    CartDTO syncCartPrices(String userId);
}
