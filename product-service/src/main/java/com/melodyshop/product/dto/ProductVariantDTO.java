package com.melodyshop.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductVariantDTO {
    private String id;

    @NotBlank(message = "Tên biến thể không được để trống")
    private String variantName;

    @NotBlank(message = "SKU không được để trống")
    private String sku;

    @NotNull(message = "Giá biến thể không được để trống")
    @DecimalMin(value = "0", message = "Giá phải >= 0")
    private BigDecimal price;

    private String color;
    private String size;
    private Boolean isActive;
}
