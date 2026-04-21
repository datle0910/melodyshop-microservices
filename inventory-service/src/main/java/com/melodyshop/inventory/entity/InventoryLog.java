package com.melodyshop.inventory.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryLog extends BaseEntity {

    @Column(name = "inventory_id", nullable = false, length = 36)
    private String inventoryId;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @Column(length = 500)
    private String note;

    @Column(name = "created_by", length = 36)
    private String createdBy;
}
