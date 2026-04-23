package com.melodyshop.order.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateCouponRequest {

    @NotBlank(message = "Mã coupon không được để trống")
    private String code;

    @NotBlank(message = "Loại coupon không được để trống")
    private String type;

    @NotNull(message = "Giá trị không được để trống")
    @DecimalMin(value = "0", message = "Giá trị phải >= 0")
    private BigDecimal value;

    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscount;
    private Integer maxUses;
    private LocalDateTime expiresAt;
}
