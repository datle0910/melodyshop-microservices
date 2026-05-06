package com.melodyshop.order.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_status_history")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderStatusHistory extends BaseEntity {

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "changed_by", length = 36)
    private String changedBy;
}
