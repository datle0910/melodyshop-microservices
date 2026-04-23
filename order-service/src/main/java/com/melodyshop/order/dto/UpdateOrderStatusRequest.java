package com.melodyshop.order.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Trạng thái mới không được để trống")
    private String newStatus;

    private String note;
}
