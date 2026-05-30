-- V4: Fix face_data.embedding column type for MariaDB/Hibernate compatibility
-- Changes LONGTEXT to JSON type to match entity definition

ALTER TABLE face_data MODIFY COLUMN embedding JSON NOT NULL;
