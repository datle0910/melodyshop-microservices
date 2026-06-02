package com.melodyshop.cart.service.impl;

import com.melodyshop.cart.client.ProductClient;
import com.melodyshop.cart.dto.AddToCartRequest;
import com.melodyshop.cart.dto.CartDTO;
import com.melodyshop.cart.dto.CartItemDTO;
import com.melodyshop.cart.dto.UpdateCartItemRequest;
import com.melodyshop.cart.entity.Cart;
import com.melodyshop.cart.entity.CartItem;
import com.melodyshop.cart.repository.CartItemRepository;
import com.melodyshop.cart.repository.CartRepository;
import com.melodyshop.cart.service.CartService;
import com.melodyshop.common.dto.ApiResponse;
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
    private final ProductClient productClient;

    @Override
    @Transactional
    public CartDTO getCartByUserId(String userId) {
        Cart cart = getOrCreateCart(userId);
        refreshCartPrices(cart);
        return toDTO(cart);
    }

    @Override
    @Transactional
    public CartItemDTO addToCart(String userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);

        // Fetch current price from product service
        BigDecimal currentPrice = request.getUnitPrice();
        try {
            ApiResponse<ProductClient.ProductDTO> prodResp = productClient.getProductById(request.getProductId());
            if (prodResp != null && prodResp.isSuccess() && prodResp.getData() != null) {
                ProductClient.ProductDTO product = prodResp.getData();
                if (request.getVariantId() != null && !request.getVariantId().isBlank()) {
                    // Find price by variant
                    ProductClient.ProductVariantDTO variant = product.getVariants().stream()
                            .filter(v -> v.getId().equals(request.getVariantId()))
                            .findFirst()
                            .orElse(null);
                    if (variant != null && variant.getPrice() != null) {
                        currentPrice = variant.getPrice();
                        log.info("Fetched current variant price {} for product {}", currentPrice, request.getProductId());
                    }
                } else if (request.getSku() != null && !request.getSku().isBlank()) {
                    // Find price by SKU
                    ProductClient.ProductVariantDTO variant = product.getVariants().stream()
                            .filter(v -> request.getSku().equals(v.getSku()))
                            .findFirst()
                            .orElse(null);
                    if (variant != null && variant.getPrice() != null) {
                        currentPrice = variant.getPrice();
                        log.info("Fetched current SKU price {} for product {}", currentPrice, request.getProductId());
                    }
                }
                if (currentPrice.equals(request.getUnitPrice()) && product.getBasePrice() != null) {
                    currentPrice = product.getBasePrice();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch current price for product {}: {}, using provided price", 
                    request.getProductId(), e.getMessage());
        }

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
            // Update price to current price when quantity changes
            item.setUnitPrice(currentPrice);
            item.setQuantity(item.getQuantity() + request.getQuantity());
            if (request.getUnitPrice() != null) {
                item.setUnitPrice(request.getUnitPrice());
            }
            item = cartItemRepository.save(item);
            log.info("Updated cart item {} quantity to {} with price {}", item.getId(), item.getQuantity(), currentPrice);
        } else {
            item = CartItem.builder()
                    .cart(cart)
                    .productId(request.getProductId())
                    .productName(request.getProductName())
                    .productImage(request.getProductImage())
                    .variantId(request.getVariantId())
                    .variantName(request.getVariantName())
                    .sku(request.getSku())
                    .unitPrice(currentPrice)
                    .quantity(request.getQuantity())
                    .build();
            item = cartItemRepository.save(item);
            log.info("Added new item {} to cart {} with price {}", item.getId(), cart.getId(), currentPrice);
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

        // Fetch current price when updating quantity
        BigDecimal originalPrice = item.getUnitPrice();
        final BigDecimal[] currentPrice = {originalPrice};
        try {
            ApiResponse<ProductClient.ProductDTO> prodResp = productClient.getProductById(item.getProductId());
            if (prodResp != null && prodResp.isSuccess() && prodResp.getData() != null) {
                ProductClient.ProductDTO product = prodResp.getData();
                if (item.getVariantId() != null && !item.getVariantId().isBlank()) {
                    String variantId = item.getVariantId();
                    ProductClient.ProductVariantDTO variant = product.getVariants().stream()
                            .filter(v -> v.getId().equals(variantId))
                            .findFirst()
                            .orElse(null);
                    if (variant != null && variant.getPrice() != null) {
                        currentPrice[0] = variant.getPrice();
                    }
                } else if (item.getSku() != null && !item.getSku().isBlank()) {
                    String sku = item.getSku();
                    ProductClient.ProductVariantDTO variant = product.getVariants().stream()
                            .filter(v -> sku.equals(v.getSku()))
                            .findFirst()
                            .orElse(null);
                    if (variant != null && variant.getPrice() != null) {
                        currentPrice[0] = variant.getPrice();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch current price for product {}: {}", item.getProductId(), e.getMessage());
        }

        item.setQuantity(request.getQuantity());
        item.setUnitPrice(currentPrice[0]);
        item = cartItemRepository.save(item);
        recalculateCartTotal(cart);

        log.info("Updated cart item {} quantity to {} with price {}", itemId, request.getQuantity(), currentPrice[0]);
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

    private void refreshCartPrices(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        if (items.isEmpty()) {
            return;
        }

        boolean updated = false;
        for (CartItem item : items) {
            try {
                ApiResponse<ProductClient.ProductDTO> response = productClient.getProductById(item.getProductId());
                if (response != null && response.getData() != null) {
                    ProductClient.ProductDTO product = response.getData();
                    BigDecimal latestPrice = product.getBasePrice();

                    // If variant is selected, get variant price
                    if (item.getVariantId() != null && !item.getVariantId().isBlank()) {
                        if (product.getVariants() != null) {
                            Optional<ProductClient.ProductVariantDTO> variantOpt = product.getVariants().stream()
                                    .filter(v -> v.getId().equals(item.getVariantId()))
                                    .findFirst();
                            if (variantOpt.isPresent()) {
                                latestPrice = variantOpt.get().getPrice();
                            }
                        }
                    }

                    if (latestPrice != null && latestPrice.compareTo(item.getUnitPrice()) != 0) {
                        log.info("Updating price for cart item {} from {} to {}", item.getId(), item.getUnitPrice(), latestPrice);
                        item.setUnitPrice(latestPrice);
                        cartItemRepository.save(item);
                        updated = true;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch latest price for product {} from product-service: {}", item.getProductId(), e.getMessage());
            }
        }

        if (updated) {
            recalculateCartTotal(cart);
        }
    }

    @Override
    @Transactional
    public CartDTO syncCartPrices(String userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
        
        for (CartItem item : items) {
            try {
                ApiResponse<ProductClient.ProductDTO> prodResp = productClient.getProductById(item.getProductId());
                if (prodResp != null && prodResp.isSuccess() && prodResp.getData() != null) {
                    ProductClient.ProductDTO product = prodResp.getData();
                    BigDecimal newPrice = null;
                    
                    if (item.getVariantId() != null && !item.getVariantId().isBlank()) {
                        ProductClient.ProductVariantDTO variant = product.getVariants().stream()
                                .filter(v -> v.getId().equals(item.getVariantId()))
                                .findFirst()
                                .orElse(null);
                        if (variant != null) newPrice = variant.getPrice();
                    } else if (item.getSku() != null && !item.getSku().isBlank()) {
                        ProductClient.ProductVariantDTO variant = product.getVariants().stream()
                                .filter(v -> item.getSku().equals(v.getSku()))
                                .findFirst()
                                .orElse(null);
                        if (variant != null) newPrice = variant.getPrice();
                    }
                    
                    if (newPrice != null && !newPrice.equals(item.getUnitPrice())) {
                        item.setUnitPrice(newPrice);
                        cartItemRepository.save(item);
                        log.info("Synced price for cart item {}: {} -> {}", item.getId(), item.getUnitPrice(), newPrice);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to sync price for product {}: {}", item.getProductId(), e.getMessage());
            }
        }
        
        recalculateCartTotal(cart);
        return toDTO(cart);
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
