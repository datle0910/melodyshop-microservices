-- =============================================
-- User Service Database Schema
-- Database: user_db
-- =============================================

CREATE TABLE IF NOT EXISTS user_profiles (
    id         VARCHAR(36)     NOT NULL COMMENT 'Same as auth_db.users.id',
    full_name  VARCHAR(150) NOT NULL,
    phone      VARCHAR(20)  NULL,
    avatar_url VARCHAR(500) NULL,
    created_at DATETIME     DEFAULT NOW(),
    updated_at DATETIME     DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_addresses (
    id             VARCHAR(36)     NOT NULL,
    user_id        VARCHAR(36)     NOT NULL,
    full_name      VARCHAR(150) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    province       VARCHAR(100) NOT NULL,
    district       VARCHAR(100) NOT NULL,
    ward           VARCHAR(100) NOT NULL,
    address_detail VARCHAR(255) NOT NULL,
    is_default     TINYINT(1)   DEFAULT 0,
    created_at     DATETIME     DEFAULT NOW(),
    updated_at     DATETIME     DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    INDEX idx_addresses_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
