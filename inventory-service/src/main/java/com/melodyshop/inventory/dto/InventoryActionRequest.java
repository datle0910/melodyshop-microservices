package com.melodyshop.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO cho các thao tác reserve/deduct/unreserve.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryActionRequest {

    @NotBlank(message = "SKU không được để trống")
    private String sku;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải >= 1")
    private Integer quantity;

    private String orderId;
    private String note;
}
