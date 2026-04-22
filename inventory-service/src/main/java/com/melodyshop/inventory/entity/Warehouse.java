package com.melodyshop.inventory.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "warehouses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Warehouse extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String location;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
