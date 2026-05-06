package com.melodyshop.order.dto;

import lombok.Data;

@Data
public class InventoryActionRequest {
    private String sku;
    private Integer quantity;
    private String orderId;
    private String note;
}
