package com.melodyshop.product.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductImageDTO {
    private String id;
    private String imageUrl;
    private String altText;
    private Integer sortOrder;
    private Boolean isPrimary;
}
