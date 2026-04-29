-- Net Banking Service Control: Admin can toggle ON/OFF net banking for Savings & Current accounts
-- Timestamps are recorded for every status change

CREATE TABLE IF NOT EXISTS net_banking_service_control (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_type VARCHAR(50) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Audit log for every toggle change with timestamp
CREATE TABLE IF NOT EXISTS net_banking_service_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_type VARCHAR(50) NOT NULL,
    old_status BOOLEAN NOT NULL,
    new_status BOOLEAN NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remarks VARCHAR(500),
    account_number VARCHAR(50),
    customer_name VARCHAR(100)
);

-- Seed default values (both ON by default)
INSERT INTO net_banking_service_control (service_type, enabled, updated_by)
VALUES ('SAVINGS_ACCOUNT', TRUE, 'SYSTEM')
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO net_banking_service_control (service_type, enabled, updated_by)
VALUES ('CURRENT_ACCOUNT', TRUE, 'SYSTEM')
ON DUPLICATE KEY UPDATE id=id;

-- Per-customer net banking control columns on accounts table
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS net_banking_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS net_banking_toggled_by VARCHAR(100);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS net_banking_toggled_at TIMESTAMP NULL;

-- Per-customer net banking control columns on current_accounts table
ALTER TABLE current_accounts ADD COLUMN IF NOT EXISTS net_banking_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE current_accounts ADD COLUMN IF NOT EXISTS net_banking_toggled_by VARCHAR(100);
ALTER TABLE current_accounts ADD COLUMN IF NOT EXISTS net_banking_toggled_at TIMESTAMP NULL;
