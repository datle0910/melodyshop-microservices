-- =============================================
-- Order Service Database Schema
-- Database: order_db
-- =============================================

-- Giỏ hàng (mỗi user có 1 giỏ duy nhất)
CREATE TABLE IF NOT EXISTS carts (
    id          VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    created_at  DATETIME     DEFAULT NOW(),
    updated_at  DATETIME     DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uk_carts_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sản phẩm trong giỏ hàng
CREATE TABLE IF NOT EXISTS cart_items (
    id              VARCHAR(36)    NOT NULL,
    cart_id         VARCHAR(36)    NOT NULL,
    product_id      VARCHAR(36)    NOT NULL,
    variant_id      VARCHAR(36)    NULL COMMENT 'NULL nếu SP không có variant',
    sku             VARCHAR(50)    NOT NULL,
    product_name    VARCHAR(255)   NOT NULL,
    variant_name    VARCHAR(150)   NULL,
    unit_price      DECIMAL(15,2)  NOT NULL,
    quantity        INT            NOT NULL DEFAULT 1,
    image_url       VARCHAR(500)   NULL,
    created_at      DATETIME       DEFAULT NOW(),
    updated_at      DATETIME       DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    INDEX idx_cart_items_cart (cart_id),
    UNIQUE KEY uk_cart_item_variant (cart_id, product_id, variant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Mã giảm giá
CREATE TABLE IF NOT EXISTS coupons (
    id                VARCHAR(36)    NOT NULL,
    code              VARCHAR(50)    NOT NULL,
    type              VARCHAR(20)    NOT NULL COMMENT 'PERCENT hoặc FIXED',
    value             DECIMAL(15,2)  NOT NULL COMMENT 'Giá trị giảm (% hoặc VNĐ)',
    min_order_amount  DECIMAL(15,2)  DEFAULT 0 COMMENT 'Đơn tối thiểu để áp dụng',
    max_discount      DECIMAL(15,2)  NULL COMMENT 'Giảm tối đa (cho PERCENT)',
    max_uses          INT            DEFAULT 0 COMMENT '0 = không giới hạn',
    used_count        INT            DEFAULT 0,
    expires_at        DATETIME       NULL,
    is_active         TINYINT(1)     DEFAULT 1,
    created_at        DATETIME       DEFAULT NOW(),
    updated_at        DATETIME       DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupons_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Đơn hàng
CREATE TABLE IF NOT EXISTS orders (
    id                VARCHAR(36)    NOT NULL,
    order_code        VARCHAR(30)    NOT NULL COMMENT 'VD: ORD-20260424-001',
    user_id           VARCHAR(36)    NOT NULL,
    status            VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    subtotal          DECIMAL(15,2)  NOT NULL COMMENT 'Tổng trước giảm giá',
    discount_amount   DECIMAL(15,2)  DEFAULT 0,
    shipping_fee      DECIMAL(15,2)  DEFAULT 0,
    total_amount      DECIMAL(15,2)  NOT NULL COMMENT 'subtotal - discount + shipping',
    coupon_code       VARCHAR(50)    NULL,
    receiver_name     VARCHAR(150)   NOT NULL,
    receiver_phone    VARCHAR(20)    NOT NULL,
    shipping_province VARCHAR(100)   NOT NULL,
    shipping_district VARCHAR(100)   NOT NULL,
    shipping_ward     VARCHAR(100)   NOT NULL,
    shipping_address  VARCHAR(255)   NOT NULL,
    note              VARCHAR(500)   NULL,
    created_at        DATETIME       DEFAULT NOW(),
    updated_at        DATETIME       DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_code (order_code),
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_status (status),
    INDEX idx_orders_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chi tiết đơn hàng (snapshot dữ liệu lúc mua)
CREATE TABLE IF NOT EXISTS order_items (
    id              VARCHAR(36)    NOT NULL,
    order_id        VARCHAR(36)    NOT NULL,
    product_id      VARCHAR(36)    NOT NULL,
    variant_id      VARCHAR(36)    NULL,
    sku             VARCHAR(50)    NOT NULL,
    product_name    VARCHAR(255)   NOT NULL,
    variant_name    VARCHAR(150)   NULL,
    unit_price      DECIMAL(15,2)  NOT NULL,
    quantity        INT            NOT NULL,
    subtotal        DECIMAL(15,2)  NOT NULL COMMENT 'unit_price * quantity',
    image_url       VARCHAR(500)   NULL,
    created_at      DATETIME       DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_items_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Lịch sử trạng thái đơn hàng (timeline)
CREATE TABLE IF NOT EXISTS order_status_logs (
    id          VARCHAR(36)  NOT NULL,
    order_id    VARCHAR(36)  NOT NULL,
    old_status  VARCHAR(20)  NULL,
    new_status  VARCHAR(20)  NOT NULL,
    note        VARCHAR(500) NULL,
    changed_by  VARCHAR(36)  NULL COMMENT 'user_id của người thay đổi',
    created_at  DATETIME     DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_status_logs_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_status_logs_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Danh sách yêu thích
CREATE TABLE IF NOT EXISTS wishlists (
    id          VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    product_id  VARCHAR(36)  NOT NULL,
    created_at  DATETIME     DEFAULT NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uk_wishlist_user_product (user_id, product_id),
    INDEX idx_wishlists_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed: Coupon mẫu
INSERT INTO coupons (id, code, type, value, min_order_amount, max_discount, max_uses, is_active)
VALUES
    (UUID(), 'WELCOME10', 'PERCENT', 10.00, 200000, 100000, 0, 1),
    (UUID(), 'FLAT50K', 'FIXED', 50000.00, 300000, NULL, 100, 1);
