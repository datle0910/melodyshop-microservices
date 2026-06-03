ALTER TABLE payment_transaction
    MODIFY COLUMN status ENUM(
        'PENDING', 'WAITING_CONFIRMATION', 'SUCCESS',
        'FAILED', 'CANCELLED', 'EXPIRED'
    ) NOT NULL,
    ADD COLUMN user_id VARCHAR(36) NULL AFTER order_id,
    ADD COLUMN method VARCHAR(30) NULL AFTER provider,
    ADD COLUMN bank_code VARCHAR(50) NULL AFTER method,
    ADD COLUMN bank_name VARCHAR(150) NULL AFTER bank_code,
    ADD COLUMN account_number VARCHAR(50) NULL AFTER bank_name,
    ADD COLUMN account_name VARCHAR(150) NULL AFTER account_number,
    ADD COLUMN transfer_content VARCHAR(150) NULL AFTER account_name,
    ADD COLUMN qr_code LONGTEXT NULL AFTER transfer_content,
    ADD COLUMN qr_url VARCHAR(1000) NULL AFTER qr_code,
    ADD COLUMN expired_at DATETIME(6) NULL AFTER qr_url,
    ADD COLUMN confirmed_by VARCHAR(36) NULL AFTER expired_at,
    ADD COLUMN confirmed_at DATETIME(6) NULL AFTER confirmed_by;

CREATE INDEX idx_payment_user_id ON payment_transaction(user_id);
CREATE INDEX idx_payment_provider_status_expired ON payment_transaction(provider, status, expired_at);
