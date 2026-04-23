package com.melodyshop.order.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.dto.*;
import com.melodyshop.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartDTO>> getCart(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(userId)));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartDTO>> addItem(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đã thêm vào giỏ hàng", cartService.addItem(userId, request)));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartDTO>> updateItem(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String itemId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật số lượng thành công",
                cartService.updateItemQuantity(userId, itemId, quantity)));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartDTO>> removeItem(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String itemId) {
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa khỏi giỏ hàng", cartService.removeItem(userId, itemId)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(@RequestHeader("X-User-Id") String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa toàn bộ giỏ hàng", null));
    }
}
