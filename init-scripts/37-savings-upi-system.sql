-- ============================================================
-- 37: Savings Account UPI System
-- Adds UPI fields to users table and creates savings_upi_transactions
-- ============================================================

-- Add UPI columns to users table
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS upi_id VARCHAR(100) UNIQUE,
    ADD COLUMN IF NOT EXISTS upi_enabled BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS transaction_pin VARCHAR(255),
    ADD COLUMN IF NOT EXISTS transaction_pin_set BOOLEAN DEFAULT FALSE;

-- Savings UPI transactions table
CREATE TABLE IF NOT EXISTS savings_upi_transactions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_ref VARCHAR(50) UNIQUE NOT NULL,
    sender_account  VARCHAR(50),
    sender_upi_id   VARCHAR(100),
    sender_name     VARCHAR(150),
    receiver_upi_id VARCHAR(100) NOT NULL,
    receiver_name   VARCHAR(150),
    receiver_account VARCHAR(50),
    amount          DECIMAL(15,2) NOT NULL,
    remark          VARCHAR(255),
    status          VARCHAR(30) DEFAULT 'SUCCESS',
    payment_method  VARCHAR(30) DEFAULT 'UPI',
    fraud_flagged   BOOLEAN DEFAULT FALSE,
    risk_score      INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sender_account  (sender_account),
    INDEX idx_sender_upi      (sender_upi_id),
    INDEX idx_receiver_upi    (receiver_upi_id),
    INDEX idx_created_at      (created_at)
);
