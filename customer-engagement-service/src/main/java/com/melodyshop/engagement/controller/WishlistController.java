package com.melodyshop.engagement.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.engagement.dto.AddWishlistItemRequest;
import com.melodyshop.engagement.dto.WishlistItemResponse;
import com.melodyshop.engagement.service.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WishlistItemResponse>> addToWishlist(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddWishlistItemRequest request) {
        WishlistItemResponse response = wishlistService.addToWishlist(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Thêm vào wishlist thành công", response));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String productId) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> getWishlist(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(wishlistService.getWishlist(userId)));
    }
}
