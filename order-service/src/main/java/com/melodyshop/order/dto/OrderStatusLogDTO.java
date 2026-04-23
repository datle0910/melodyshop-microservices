package com.melodyshop.order.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderStatusLogDTO {
    private String id;
    private String oldStatus;
    private String newStatus;
    private String note;
    private String changedBy;
    private LocalDateTime createdAt;
}
