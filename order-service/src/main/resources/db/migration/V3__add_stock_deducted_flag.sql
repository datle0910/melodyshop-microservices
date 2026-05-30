-- V3: Add stock_deducted flag to orders table
-- Tracks whether stock has been deducted for an order to prevent duplicate deduction.
-- Also add INDEX for stock_deducted for query optimization.

ALTER TABLE orders
    ADD COLUMN stock_deducted TINYINT(1) DEFAULT 0 AFTER paid_at;

CREATE INDEX idx_orders_stock_deducted ON orders(stock_deducted);
