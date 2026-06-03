package com.melodyshop.engagement.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminReviewResponse {
    private String id;
    private String productId;
    private String productName;
    private String userId;
    private String userName;
    private String userAvatarUrl;
    private int rating;
    private String comment;
    private java.time.LocalDateTime createdAt;
}
