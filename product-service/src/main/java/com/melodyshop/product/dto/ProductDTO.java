package com.melodyshop.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductDTO {
    private String id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
    private String name;

    private String slug;
    private String description;
    private String shortDesc;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0", message = "Giá phải >= 0")
    private BigDecimal basePrice;

    private String categoryId;
    private String categoryName;
    private String brandId;
    private String brandName;
    private String specs;
    private Boolean isFeatured;
    private Boolean isActive;
    private BigDecimal avgRating;
    private Integer reviewCount;

    private List<ProductVariantDTO> variants;
    private List<ProductImageDTO> images;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
