-- Cheque Leaf Allocation System
-- Each salary account user gets 30 unique cheque leaves
-- Serial numbers are globally unique across all users

CREATE TABLE IF NOT EXISTS cheque_leaves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    salary_account_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    leaf_number VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    used_cheque_request_id BIGINT NULL,
    allocated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at DATETIME NULL,
    INDEX idx_cheque_leaves_account (salary_account_id),
    INDEX idx_cheque_leaves_status (salary_account_id, status),
    INDEX idx_cheque_leaves_leaf (leaf_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
