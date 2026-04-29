-- ============================================
-- 33: EzyVault Pay - Payment Gateway System
-- ============================================

USE springapp;

-- Payment Gateway Merchants (businesses using EzyVault Pay)
CREATE TABLE IF NOT EXISTS pg_merchants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id VARCHAR(30) NOT NULL UNIQUE,
    business_name VARCHAR(100) NOT NULL,
    business_email VARCHAR(100) NOT NULL,
    business_phone VARCHAR(15),
    business_type VARCHAR(50) DEFAULT 'ONLINE',
    api_key VARCHAR(64) NOT NULL UNIQUE,
    secret_key VARCHAR(64) NOT NULL,
    webhook_url VARCHAR(500),
    callback_url VARCHAR(500),
    account_number VARCHAR(20),
    settlement_account VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    daily_limit DECIMAL(15,2) DEFAULT 500000.00,
    monthly_volume DECIMAL(15,2) DEFAULT 0.00,
    total_volume DECIMAL(15,2) DEFAULT 0.00,
    risk_score INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pg_merchant_id (merchant_id),
    INDEX idx_pg_api_key (api_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Payment Gateway Orders
CREATE TABLE IF NOT EXISTS pg_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(30) NOT NULL UNIQUE,
    merchant_id VARCHAR(30) NOT NULL,
    customer_email VARCHAR(100),
    customer_phone VARCHAR(15),
    customer_name VARCHAR(100),
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(5) DEFAULT 'INR',
    description VARCHAR(255),
    status VARCHAR(20) DEFAULT 'CREATED',
    payment_method VARCHAR(30),
    receipt VARCHAR(50),
    notes TEXT,
    expires_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pg_order_id (order_id),
    INDEX idx_pg_order_merchant (merchant_id),
    INDEX idx_pg_order_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Payment Gateway Transactions
CREATE TABLE IF NOT EXISTS pg_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(30) NOT NULL UNIQUE,
    order_id VARCHAR(30) NOT NULL,
    merchant_id VARCHAR(30) NOT NULL,
    payer_account VARCHAR(20),
    payer_name VARCHAR(100),
    amount DECIMAL(15,2) NOT NULL,
    fee DECIMAL(10,2) DEFAULT 0.00,
    tax DECIMAL(10,2) DEFAULT 0.00,
    net_amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(5) DEFAULT 'INR',
    payment_method VARCHAR(30) NOT NULL,
    status VARCHAR(20) DEFAULT 'INITIATED',
    signature VARCHAR(128),
    signature_verified BOOLEAN DEFAULT FALSE,
    error_code VARCHAR(20),
    error_description VARCHAR(255),
    risk_score INT DEFAULT 0,
    fraud_flagged BOOLEAN DEFAULT FALSE,
    ip_address VARCHAR(45),
    device_info VARCHAR(255),
    settled BOOLEAN DEFAULT FALSE,
    settled_at DATETIME,
    refund_status VARCHAR(20),
    refunded_amount DECIMAL(15,2) DEFAULT 0.00,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pg_txn_id (transaction_id),
    INDEX idx_pg_txn_order (order_id),
    INDEX idx_pg_txn_merchant (merchant_id),
    INDEX idx_pg_txn_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Payment Gateway Refunds
CREATE TABLE IF NOT EXISTS pg_refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_id VARCHAR(30) NOT NULL UNIQUE,
    transaction_id VARCHAR(30) NOT NULL,
    order_id VARCHAR(30) NOT NULL,
    merchant_id VARCHAR(30) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(20) DEFAULT 'INITIATED',
    refund_type VARCHAR(20) DEFAULT 'FULL',
    processed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pg_refund_txn (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Payment Gateway Webhooks Log
CREATE TABLE IF NOT EXISTS pg_webhook_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    webhook_id VARCHAR(30) NOT NULL UNIQUE,
    merchant_id VARCHAR(30) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    order_id VARCHAR(30),
    transaction_id VARCHAR(30),
    payload TEXT,
    response_status INT,
    response_body TEXT,
    delivered BOOLEAN DEFAULT FALSE,
    retry_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pg_webhook_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Payment Gateway Fraud Rules
CREATE TABLE IF NOT EXISTS pg_fraud_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    threshold_value DECIMAL(15,2),
    time_window_minutes INT,
    max_count INT,
    action_type VARCHAR(20) DEFAULT 'FLAG',
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default fraud rules
INSERT INTO pg_fraud_rules (rule_name, rule_type, threshold_value, time_window_minutes, max_count, action_type) VALUES
('High Amount Transaction', 'AMOUNT_THRESHOLD', 100000.00, NULL, NULL, 'FLAG'),
('Rapid Transactions', 'VELOCITY', NULL, 1, 5, 'BLOCK'),
('Daily Volume Limit', 'DAILY_VOLUME', 500000.00, 1440, NULL, 'BLOCK'),
('Suspicious Amount Pattern', 'AMOUNT_PATTERN', 99999.00, 60, 3, 'FLAG'),
('New Device Transaction', 'NEW_DEVICE', 50000.00, NULL, NULL, 'VERIFY');

-- Payment Gateway Settlement Batches
CREATE TABLE IF NOT EXISTS pg_settlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_id VARCHAR(30) NOT NULL UNIQUE,
    merchant_id VARCHAR(30) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    total_fees DECIMAL(10,2) DEFAULT 0.00,
    net_amount DECIMAL(15,2) NOT NULL,
    transaction_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    settlement_date DATE,
    processed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pg_settlement_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
