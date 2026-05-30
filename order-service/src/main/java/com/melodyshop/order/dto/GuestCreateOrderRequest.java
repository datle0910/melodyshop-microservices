package com.melodyshop.order.dto;

import com.melodyshop.order.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class GuestCreateOrderRequest {

    @NotBlank(message = "Thong tin nguoi nhan khong duoc de trong")
    private String shippingFullName;

    @NotBlank(message = "So dien thoai khong duoc de trong")
    private String shippingPhone;

    @NotBlank(message = "Dia chi giao hang khong duoc de trong")
    private String shippingAddress;

    private String shippingCity;

    @Email(message = "Email khong hop le")
    private String shippingEmail;

    private String shippingPostalCode;

    private String orderNote;

    @NotNull(message = "Phuong thuc thanh toan khong duoc de trong")
    private PaymentMethod paymentMethod;

    private String provider;

    @NotEmpty(message = "Danh sach san pham khong duoc de trong")
    @Valid
    private List<OrderItemRequest> items;

    private String voucherCode;
}
