package com.melodyshop.ai.infrastructure.client;

import com.melodyshop.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * REST-based cart client using RestTemplate with LoadBalancer.
 */
@Component
public class CartRestClient {

    @LoadBalanced
    @Autowired
    private RestTemplate restTemplate;

    private static final String BASE_URL = "http://CART-SERVICE";

    public record AddToCartRequest(
            String userId,
            String productId,
            String productName,
            String productImage,
            String variantId,
            String variantName,
            String sku,
            Double unitPrice,
            Integer quantity
    ) {}

    public record CartItemDTO(
            String id,
            String productId,
            String productName,
            String productImage,
            String variantId,
            String variantName,
            String sku,
            Double unitPrice,
            Integer quantity,
            Double subtotal
    ) {}

    public record CartDTO(
            String id,
            String userId,
            java.util.List<CartItemDTO> items,
            Integer totalItems,
            Double totalAmount
    ) {}

    public ApiResponse<CartItemDTO> addToCart(AddToCartRequest request) {
        return restTemplate.exchange(
            BASE_URL + "/api/cart/items",
            HttpMethod.POST,
            new HttpEntity<>(request),
            new ParameterizedTypeReference<ApiResponse<CartItemDTO>>() {}
        ).getBody();
    }

    public ApiResponse<CartDTO> getCart(String userId) {
        return restTemplate.exchange(
            BASE_URL + "/api/cart?userId=" + userId,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<CartDTO>>() {}
        ).getBody();
    }
}
