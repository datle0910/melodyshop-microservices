-- =============================================
-- Product Service Database Schema
-- Database: product_db
-- =============================================

-- Danh mục sản phẩm (hỗ trợ cấu trúc cây cha-con)
CREATE TABLE IF NOT EXISTS categories (
    id          VARCHAR(36)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    description TEXT         NULL,
    image_url   VARCHAR(500) NULL,
    parent_id   VARCHAR(36)  NULL,
    sort_order  INT          DEFAULT 0,
    is_active   TINYINT(1)   DEFAULT 1,
    created_at  DATETIME     DEFAULT NOW(),
    updated_at  DATETIME     DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    INDEX idx_categories_parent (parent_id),
    INDEX idx_categories_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thương hiệu
CREATE TABLE IF NOT EXISTS brands (
    id          VARCHAR(36)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    description TEXT         NULL,
    logo_url    VARCHAR(500) NULL,
    is_active   TINYINT(1)   DEFAULT 1,
    created_at  DATETIME     DEFAULT NOW(),
    updated_at  DATETIME     DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    INDEX idx_brands_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sản phẩm chính
CREATE TABLE IF NOT EXISTS products (
    id              VARCHAR(36)    NOT NULL,
    name            VARCHAR(255)   NOT NULL,
    slug            VARCHAR(280)   NOT NULL UNIQUE,
    description     TEXT           NULL,
    short_desc      VARCHAR(500)   NULL,
    base_price      DECIMAL(15,2)  NOT NULL DEFAULT 0,
    category_id     VARCHAR(36)    NULL,
    brand_id        VARCHAR(36)    NULL,
    specs           JSON           NULL COMMENT 'Thông số kỹ thuật: chất liệu, số dây, etc.',
    is_featured     TINYINT(1)     DEFAULT 0,
    is_active       TINYINT(1)     DEFAULT 1,
    avg_rating      DECIMAL(3,2)   DEFAULT 0.00,
    review_count    INT            DEFAULT 0,
    created_at      DATETIME       DEFAULT NOW(),
    updated_at      DATETIME       DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    INDEX idx_products_slug (slug),
    INDEX idx_products_category (category_id),
    INDEX idx_products_brand (brand_id),
    INDEX idx_products_featured (is_featured),
    INDEX idx_products_price (base_price),
    INDEX idx_products_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Biến thể sản phẩm (ví dụ: màu sắc, kích thước)
CREATE TABLE IF NOT EXISTS product_variants (
    id              VARCHAR(36)    NOT NULL,
    product_id      VARCHAR(36)    NOT NULL,
    variant_name    VARCHAR(150)   NOT NULL COMMENT 'VD: Đỏ - Size M',
    sku             VARCHAR(50)    NOT NULL UNIQUE,
    price           DECIMAL(15,2)  NOT NULL DEFAULT 0,
    color           VARCHAR(50)    NULL,
    size            VARCHAR(50)    NULL,
    is_active       TINYINT(1)     DEFAULT 1,
    created_at      DATETIME       DEFAULT NOW(),
    updated_at      DATETIME       DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    INDEX idx_variants_product (product_id),
    INDEX idx_variants_sku (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Hình ảnh sản phẩm
CREATE TABLE IF NOT EXISTS product_images (
    id          VARCHAR(36)  NOT NULL,
    product_id  VARCHAR(36)  NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    alt_text    VARCHAR(255) NULL,
    sort_order  INT          DEFAULT 0,
    is_primary  TINYINT(1)   DEFAULT 0,
    created_at  DATETIME     DEFAULT NOW(),
    updated_at  DATETIME     DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    INDEX idx_images_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Đánh giá sản phẩm
CREATE TABLE IF NOT EXISTS reviews (
    id          VARCHAR(36)  NOT NULL,
    product_id  VARCHAR(36)  NOT NULL,
    user_id     VARCHAR(36)  NOT NULL,
    rating      INT          NOT NULL COMMENT '1-5 sao',
    comment     TEXT         NULL,
    is_verified TINYINT(1)   DEFAULT 0 COMMENT 'Verified purchase',
    created_at  DATETIME     DEFAULT NOW(),
    updated_at  DATETIME     DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    INDEX idx_reviews_product (product_id),
    INDEX idx_reviews_user (user_id),
    UNIQUE KEY uk_review_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
