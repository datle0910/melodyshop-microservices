package com.melodyshop.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRequest {

    @NotEmpty(message = "Danh sách sản phẩm nhập hàng không được để trống")
    @Valid
    private List<ImportItemRequest> items;

    private String note;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportItemRequest {

        @NotBlank(message = "SKU không được để trống")
        private String sku;

        @NotBlank(message = "Product ID không được để trống")
        private String productId;

        private String variantId;

        private String productName;

        @Min(value = 1, message = "Số lượng nhập phải >= 1")
        private Integer quantity;
    }
}
