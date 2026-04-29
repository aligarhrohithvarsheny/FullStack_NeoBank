-- FASTag Login System - OTP-based Gmail login
CREATE TABLE IF NOT EXISTS fastag_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gmail_id VARCHAR(255) NOT NULL UNIQUE,
    otp VARCHAR(6),
    otp_expiry DATETIME,
    otp_attempts INT DEFAULT 0,
    is_verified BOOLEAN DEFAULT FALSE,
    session_token VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME
);
