package com.melodyshop.payment.service.impl;

import com.melodyshop.payment.entity.OutboxEvent;
import com.melodyshop.payment.enums.OutboxStatus;
import com.melodyshop.payment.repository.OutboxEventRepository;
import com.melodyshop.payment.service.OutboxPublisherService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.logging.Logger;

@Service
public class MockOutboxPublisherService implements OutboxPublisherService {

    private static final Logger LOGGER = Logger.getLogger(MockOutboxPublisherService.class.getName());

    private final OutboxEventRepository outboxEventRepository;

    public MockOutboxPublisherService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${payment.outbox.publisher.fixed-delay-ms:3000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus.NEW);
        for (OutboxEvent event : pendingEvents) {
            LOGGER.info(() -> "[MOCK-EVENT] Publishing eventType=" + event.getEventType() + " payload=" + event.getPayload());
            event.setStatus(OutboxStatus.SENT);
            outboxEventRepository.save(event);
        }
    }
}
