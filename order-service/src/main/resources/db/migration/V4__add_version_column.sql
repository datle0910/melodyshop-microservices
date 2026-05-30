-- Add version column for Optimistic Locking
ALTER TABLE orders ADD COLUMN version BIGINT DEFAULT 0;
