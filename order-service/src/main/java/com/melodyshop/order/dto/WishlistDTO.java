package com.melodyshop.order.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WishlistDTO {
    private String id;
    private String productId;
    private LocalDateTime createdAt;
}
