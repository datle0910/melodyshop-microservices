package com.melodyshop.engagement.service.impl;

import com.melodyshop.engagement.dto.AddWishlistItemRequest;
import com.melodyshop.engagement.dto.CreateReviewRequest;
import com.melodyshop.engagement.entity.PurchasedProduct;
import com.melodyshop.engagement.entity.Review;
import com.melodyshop.engagement.entity.WishlistItem;
import com.melodyshop.engagement.exception.AlreadyReviewedException;
import com.melodyshop.engagement.exception.ProductNotPurchasedException;
import com.melodyshop.engagement.repository.PurchasedProductRepository;
import com.melodyshop.engagement.repository.ReviewRepository;
import com.melodyshop.engagement.repository.WishlistItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerEngagementServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PurchasedProductRepository purchasedProductRepository;

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    private PurchaseEligibilityServiceImpl purchaseEligibilityService;
    private ReviewServiceImpl reviewService;
    private WishlistServiceImpl wishlistService;

    @BeforeEach
    void setUp() {
        purchaseEligibilityService = new PurchaseEligibilityServiceImpl(purchasedProductRepository);
        reviewService = new ReviewServiceImpl(reviewRepository, purchaseEligibilityService);
        wishlistService = new WishlistServiceImpl(wishlistItemRepository);
        ReflectionTestUtils.setField(purchaseEligibilityService, "assumePurchased", true);
    }

    @Test
    void shouldRejectReviewWhenUserAlreadyReviewedProduct() {
        when(reviewRepository.existsByUserIdAndProductId("user-1", "product-1")).thenReturn(true);

        CreateReviewRequest request = new CreateReviewRequest("product-1", 5, "Excellent");

        assertThrows(AlreadyReviewedException.class, () -> reviewService.createReview("user-1", request));
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void shouldRejectReviewWhenProductWasNotPurchasedAndMockIsDisabled() {
        ReflectionTestUtils.setField(purchaseEligibilityService, "assumePurchased", false);
        when(reviewRepository.existsByUserIdAndProductId("user-2", "product-2")).thenReturn(false);
        when(purchasedProductRepository.existsByUserIdAndProductId("user-2", "product-2")).thenReturn(false);

        CreateReviewRequest request = new CreateReviewRequest("product-2", 4, "Good");

        assertThrows(ProductNotPurchasedException.class, () -> reviewService.createReview("user-2", request));
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void shouldReturnExistingWishlistItemWhenDuplicateAddHappens() {
        WishlistItem existing = new WishlistItem();
        existing.setId("wishlist-1");
        existing.setUserId("user-3");
        existing.setProductId("product-3");
        existing.setCreatedAt(LocalDateTime.now());

        when(wishlistItemRepository.findByUserIdAndProductId("user-3", "product-3")).thenReturn(Optional.of(existing));

        var response = wishlistService.addToWishlist("user-3", new AddWishlistItemRequest("product-3"));

        assertEquals("wishlist-1", response.getId());
        verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
    }

    @Test
    void shouldStoreUniquePurchasedProductsFromOrderCompletedEvent() {
        when(purchasedProductRepository.existsByUserIdAndProductId("user-4", "product-4")).thenReturn(false);
        when(purchasedProductRepository.existsByUserIdAndProductId("user-4", "product-5")).thenReturn(false);
        when(purchasedProductRepository.save(any(PurchasedProduct.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int inserted = purchaseEligibilityService.handleOrderCompletedEvent(
                "order-4",
                "user-4",
                List.of("product-4", "product-4", "product-5")
        );

        assertEquals(2, inserted);
        verify(purchasedProductRepository, times(2)).save(any(PurchasedProduct.class));
    }
}
