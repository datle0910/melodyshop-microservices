package com.melodyshop.cart.service.impl;

import com.melodyshop.cart.dto.AddToCartRequest;
import com.melodyshop.cart.dto.CartDTO;
import com.melodyshop.cart.dto.CartItemDTO;
import com.melodyshop.cart.dto.UpdateCartItemRequest;
import com.melodyshop.cart.entity.Cart;
import com.melodyshop.cart.entity.CartItem;
import com.melodyshop.cart.repository.CartItemRepository;
import com.melodyshop.cart.repository.CartRepository;
import com.melodyshop.cart.service.CartService;
import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = true)
    public CartDTO getCartByUserId(String userId) {
        Cart cart = getOrCreateCart(userId);
        return toDTO(cart);
    }

    @Override
    @Transactional
    public CartItemDTO addToCart(String userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);

        Optional<CartItem> existingItem;
        if (request.getVariantId() != null && !request.getVariantId().isBlank()) {
            existingItem = cartItemRepository.findByCartIdAndProductIdAndVariantId(
                    cart.getId(), request.getProductId(), request.getVariantId());
        } else {
            existingItem = cartItemRepository.findByCartIdAndSku(cart.getId(), request.getSku());
        }

        CartItem item;
        if (existingItem.isPresent()) {
            item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item = cartItemRepository.save(item);
            log.info("Updated cart item {} quantity to {}", item.getId(), item.getQuantity());
        } else {
            item = CartItem.builder()
                    .cart(cart)
                    .productId(request.getProductId())
                    .productName(request.getProductName())
                    .productImage(request.getProductImage())
                    .variantId(request.getVariantId())
                    .variantName(request.getVariantName())
                    .sku(request.getSku())
                    .unitPrice(request.getUnitPrice())
                    .quantity(request.getQuantity())
                    .build();
            item = cartItemRepository.save(item);
            log.info("Added new item {} to cart {}", item.getId(), cart.getId());
        }

        recalculateCartTotal(cart);
        return toItemDTO(item);
    }

    @Override
    @Transactional
    public CartItemDTO updateCartItem(String userId, String itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("San pham khong nam trong gio hang cua ban");
        }

        if (request.getQuantity() == 0) {
            cartItemRepository.delete(item);
            recalculateCartTotal(cart);
            log.info("Removed cart item {} (quantity set to 0)", itemId);
            return null;
        }

        item.setQuantity(request.getQuantity());
        item = cartItemRepository.save(item);
        recalculateCartTotal(cart);

        log.info("Updated cart item {} quantity to {}", itemId, request.getQuantity());
        return toItemDTO(item);
    }

    @Override
    @Transactional
    public void removeCartItem(String userId, String itemId) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", itemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("San pham khong nam trong gio hang cua ban");
        }

        cartItemRepository.delete(item);
        recalculateCartTotal(cart);

        log.info("Removed cart item {} from cart {}", itemId, cart.getId());
    }

    @Override
    @Transactional
    public void clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cartItemRepository.deleteAllByCartId(cart.getId());
            cart.setTotalAmount(BigDecimal.ZERO);
            cartRepository.save(cart);
            log.info("Cleared cart for user {}", userId);
        }
    }

    @Override
    @Transactional
    public CartDTO mergeCart(String userId, List<AddToCartRequest> items) {
        Cart cart = getOrCreateCart(userId);
        for (AddToCartRequest item : items) {
            addToCart(userId, item);
        }
        return toDTO(cartRepository.findById(cart.getId()).orElse(cart));
    }

    private Cart getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .totalAmount(BigDecimal.ZERO)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private void recalculateCartTotal(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        BigDecimal total = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(total);
        cartRepository.save(cart);
    }

    private CartDTO toDTO(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());

        List<CartItemDTO> itemDTOs = items.stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());

        int totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();

        return CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemDTOs)
                .totalItems(totalItems)
                .totalAmount(cart.getTotalAmount())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartItemDTO toItemDTO(CartItem item) {
        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return CartItemDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productImage(item.getProductImage())
                .variantId(item.getVariantId())
                .variantName(item.getVariantName())
                .sku(item.getSku())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
