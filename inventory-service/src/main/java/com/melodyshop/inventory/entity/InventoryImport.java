package com.melodyshop.inventory.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_imports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryImport extends BaseEntity {

    @Column(name = "import_code", nullable = false, unique = true, length = 50)
    private String importCode;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "imported_by", length = 36)
    private String importedBy;

    @Column(name = "total_quantity", nullable = false)
    @Builder.Default
    private Integer totalQuantity = 0;

    @OneToMany(mappedBy = "inventoryImport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryImportItem> items = new ArrayList<>();

    public void addItem(InventoryImportItem item) {
        items.add(item);
        item.setInventoryImport(this);
    }
}
