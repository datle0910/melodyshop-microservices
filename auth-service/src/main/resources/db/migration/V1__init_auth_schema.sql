-- =============================================
-- Auth Service Database Schema
-- Database: auth_db
-- =============================================

CREATE TABLE IF NOT EXISTS roles (
    id          VARCHAR(36)     NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NULL,
    is_system   TINYINT(1)   DEFAULT 0,
    created_at  DATETIME     DEFAULT NOW(),
    updated_at  DATETIME     DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    id             VARCHAR(36)     NOT NULL,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(150) NOT NULL,
    phone          VARCHAR(20)  NULL,
    avatar_url     VARCHAR(500) NULL,
    is_active      TINYINT(1)   DEFAULT 1,
    is_verified    TINYINT(1)   DEFAULT 0,
    loyalty_points INT          DEFAULT 0,
    created_at     DATETIME     DEFAULT NOW(),
    updated_at     DATETIME     DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permissions (
    id       VARCHAR(36)     NOT NULL,
    role_id  VARCHAR(36)     NOT NULL,
    resource VARCHAR(100) NOT NULL,
    action   VARCHAR(50)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id     VARCHAR(36) NOT NULL,
    role_id     VARCHAR(36) NOT NULL,
    assigned_at DATETIME DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          VARCHAR(36)     NOT NULL,
    user_id     VARCHAR(36)     NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    device_info VARCHAR(255) NULL,
    ip_address  VARCHAR(45)  NULL,
    is_revoked  TINYINT(1)   DEFAULT 0,
    expires_at  DATETIME     NOT NULL,
    created_at  DATETIME     DEFAULT NOW(),
    updated_at  DATETIME     DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default roles
INSERT INTO roles (id, name, description, is_system) VALUES
(UUID(), 'ROLE_GUEST', 'Khách vãng lai', 1),
(UUID(), 'ROLE_CUSTOMER', 'Khách hàng đã đăng ký', 1),
(UUID(), 'ROLE_ADMIN', 'Quản trị viên', 1);
