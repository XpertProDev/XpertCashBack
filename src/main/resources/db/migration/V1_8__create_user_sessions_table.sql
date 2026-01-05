-- Migration pour créer la table user_sessions
-- Permet de gérer les sessions multiples par utilisateur/appareil

CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_uuid VARCHAR(36) NOT NULL,
    session_token VARCHAR(500) NULL,
    device_id VARCHAR(255),
    device_name VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at DATETIME NOT NULL,
    last_activity DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_uuid (user_uuid),
    INDEX idx_session_token (session_token),
    INDEX idx_device_id (device_id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_user_device (user_uuid, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


