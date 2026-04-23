-- =============================================
-- Inventory Service Database Schema
-- Database: inventory_db
-- =============================================

-- Kho hàng
CREATE TABLE IF NOT EXISTS warehouses (
    id          VARCHAR(36)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    location    VARCHAR(255) NULL,
    is_active   TINYINT(1)   DEFAULT 1,
    created_at  DATETIME     DEFAULT NOW(),
    updated_at  DATETIME     DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tồn kho (theo variant + warehouse)
CREATE TABLE IF NOT EXISTS inventory (
    id                  VARCHAR(36)  NOT NULL,
    product_id          VARCHAR(36)  NOT NULL,
    variant_id          VARCHAR(36)  NULL COMMENT 'NULL nếu SP không có variant',
    sku                 VARCHAR(50)  NOT NULL,
    warehouse_id        VARCHAR(36)  NOT NULL,
    quantity            INT          NOT NULL DEFAULT 0 COMMENT 'Tổng số lượng',
    reserved_quantity   INT          NOT NULL DEFAULT 0 COMMENT 'Số lượng đang khóa (đang thanh toán)',
    reorder_point       INT          DEFAULT 10 COMMENT 'Ngưỡng cảnh báo hết hàng',
    created_at          DATETIME     DEFAULT NOW(),
    updated_at          DATETIME     DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_warehouse (sku, warehouse_id),
    INDEX idx_inventory_product (product_id),
    INDEX idx_inventory_variant (variant_id),
    INDEX idx_inventory_warehouse (warehouse_id),
    INDEX idx_inventory_sku (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Lịch sử biến động kho (audit trail)
CREATE TABLE IF NOT EXISTS inventory_logs (
    id              VARCHAR(36)  NOT NULL,
    inventory_id    VARCHAR(36)  NOT NULL,
    action          VARCHAR(30)  NOT NULL COMMENT 'IMPORT, RESERVE, DEDUCT, UNRESERVE, ADJUST',
    quantity_change  INT          NOT NULL COMMENT 'Số lượng thay đổi (+ hoặc -)',
    quantity_before  INT          NOT NULL,
    quantity_after   INT          NOT NULL,
    reference_id    VARCHAR(36)  NULL COMMENT 'Order ID hoặc reference khác',
    note            VARCHAR(500) NULL,
    created_by      VARCHAR(36)  NULL,
    created_at      DATETIME     DEFAULT NOW(),
    PRIMARY KEY (id),
    INDEX idx_logs_inventory (inventory_id),
    INDEX idx_logs_action (action),
    INDEX idx_logs_reference (reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed: Kho mặc định
INSERT INTO warehouses (id, name, location, is_active)
VALUES ('default-warehouse-001', 'Kho chính HCM', 'Quận 1, TP.HCM', 1)
ON DUPLICATE KEY UPDATE name = name;
