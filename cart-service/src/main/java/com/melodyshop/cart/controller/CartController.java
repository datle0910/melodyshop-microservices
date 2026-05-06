package com.melodyshop.cart.controller;

import com.melodyshop.cart.dto.AddToCartRequest;
import com.melodyshop.cart.dto.CartDTO;
import com.melodyshop.cart.dto.CartItemDTO;
import com.melodyshop.cart.dto.UpdateCartItemRequest;
import com.melodyshop.cart.service.CartService;
import com.melodyshop.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartDTO>> getCart(
            @RequestHeader("X-User-Id") String userId) {
        CartDTO cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartItemDTO>> addToCart(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddToCartRequest request) {
        CartItemDTO item = cartService.addToCart(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(item));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartItemDTO>> updateCartItem(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("itemId") String itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartItemDTO item = cartService.updateCartItem(userId, itemId, request);
        if (item == null) {
            return ResponseEntity.ok(ApiResponse.ok("Đã xóa sản phẩm khỏi giỏ hàng", null));
        }
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật giỏ hàng thành công", item));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeCartItem(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("itemId") String itemId) {
        cartService.removeCartItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @RequestHeader("X-User-Id") String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
