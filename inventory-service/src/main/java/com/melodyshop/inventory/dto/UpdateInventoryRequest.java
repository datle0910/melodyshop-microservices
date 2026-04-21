package com.melodyshop.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO cho cập nhật tồn kho (nhập hàng).
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateInventoryRequest {

    @NotBlank(message = "Product ID không được để trống")
    private String productId;

    private String variantId;

    @NotBlank(message = "SKU không được để trống")
    private String sku;

    @NotBlank(message = "Warehouse ID không được để trống")
    private String warehouseId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng phải >= 0")
    private Integer quantity;

    private Integer reorderPoint;
    private String note;
}
