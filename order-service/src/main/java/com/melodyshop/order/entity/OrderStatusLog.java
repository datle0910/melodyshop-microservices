package com.melodyshop.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatusLog {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "old_status", length = 20)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;

    @Column(length = 500)
    private String note;

    @Column(name = "changed_by", length = 36)
    private String changedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
