package com.melodyshop.ai.infrastructure.client;

import com.melodyshop.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Simple REST-based product client using RestTemplate with LoadBalancer.
 * Uses ParameterizedTypeReference to preserve generic type information.
 */
@Component
public class ProductRestClient {

    @LoadBalanced
    @Autowired
    private RestTemplate restTemplate;

    private static final String BASE_URL = "http://PRODUCT-SERVICE";

    public record ProductListData(
            List<ProductSummaryDTO> content,
            int totalPages,
            long totalElements
    ) {}

    public record ProductSummaryDTO(
            String id,
            String name,
            String slug,
            String image,
            Double basePrice,
            String categoryName,
            String brandName,
            Double averageRating,
            Integer reviewCount,
            Boolean inStock
    ) {
        public boolean isInStock() { return Boolean.TRUE.equals(inStock); }
    }

    public record ProductDetailDTO(
            String id,
            String name,
            String slug,
            String description,
            Double basePrice,
            String categoryName,
            String brandName,
            Double averageRating,
            Integer reviewCount,
            Boolean inStock,
            List<VariantDTO> variants,
            List<Object> images,
            Object specs
    ) {
        public boolean isInStock() { return Boolean.TRUE.equals(inStock); }
    }

    public record VariantDTO(
            String id,
            String sku,
            String variantName,
            Double price,
            Integer stockQuantity,
            Boolean inStock
    ) {
        public boolean isInStock() { return Boolean.TRUE.equals(inStock); }
    }

    public ApiResponse<ProductListData> searchProducts(
            String keyword, String categoryId, String brandId,
            Double minPrice, Double maxPrice,
            int page, int size, String sortBy, String sortDir
    ) {
        StringBuilder url = new StringBuilder(BASE_URL).append("/api/products?");
        if (keyword != null) url.append("keyword=").append(keyword).append("&");
        if (categoryId != null) url.append("categoryId=").append(categoryId).append("&");
        if (brandId != null) url.append("brandId=").append(brandId).append("&");
        if (minPrice != null) url.append("minPrice=").append(minPrice).append("&");
        if (maxPrice != null) url.append("maxPrice=").append(maxPrice).append("&");
        url.append("page=").append(page).append("&");
        url.append("size=").append(size).append("&");
        url.append("sortBy=").append(sortBy).append("&");
        url.append("sortDir=").append(sortDir);

        return restTemplate.exchange(
            url.toString(),
            org.springframework.http.HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<ProductListData>>() {}
        ).getBody();
    }

    public ApiResponse<ProductDetailDTO> getProductById(String id) {
        return restTemplate.exchange(
            BASE_URL + "/api/products/" + id,
            org.springframework.http.HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<ProductDetailDTO>>() {}
        ).getBody();
    }

    public ApiResponse<ProductDetailDTO> getProductBySlug(String slug) {
        return restTemplate.exchange(
            BASE_URL + "/api/products/slug/" + slug,
            org.springframework.http.HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<ProductDetailDTO>>() {}
        ).getBody();
    }
}
