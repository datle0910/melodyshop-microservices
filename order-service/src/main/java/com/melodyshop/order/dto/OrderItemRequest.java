package com.melodyshop.order.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequest {
    @NotBlank(message = "ID san pham khong duoc de trong")
    private String productId;

    @NotBlank(message = "Ten san pham khong duoc de trong")
    private String productName;

    private String productImage;

    private String variantId;

    private String variantName;

    private String sku;

    @NotNull(message = "So luong khong duoc de trong")
    @Min(value = 1, message = "So luong phai lon hon 0")
    private Integer quantity;

    @NotNull(message = "Don gia khong duoc de trong")
    @DecimalMin(value = "0.0", inclusive = false, message = "Don gia phai lon hon 0")
    private BigDecimal unitPrice;
}
