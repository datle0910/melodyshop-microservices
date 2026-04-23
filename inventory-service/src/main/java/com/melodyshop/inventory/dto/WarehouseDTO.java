package com.melodyshop.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WarehouseDTO {
    private String id;

    @NotBlank(message = "Tên kho không được để trống")
    private String name;

    private String location;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
