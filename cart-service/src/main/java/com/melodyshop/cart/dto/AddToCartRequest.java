package com.melodyshop.cart.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddToCartRequest {
    @NotBlank(message = "ID san pham khong duoc de trong")
    private String productId;

    @NotBlank(message = "Ten san pham khong duoc de trong")
    private String productName;

    private String productImage;

    private String variantId;

    private String variantName;

    private String sku;

    @NotNull(message = "Don gia khong duoc de trong")
    @DecimalMin(value = "0.0", inclusive = false, message = "Don gia phai lon hon 0")
    private BigDecimal unitPrice;

    @Min(value = 1, message = "So luong phai lon hon 0")
    private Integer quantity = 1;
}
