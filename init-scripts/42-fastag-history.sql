-- FASTag password login attempts and administrator detail edit history
CREATE TABLE IF NOT EXISTS fastag_login_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gmail_id VARCHAR(255) NOT NULL,
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50),
    login_method VARCHAR(100),
    failure_reason VARCHAR(1000),
    ip_address VARCHAR(100),
    device_info TEXT,
    INDEX idx_fastag_login_history_gmail (gmail_id),
    INDEX idx_fastag_login_history_time (login_time)
);

CREATE TABLE IF NOT EXISTS fasttag_edit_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fasttag_id BIGINT NOT NULL,
    fasttag_number VARCHAR(100),
    changed_by VARCHAR(255),
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    before_values TEXT,
    after_values TEXT,
    INDEX idx_fasttag_edit_history_tag (fasttag_id),
    INDEX idx_fasttag_edit_history_time (changed_at)
);
