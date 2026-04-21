package com.melodyshop.search.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductSearchResult {
    private String id;
    private String name;
    private String slug;
    private String shortDesc;
    private BigDecimal basePrice;
    private String categoryId;
    private String categoryName;
    private String brandId;
    private String brandName;
    private Boolean isFeatured;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private String imageUrl;
}
