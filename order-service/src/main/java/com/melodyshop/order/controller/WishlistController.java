package com.melodyshop.order.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.dto.WishlistDTO;
import com.melodyshop.order.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistDTO>>> getWishlist(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(wishlistService.getWishlist(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WishlistDTO>> addToWishlist(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String productId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(wishlistService.addToWishlist(userId, productId)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String productId) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa khỏi danh sách yêu thích", null));
    }
}
