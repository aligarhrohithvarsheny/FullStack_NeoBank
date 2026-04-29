-- Employee Dashboard Features: UPI, Auto Savings, Salary Advance, Loan Eligibility, Debit Card, FASTag
USE springapp;

-- Add new columns to salary_accounts for employee dashboard features
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS address VARCHAR(500) DEFAULT NULL;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS upi_id VARCHAR(100) DEFAULT NULL;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS upi_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS auto_savings_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS auto_savings_percentage DOUBLE DEFAULT 10;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS savings_balance DOUBLE DEFAULT 0;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS debit_card_status VARCHAR(20) DEFAULT 'Active';
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS daily_limit DOUBLE DEFAULT 50000;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS international_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS online_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS contactless_enabled BOOLEAN DEFAULT TRUE;

-- UPI Transactions table
CREATE TABLE IF NOT EXISTS salary_upi_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    salary_account_id BIGINT NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    upi_id VARCHAR(100),
    recipient_upi VARCHAR(100),
    recipient_name VARCHAR(255),
    type VARCHAR(20) NOT NULL,
    amount DOUBLE NOT NULL,
    remark VARCHAR(500),
    status VARCHAR(20) DEFAULT 'Success',
    transaction_ref VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (salary_account_id) REFERENCES salary_accounts(id)
);

-- Salary Advance Requests
CREATE TABLE IF NOT EXISTS salary_advance_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    salary_account_id BIGINT NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    employee_name VARCHAR(255),
    monthly_salary DOUBLE,
    advance_amount DOUBLE NOT NULL,
    advance_limit DOUBLE,
    reason VARCHAR(500),
    status VARCHAR(20) DEFAULT 'Pending',
    approved_at DATETIME,
    repaid BOOLEAN DEFAULT FALSE,
    repaid_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (salary_account_id) REFERENCES salary_accounts(id)
);

-- Fraud Alerts
CREATE TABLE IF NOT EXISTS salary_fraud_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    salary_account_id BIGINT NOT NULL,
    account_number VARCHAR(30),
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) DEFAULT 'Medium',
    description VARCHAR(500),
    amount DOUBLE,
    location VARCHAR(255),
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (salary_account_id) REFERENCES salary_accounts(id)
);
