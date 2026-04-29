-- ================================================
-- 34: PAYMENT GATEWAY LOGIN & ADMIN ACCESS SYSTEM
-- ================================================

USE springapp;

-- PG Merchant access control (admin approval)
ALTER TABLE pg_merchants ADD COLUMN IF NOT EXISTS login_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE pg_merchants ADD COLUMN IF NOT EXISTS admin_approved BOOLEAN DEFAULT FALSE;
ALTER TABLE pg_merchants ADD COLUMN IF NOT EXISTS approved_by VARCHAR(100) DEFAULT NULL;
ALTER TABLE pg_merchants ADD COLUMN IF NOT EXISTS approved_at DATETIME DEFAULT NULL;
ALTER TABLE pg_merchants ADD COLUMN IF NOT EXISTS linked_account_number VARCHAR(20) DEFAULT NULL;
ALTER TABLE pg_merchants ADD COLUMN IF NOT EXISTS linked_account_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE pg_merchants ADD COLUMN IF NOT EXISTS linked_account_holder_name VARCHAR(200) DEFAULT NULL;
ALTER TABLE pg_merchants ADD COLUMN IF NOT EXISTS linked_account_type VARCHAR(50) DEFAULT 'CURRENT';
ALTER TABLE pg_merchants ADD COLUMN IF NOT EXISTS registration_status VARCHAR(30) DEFAULT 'PENDING';
ALTER TABLE pg_merchants ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500) DEFAULT NULL;

-- PG payment sessions for QR code + UPI payments
CREATE TABLE IF NOT EXISTS pg_payment_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(50) UNIQUE NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    merchant_id VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    customer_name VARCHAR(200),
    customer_email VARCHAR(200),
    customer_phone VARCHAR(20),
    payer_name VARCHAR(200),
    payer_verified BOOLEAN DEFAULT FALSE,
    payer_account VARCHAR(20),
    payment_method VARCHAR(30) DEFAULT 'UPI',
    upi_id VARCHAR(100),
    qr_data TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    name_match_score DECIMAL(5,2) DEFAULT 0,
    expires_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pg_session_id (session_id),
    INDEX idx_pg_session_order (order_id),
    INDEX idx_pg_session_merchant (merchant_id),
    INDEX idx_pg_session_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- PG settlement ledger (real-time credit tracking)
CREATE TABLE IF NOT EXISTS pg_settlement_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ledger_id VARCHAR(50) UNIQUE NOT NULL,
    merchant_id VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    gross_amount DECIMAL(15,2) NOT NULL,
    fee_amount DECIMAL(15,2) NOT NULL,
    tax_amount DECIMAL(15,2) NOT NULL,
    net_amount DECIMAL(15,2) NOT NULL,
    credit_account VARCHAR(20) NOT NULL,
    credit_status VARCHAR(20) DEFAULT 'PENDING',
    credited_at DATETIME,
    balance_before DECIMAL(15,2),
    balance_after DECIMAL(15,2),
    reference_note VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pg_ledger_merchant (merchant_id),
    INDEX idx_pg_ledger_credit_status (credit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
