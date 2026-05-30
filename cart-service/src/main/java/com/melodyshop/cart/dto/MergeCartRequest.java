package com.melodyshop.cart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class MergeCartRequest {
    @NotEmpty(message = "Danh sach san pham khong duoc de trong")
    @Valid
    private List<AddToCartRequest> items;
}
