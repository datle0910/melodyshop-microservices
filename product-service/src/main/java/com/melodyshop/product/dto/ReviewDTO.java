package com.melodyshop.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewDTO {
    private String id;
    private String productId;
    private String userId;

    @NotNull(message = "Đánh giá sao không được để trống")
    @Min(value = 1, message = "Đánh giá tối thiểu 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa 5 sao")
    private Integer rating;

    @Size(max = 2000, message = "Bình luận tối đa 2000 ký tự")
    private String comment;

    private Boolean isVerified;
    private LocalDateTime createdAt;
}
