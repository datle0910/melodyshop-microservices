package com.melodyshop.order.dto;

import com.melodyshop.order.enums.OrderStatus;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {
    private OrderStatus status;
    private String note;
}
