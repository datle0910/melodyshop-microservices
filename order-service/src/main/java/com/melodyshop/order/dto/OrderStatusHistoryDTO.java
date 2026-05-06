package com.melodyshop.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryDTO {
    private String id;
    private String orderId;
    private String fromStatus;
    private String toStatus;
    private String note;
    private String changedBy;
    private LocalDateTime createdAt;
}
