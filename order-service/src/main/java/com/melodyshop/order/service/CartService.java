package com.melodyshop.order.service;

import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.order.dto.*;
import com.melodyshop.order.entity.*;
import com.melodyshop.order.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    /**
     * Lấy giỏ hàng của user (tự tạo nếu chưa có).
     */
    public CartDTO getCart(String userId) {
        Cart cart = getOrCreateCart(userId);
        return toDTO(cart);
    }

    /**
     * Thêm sản phẩm vào giỏ hàng.
     * Nếu SP đã có trong giỏ (cùng productId + variantId) → cộng dồn quantity.
     */
    @Transactional
    public CartDTO addItem(String userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);

        // Kiểm tra SP đã có trong giỏ chưa
        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId())
                        && (item.getVariantId() == null ? request.getVariantId() == null
                            : item.getVariantId().equals(request.getVariantId())))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            cartItemRepository.save(existing);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(request.getProductId())
                    .variantId(request.getVariantId())
                    .sku(request.getSku())
                    .productName("Sản phẩm " + request.getProductId())
                    .variantName(request.getVariantId() != null ? "Biến thể " + request.getVariantId() : null)
                    .unitPrice(BigDecimal.ZERO)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
            cartRepository.save(cart);
        }

        log.info("Added item to cart: userId={}, productId={}, qty={}", userId, request.getProductId(), request.getQuantity());
        return toDTO(cartRepository.findByUserId(userId).orElse(cart));
    }

    /**
     * Cập nhật số lượng SP trong giỏ (nếu qty = 0 → xóa).
     */
    @Transactional
    public CartDTO updateItemQuantity(String userId, String itemId, int quantity) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", itemId));

        if (!item.getCart().getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền sửa item này");
        }

        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return toDTO(cartRepository.findByUserId(userId).orElse(cart));
    }

    /**
     * Xóa 1 SP khỏi giỏ.
     */
    @Transactional
    public CartDTO removeItem(String userId, String itemId) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", itemId));

        if (!item.getCart().getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xóa item này");
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        return toDTO(cartRepository.findByUserId(userId).orElse(cart));
    }

    /**
     * Xóa toàn bộ giỏ hàng.
     */
    @Transactional
    public void clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }
    }

    // ========== Private helpers ==========

    private Cart getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = Cart.builder().userId(userId).build();
            return cartRepository.save(newCart);
        });
    }

    private CartDTO toDTO(Cart cart) {
        List<CartItemDTO> items = cart.getItems().stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = items.stream().mapToInt(CartItemDTO::getQuantity).sum();

        return CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(items)
                .totalAmount(total)
                .totalItems(totalItems)
                .build();
    }

    private CartItemDTO toItemDTO(CartItem item) {
        return CartItemDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .sku(item.getSku())
                .productName(item.getProductName())
                .variantName(item.getVariantName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .imageUrl(item.getImageUrl())
                .build();
    }
}
