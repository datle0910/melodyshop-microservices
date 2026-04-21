package com.melodyshop.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO để đồng bộ sản phẩm từ Product Service vào Elasticsearch.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductIndexRequest {

    @NotBlank
    private String id;

    @NotBlank
    private String name;

    private String slug;
    private String description;
    private String shortDesc;

    @NotNull
    private BigDecimal basePrice;

    private String categoryId;
    private String categoryName;
    private String brandId;
    private String brandName;
    private Boolean isFeatured;
    private Boolean isActive;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private String imageUrl;
}
