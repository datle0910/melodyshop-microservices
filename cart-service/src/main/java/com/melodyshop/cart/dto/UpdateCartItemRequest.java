package com.melodyshop.cart.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateCartItemRequest {
    @NotNull(message = "So luong khong duoc de trong")
    @Min(value = 0, message = "So luong khong the nho hon 0")
    private Integer quantity;
}
