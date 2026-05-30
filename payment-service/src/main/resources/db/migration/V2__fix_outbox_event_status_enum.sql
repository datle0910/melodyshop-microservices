-- Fix: Change enum columns from VARCHAR to ENUM to match JPA @Enumerated expectations with MariaDBDialect

ALTER TABLE outbox_event
    MODIFY COLUMN status ENUM('NEW', 'SENT') NOT NULL;

ALTER TABLE payment_transaction
    MODIFY COLUMN status ENUM('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED') NOT NULL;
