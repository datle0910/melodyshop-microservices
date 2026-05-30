package com.melodyshop.inventory.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "inventory_import_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryImportItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id", nullable = false)
    private InventoryImport inventoryImport;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(name = "product_id", length = 36)
    private String productId;

    @Column(name = "variant_id", length = 36)
    private String variantId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "quantity_before", nullable = false)
    private Integer quantityBefore;

    @Column(name = "quantity_after", nullable = false)
    private Integer quantityAfter;

    @Column(name = "quantity_added", nullable = false)
    private Integer quantityAdded;

    @Column(name = "import_price", precision = 12, scale = 2)
    private BigDecimal importPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
