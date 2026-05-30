-- =============================================
-- Payment Service - Add Missing Columns
-- Fixes schema mismatch with PaymentTransaction entity
-- =============================================

ALTER TABLE payment_transaction
    ADD COLUMN IF NOT EXISTS provider VARCHAR(20) NULL AFTER successful_payment_key,
    ADD COLUMN IF NOT EXISTS version BIGINT NULL DEFAULT 0 AFTER provider,
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(64) NULL DEFAULT NULL AFTER version,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64) NULL DEFAULT NULL AFTER created_by,
    ADD COLUMN IF NOT EXISTS is_deleted TINYINT(1) NULL DEFAULT 0 AFTER updated_by;

-- Add back unique constraints for nullable columns (MariaDB requires index for unique)
CREATE INDEX IF NOT EXISTS idx_provider ON payment_transaction(provider);
