package com.melodyshop.order.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AddToCartRequest {

    @NotBlank(message = "Product ID không được để trống")
    private String productId;

    private String variantId;

    @NotBlank(message = "SKU không được để trống")
    private String sku;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải >= 1")
    private Integer quantity;
}
