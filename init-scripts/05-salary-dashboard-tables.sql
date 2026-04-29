-- Salary Normal Transactions (transfer/withdraw) & Login Activity tables
USE springapp;

ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS transaction_pin VARCHAR(255) DEFAULT NULL;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS transaction_pin_set BOOLEAN DEFAULT FALSE;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS password VARCHAR(255) DEFAULT NULL;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS password_set BOOLEAN DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS salary_normal_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    salary_account_id BIGINT NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DOUBLE NOT NULL,
    charge DOUBLE DEFAULT 0,
    recipient_account VARCHAR(30),
    recipient_ifsc VARCHAR(20),
    remark VARCHAR(500),
    previous_balance DOUBLE DEFAULT 0,
    new_balance DOUBLE DEFAULT 0,
    status VARCHAR(20) DEFAULT 'Success',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (salary_account_id) REFERENCES salary_accounts(id)
);

CREATE TABLE IF NOT EXISTS salary_login_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    salary_account_id BIGINT NOT NULL,
    account_number VARCHAR(30),
    activity_type VARCHAR(30) NOT NULL,
    ip_address VARCHAR(50),
    device_info VARCHAR(500),
    status VARCHAR(20) DEFAULT 'Success',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (salary_account_id) REFERENCES salary_accounts(id)
);
