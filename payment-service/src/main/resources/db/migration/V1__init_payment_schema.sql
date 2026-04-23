-- =============================================
-- Payment Service Database Schema
-- Database: payment_db
-- =============================================

CREATE TABLE IF NOT EXISTS payment_transaction (
    id                     VARCHAR(36)    NOT NULL,
    order_id               VARCHAR(64)    NOT NULL,
    amount                 DECIMAL(19, 2) NOT NULL,
    currency               VARCHAR(10)    NOT NULL,
    status                 VARCHAR(20)    NOT NULL,
    idempotency_key        VARCHAR(100)   NOT NULL,
    gateway_transaction_id VARCHAR(100)   NOT NULL,
    active_payment_key     VARCHAR(64)    NULL,
    successful_payment_key VARCHAR(64)    NULL,
    created_at             DATETIME       DEFAULT NOW(),
    updated_at             DATETIME       DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_idempotency_key (idempotency_key),
    UNIQUE KEY uk_payment_gateway_transaction_id (gateway_transaction_id),
    UNIQUE KEY uk_payment_active_payment_key (active_payment_key),
    UNIQUE KEY uk_payment_successful_payment_key (successful_payment_key),
    INDEX idx_payment_order_status (order_id, status),
    INDEX idx_payment_gateway_tx (gateway_transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS outbox_event (
    id         VARCHAR(36)   NOT NULL,
    event_type VARCHAR(100)  NOT NULL,
    payload    LONGTEXT      NOT NULL,
    status     VARCHAR(20)   NOT NULL,
    created_at DATETIME      DEFAULT NOW(),
    updated_at DATETIME      DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    INDEX idx_outbox_status_created_at (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
