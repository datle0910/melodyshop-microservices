package com.melodyshop.order.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateOrderItemRequest {
    @Min(value = 1, message = "So luong phai lon hon 0")
    private int quantity;
}
