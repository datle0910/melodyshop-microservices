-- Fix: Change enum columns from VARCHAR to ENUM to match JPA @Enumerated expectations with MariaDBDialect
-- Also fix CHAR(36) to VARCHAR(36) for id columns to match BaseEntity.id definition

-- Drop foreign key first before modifying referenced column
ALTER TABLE order_items
    DROP FOREIGN KEY fk_order_items_order;

ALTER TABLE orders
    MODIFY COLUMN id VARCHAR(36) NOT NULL,
    MODIFY COLUMN status ENUM('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    MODIFY COLUMN payment_method ENUM('COD', 'CREDIT_CARD', 'BANK_TRANSFER', 'E_WALLET') NOT NULL;

ALTER TABLE order_items
    MODIFY COLUMN id VARCHAR(36) NOT NULL,
    MODIFY COLUMN order_id VARCHAR(36) NOT NULL,
    ADD CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;

ALTER TABLE order_status_history
    MODIFY COLUMN id VARCHAR(36) NOT NULL,
    MODIFY COLUMN order_id VARCHAR(36) NOT NULL,
    MODIFY COLUMN from_status ENUM('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'),
    MODIFY COLUMN to_status ENUM('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED') NOT NULL;
