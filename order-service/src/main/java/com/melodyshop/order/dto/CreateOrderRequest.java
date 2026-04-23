package com.melodyshop.order.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateOrderRequest {

    private String couponCode;

    @NotBlank(message = "Tên người nhận không được để trống")
    private String receiverName;

    @NotBlank(message = "SĐT người nhận không được để trống")
    private String receiverPhone;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String shippingProvince;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String shippingDistrict;

    @NotBlank(message = "Phường/Xã không được để trống")
    private String shippingWard;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String shippingAddress;

    private String note;
}
