package com.melodyshop.payment.service;

public interface OutboxPublisherService {
    void publishPendingEvents();
}
