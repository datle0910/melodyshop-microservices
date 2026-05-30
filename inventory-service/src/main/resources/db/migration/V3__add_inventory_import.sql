-- V3: Inventory Import (stock import notes / phiếu nhập hàng)
-- Stores stock import records so admins can track where inventory came from.

CREATE TABLE inventory_imports (
    id VARCHAR(36) PRIMARY KEY,
    import_code VARCHAR(50) UNIQUE NOT NULL,
    note TEXT,
    imported_by VARCHAR(36),
    total_quantity INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_vietnamese_ci;

CREATE TABLE inventory_import_items (
    id VARCHAR(36) PRIMARY KEY,
    import_id VARCHAR(36) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    product_id VARCHAR(36),
    variant_id VARCHAR(36),
    product_name VARCHAR(255),
    quantity_before INT NOT NULL,
    quantity_after INT NOT NULL,
    quantity_added INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_import_item_import FOREIGN KEY (import_id) REFERENCES inventory_imports(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_vietnamese_ci;

CREATE INDEX idx_import_code ON inventory_imports(import_code);
CREATE INDEX idx_import_items_import_id ON inventory_import_items(import_id);
CREATE INDEX idx_import_items_sku ON inventory_import_items(sku);
