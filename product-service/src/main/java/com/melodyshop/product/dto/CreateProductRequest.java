package com.melodyshop.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a new product with variants.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
    private String name;

    private String description;
    private String shortDesc;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0", message = "Giá phải >= 0")
    private BigDecimal basePrice;

    private String categoryId;
    private String brandId;
    private String specs;
    private Boolean isFeatured;

    @Valid
    private List<ProductVariantDTO> variants;

    @Valid
    private List<ProductImageDTO> images;
}
