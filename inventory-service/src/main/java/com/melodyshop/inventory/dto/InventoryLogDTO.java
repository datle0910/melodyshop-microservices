package com.melodyshop.inventory.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryLogDTO {
    private String id;
    private String inventoryId;
    private String action;
    private Integer quantityChange;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private String referenceId;
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
}
