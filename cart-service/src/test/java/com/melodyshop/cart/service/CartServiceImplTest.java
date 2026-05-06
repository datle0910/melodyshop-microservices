package com.melodyshop.cart.service.impl;

import com.melodyshop.cart.dto.AddToCartRequest;
import com.melodyshop.cart.dto.CartDTO;
import com.melodyshop.cart.dto.CartItemDTO;
import com.melodyshop.cart.dto.UpdateCartItemRequest;
import com.melodyshop.cart.entity.Cart;
import com.melodyshop.cart.entity.CartItem;
import com.melodyshop.cart.repository.CartItemRepository;
import com.melodyshop.cart.repository.CartRepository;
import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CartServiceImplTest {

    private CartRepository cartRepository;
    private CartItemRepository cartItemRepository;
    private CartServiceImpl cartService;

    private String userId;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        cartItemRepository = mock(CartItemRepository.class);
        cartService = new CartServiceImpl(cartRepository, cartItemRepository);

        userId = "user-001";
        cart = Cart.builder()
                .userId(userId)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();
        cart.setId("cart-001");
    }

    private CartItem item(String id, int quantity) {
        CartItem i = new CartItem();
        i.setId(id);
        i.setCart(cart);
        i.setProductId("prod-001");
        i.setProductName("Guitar");
        i.setSku("SKU-001");
        i.setUnitPrice(new BigDecimal("15000000"));
        i.setQuantity(quantity);
        return i;
    }

    // ── GET CART ──────────────────────────────────────────────────────────────

    @Test
    void getCartByUserId_existingCart_shouldReturnCart() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-001")).thenReturn(new ArrayList<>());

        CartDTO result = cartService.getCartByUserId(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("cart-001", result.getId());
    }

    @Test
    void getCartByUserId_noCart_shouldCreateNewCart() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
            Cart c = inv.getArgument(0);
            c.setId("new-cart-001");
            return c;
        });
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("new-cart-001")).thenReturn(new ArrayList<>());

        CartDTO result = cartService.getCartByUserId(userId);

        assertNotNull(result);
        assertEquals("new-cart-001", result.getId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void getCartByUserId_withItems_shouldReturnCorrectTotalItems() {
        CartItem item1 = item("item-001", 2);
        item1.setUnitPrice(new BigDecimal("15000000"));
        CartItem item2 = item("item-002", 1);
        item2.setProductId("prod-002");
        item2.setUnitPrice(new BigDecimal("25000000"));

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-001"))
                .thenReturn(new ArrayList<>(List.of(item1, item2)));

        CartDTO result = cartService.getCartByUserId(userId);

        assertNotNull(result);
        assertEquals(3, result.getTotalItems());
    }

    // ── ADD TO CART ───────────────────────────────────────────────────────────

    @Test
    void addToCart_newItem_shouldAddItem() {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId("prod-001");
        request.setProductName("Yamaha Guitar");
        request.setUnitPrice(new BigDecimal("15000000"));
        request.setQuantity(1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndSku("cart-001", null)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> {
            CartItem i = inv.getArgument(0);
            i.setId("item-001");
            return i;
        });
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-001")).thenReturn(new ArrayList<>());

        CartItemDTO result = cartService.addToCart(userId, request);

        assertNotNull(result);
        assertEquals("prod-001", result.getProductId());
        assertEquals("Yamaha Guitar", result.getProductName());
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addToCart_existingItem_shouldIncreaseQuantity() {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId("prod-001");
        request.setProductName("Yamaha Guitar");
        request.setUnitPrice(new BigDecimal("15000000"));
        request.setQuantity(2);

        CartItem existing = item("item-001", 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductIdAndVariantId(
                eq("cart-001"), eq("prod-001"), isNull()))
                .thenReturn(Optional.of(existing));
        doAnswer(inv -> {
            CartItem i = inv.getArgument(0);
            i.setQuantity(3);
            return i;
        }).when(cartItemRepository).save(any());
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-001")).thenReturn(new ArrayList<>());

        CartItemDTO result = cartService.addToCart(userId, request);

        assertNotNull(result);
        assertEquals("prod-001", result.getProductId());
        assertEquals(3, result.getQuantity());
        verify(cartItemRepository).save(any());
    }

    // ── UPDATE CART ITEM ─────────────────────────────────────────────────────

    @Test
    void updateCartItem_increaseQuantity_shouldSucceed() {
        CartItem it = item("item-001", 1);

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("item-001")).thenReturn(Optional.of(it));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-001")).thenReturn(new ArrayList<>());

        CartItemDTO result = cartService.updateCartItem(userId, "item-001", request);

        assertNotNull(result);
        assertEquals(5, result.getQuantity());
    }

    @Test
    void updateCartItem_setZero_shouldDeleteItem() {
        CartItem it = item("item-001", 1);

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(0);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("item-001")).thenReturn(Optional.of(it));

        CartItemDTO result = cartService.updateCartItem(userId, "item-001", request);

        assertNull(result);
        verify(cartItemRepository).delete(it);
    }

    @Test
    void updateCartItem_wrongCart_shouldThrowException() {
        Cart otherCart = Cart.builder().userId("other-user").build();
        otherCart.setId("other-cart");

        CartItem it = CartItem.builder()
                .cart(otherCart)
                .productId("prod-001")
                .productName("Yamaha Guitar")
                .unitPrice(new BigDecimal("15000000"))
                .quantity(1)
                .build();
        it.setId("item-001");

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("item-001")).thenReturn(Optional.of(it));

        assertThrows(BadRequestException.class, () ->
                cartService.updateCartItem(userId, "item-001", request));
    }

    // ── REMOVE CART ITEM ─────────────────────────────────────────────────────

    @Test
    void removeCartItem_shouldDeleteItem() {
        CartItem it = item("item-001", 1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("item-001")).thenReturn(Optional.of(it));
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-001")).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> cartService.removeCartItem(userId, "item-001"));
        verify(cartItemRepository).delete(it);
    }

    @Test
    void removeCartItem_notFound_shouldThrowException() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.removeCartItem(userId, "nonexistent"));
    }

    // ── CLEAR CART ────────────────────────────────────────────────────────────

    @Test
    void clearCart_shouldDeleteAllItems() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        assertDoesNotThrow(() -> cartService.clearCart(userId));
        verify(cartItemRepository).deleteAllByCartId("cart-001");
        verify(cartRepository).save(any(Cart.class));
    }
}
