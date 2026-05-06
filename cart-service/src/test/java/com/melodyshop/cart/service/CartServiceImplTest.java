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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private String userId;
    private Cart cart;

    @BeforeEach
    void setUp() {
        userId = "user-001";
        cart = Cart.builder()
                .userId(userId)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();
        cart.setId("cart-001");
    }

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
    void addToCart_newItem_shouldAddItem() {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId("prod-001");
        request.setProductName("Yamaha Guitar");
        request.setUnitPrice(new BigDecimal("15000000"));
        request.setQuantity(1);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductIdAndVariantId(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(cartItemRepository.findByCartIdAndSku(anyString(), any())).thenReturn(Optional.empty());
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

        CartItem existingItem = CartItem.builder()
                .cart(cart)
                .productId("prod-001")
                .productName("Yamaha Guitar")
                .unitPrice(new BigDecimal("15000000"))
                .quantity(1)
                .build();
        existingItem.setId("item-001");

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductIdAndVariantId("cart-001", "prod-001", null))
                .thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(existingItem);
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-001"))
                .thenReturn(new ArrayList<>(List.of(existingItem)));

        CartItemDTO result = cartService.addToCart(userId, request);

        assertNotNull(result);
        assertEquals(3, result.getQuantity());
        verify(cartItemRepository).save(argThat(item -> item.getQuantity() == 3));
    }

    @Test
    void updateCartItem_increaseQuantity_shouldSucceed() {
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId("prod-001")
                .productName("Yamaha Guitar")
                .unitPrice(new BigDecimal("15000000"))
                .quantity(1)
                .build();
        item.setId("item-001");

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("item-001")).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(item);
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-001"))
                .thenReturn(new ArrayList<>(List.of(item)));

        CartItemDTO result = cartService.updateCartItem(userId, "item-001", request);

        assertNotNull(result);
        verify(cartItemRepository).save(argThat(i -> i.getQuantity() == 5));
    }

    @Test
    void updateCartItem_setZero_shouldDeleteItem() {
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId("prod-001")
                .productName("Yamaha Guitar")
                .unitPrice(new BigDecimal("15000000"))
                .quantity(1)
                .build();
        item.setId("item-001");

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(0);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("item-001")).thenReturn(Optional.of(item));
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-001")).thenReturn(new ArrayList<>());

        CartItemDTO result = cartService.updateCartItem(userId, "item-001", request);

        assertNull(result);
        verify(cartItemRepository).delete(item);
    }

    @Test
    void updateCartItem_wrongCart_shouldThrowException() {
        Cart otherCart = Cart.builder().userId("other-user").build();
        otherCart.setId("other-cart");

        CartItem item = CartItem.builder()
                .cart(otherCart)
                .productId("prod-001")
                .productName("Yamaha Guitar")
                .unitPrice(new BigDecimal("15000000"))
                .quantity(1)
                .build();
        item.setId("item-001");

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("item-001")).thenReturn(Optional.of(item));

        assertThrows(BadRequestException.class, () ->
                cartService.updateCartItem(userId, "item-001", request));
    }

    @Test
    void removeCartItem_shouldDeleteItem() {
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId("prod-001")
                .productName("Yamaha Guitar")
                .unitPrice(new BigDecimal("15000000"))
                .quantity(1)
                .build();
        item.setId("item-001");

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("item-001")).thenReturn(Optional.of(item));
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-001")).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> cartService.removeCartItem(userId, "item-001"));
        verify(cartItemRepository).delete(item);
    }

    @Test
    void removeCartItem_notFound_shouldThrowException() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.removeCartItem(userId, "nonexistent"));
    }

    @Test
    void clearCart_shouldDeleteAllItems() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-001")).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> cartService.clearCart(userId));
        verify(cartItemRepository).deleteAllByCartId("cart-001");
        verify(cartRepository).save(argThat(c -> c.getTotalAmount().compareTo(BigDecimal.ZERO) == 0));
    }

    @Test
    void getCartByUserId_withItems_shouldReturnCorrectTotal() {
        CartItem item1 = CartItem.builder()
                .cart(cart)
                .productId("prod-001")
                .productName("Yamaha Guitar")
                .unitPrice(new BigDecimal("15000000"))
                .quantity(2)
                .build();
        item1.setId("item-001");

        CartItem item2 = CartItem.builder()
                .cart(cart)
                .productId("prod-002")
                .productName("Fender Guitar")
                .unitPrice(new BigDecimal("25000000"))
                .quantity(1)
                .build();
        item2.setId("item-002");

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByCreatedAtAsc("cart-001"))
                .thenReturn(new ArrayList<>(List.of(item1, item2)));

        CartDTO result = cartService.getCartByUserId(userId);

        assertNotNull(result);
        assertEquals(3, result.getTotalItems());
        assertEquals(new BigDecimal("55000000"), result.getTotalAmount());
    }
}
