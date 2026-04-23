-- =============================================
-- Customer Engagement Service Database Schema
-- Default runtime profile: H2 (PostgreSQL mode)
-- =============================================

CREATE TABLE IF NOT EXISTS review (
    id         VARCHAR(36)   NOT NULL,
    user_id    VARCHAR(64)   NOT NULL,
    product_id VARCHAR(64)   NOT NULL,
    rating     INTEGER       NOT NULL,
    comment    VARCHAR(1000) NULL,
    created_at TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_review_user_product UNIQUE (user_id, product_id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX IF NOT EXISTS idx_review_product_created ON review (product_id, created_at);

CREATE TABLE IF NOT EXISTS wishlist_items (
    id         VARCHAR(36) NOT NULL,
    user_id    VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_wishlist_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_wishlist_user_created ON wishlist_items (user_id, created_at);

CREATE TABLE IF NOT EXISTS purchased_products (
    id         VARCHAR(36) NOT NULL,
    user_id    VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_purchased_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_purchased_user_product ON purchased_products (user_id, product_id);
