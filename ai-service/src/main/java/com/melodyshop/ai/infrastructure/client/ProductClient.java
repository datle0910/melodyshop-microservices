package com.melodyshop.ai.infrastructure.client;

import com.melodyshop.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {

    @GetMapping("/api/products")
    ApiResponse<ProductListData> searchProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "brandId", required = false) String brandId,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir
    );

    @GetMapping("/api/products/{id}")
    ApiResponse<ProductDetailDTO> getProductById(@PathVariable("id") String id);

    @GetMapping("/api/products/slug/{slug}")
    ApiResponse<ProductDetailDTO> getProductBySlug(@PathVariable("slug") String slug);

    record ProductListData(
            List<ProductSummaryDTO> content,
            int totalPages,
            long totalElements
    ) {}

    record ProductSummaryDTO(
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
    ) {}

    record ProductDetailDTO(
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
            List<String> images,
            Object specs
    ) {}

    record VariantDTO(
            String id,
            String sku,
            String variantName,
            Double price,
            Integer stockQuantity,
            Boolean inStock
    ) {}
}
