-- UPI QR Payments for Current Accounts
CREATE TABLE IF NOT EXISTS current_account_upi_payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    txn_id VARCHAR(50) UNIQUE NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    business_name VARCHAR(255),
    upi_id VARCHAR(100) NOT NULL,
    amount DOUBLE NOT NULL,
    payer_name VARCHAR(255),
    payer_upi VARCHAR(100),
    payment_method VARCHAR(30) DEFAULT 'UPI',
    status VARCHAR(20) DEFAULT 'SUCCESS',
    txn_type VARCHAR(10) DEFAULT 'CREDIT',
    note VARCHAR(500),
    qr_generated BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_upi_account (account_number),
    INDEX idx_upi_status (status),
    INDEX idx_upi_created (created_at)
);

-- Add UPI ID columns to current_accounts if not exist
ALTER TABLE current_accounts ADD COLUMN IF NOT EXISTS upi_id VARCHAR(100);
ALTER TABLE current_accounts ADD COLUMN IF NOT EXISTS upi_enabled BOOLEAN DEFAULT TRUE;
