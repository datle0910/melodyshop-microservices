-- V3: Face Recognition Data
-- Stores 128-D face embedding vectors for face login functionality.

CREATE TABLE IF NOT EXISTS face_data (
    id              VARCHAR(36)     NOT NULL,
    user_id         VARCHAR(36)     NOT NULL,
    -- 128-D embedding stored as JSON array
    embedding        JSON            NOT NULL,
    -- Image quality score at registration time (0-100)
    quality_score   TINYINT UNSIGNED NULL,
    -- Thumbnail image for display (optional, small base64 JPEG)
    thumbnail        MEDIUMBLOB      NULL,
    is_active       TINYINT(1)     DEFAULT 1,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_face_data_user_id (user_id),
    CONSTRAINT fk_face_data_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_face_data_user_id ON face_data(user_id);
CREATE INDEX idx_face_data_is_active ON face_data(is_active);
