package com.melodyshop.payment.entity;

import com.melodyshop.common.entity.BaseEntity;
import com.melodyshop.payment.enums.OutboxStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_outbox_status_created_at", columnList = "status, created_at")
})
public class OutboxEvent extends BaseEntity {

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    public OutboxEvent() {
    }

    public OutboxEvent(String eventType, String payload, OutboxStatus status) {
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }
}
