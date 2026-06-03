package com.melodyshop.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutQuoteRequest {

    @NotEmpty(message = "Danh sach san pham khong duoc de trong")
    @Valid
    private List<OrderItemRequest> items;

    private String voucherCode;
}
