-- Add version column for optimistic locking
ALTER TABLE inventory
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Also ensure warehouses and inventory_logs have version if needed
ALTER TABLE inventory_logs
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
