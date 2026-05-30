-- =============================================
-- Verification Codes Table for Email Verification
-- =============================================

CREATE TABLE IF NOT EXISTS verification_codes (
    id          VARCHAR(36)     NOT NULL,
    email       VARCHAR(255)    NOT NULL,
    code        VARCHAR(6)      NOT NULL,
    purpose     VARCHAR(20)     NOT NULL DEFAULT 'REGISTRATION',
    expires_at  DATETIME        NOT NULL,
    is_used     TINYINT(1)     DEFAULT 0,
    is_verified TINYINT(1)     DEFAULT 0,
    created_at  DATETIME        DEFAULT NOW(),
    updated_at  DATETIME        DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    INDEX idx_verification_email_purpose (email, purpose, is_used),
    INDEX idx_verification_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
