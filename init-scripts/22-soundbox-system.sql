-- =====================================================
-- SOUNDBOX PAYMENT SYSTEM
-- NeoBank Current Account Soundbox Integration
-- =====================================================

-- 1. Soundbox Devices Table
CREATE TABLE IF NOT EXISTS soundbox_devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(30) UNIQUE NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    business_name VARCHAR(255),
    owner_name VARCHAR(255),
    status VARCHAR(20) DEFAULT 'INACTIVE',
    voice_enabled BOOLEAN DEFAULT TRUE,
    voice_language VARCHAR(20) DEFAULT 'en-IN',
    volume_mode VARCHAR(20) DEFAULT 'NORMAL',
    linked_upi VARCHAR(255),
    monthly_charge DOUBLE DEFAULT 100.0,
    device_charge DOUBLE DEFAULT 499.0,
    charge_status VARCHAR(20) DEFAULT 'PENDING',
    last_active_at DATETIME,
    activated_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_number) REFERENCES current_accounts(account_number)
);

-- 2. Soundbox Requests Table
CREATE TABLE IF NOT EXISTS soundbox_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(30) UNIQUE NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    business_name VARCHAR(255) NOT NULL,
    owner_name VARCHAR(255) NOT NULL,
    delivery_address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    mobile VARCHAR(15),
    status VARCHAR(20) DEFAULT 'PENDING',
    admin_remarks VARCHAR(500),
    assigned_device_id VARCHAR(30),
    monthly_charge DOUBLE DEFAULT 100.0,
    device_charge DOUBLE DEFAULT 499.0,
    processed_by VARCHAR(255),
    processed_at DATETIME,
    requested_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_number) REFERENCES current_accounts(account_number)
);

-- 3. Soundbox Transactions (UPI/QR payment records)
CREATE TABLE IF NOT EXISTS soundbox_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    txn_id VARCHAR(30) UNIQUE NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    device_id VARCHAR(30),
    amount DOUBLE NOT NULL,
    txn_type VARCHAR(20) NOT NULL DEFAULT 'CREDIT',
    payment_method VARCHAR(20) NOT NULL DEFAULT 'UPI',
    payer_name VARCHAR(255),
    payer_upi VARCHAR(255),
    status VARCHAR(20) DEFAULT 'SUCCESS',
    voice_played BOOLEAN DEFAULT FALSE,
    voice_message VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_number) REFERENCES current_accounts(account_number),
    FOREIGN KEY (device_id) REFERENCES soundbox_devices(device_id)
);

-- Indexes for performance
CREATE INDEX idx_soundbox_devices_account ON soundbox_devices(account_number);
CREATE INDEX idx_soundbox_devices_status ON soundbox_devices(status);
CREATE INDEX idx_soundbox_requests_account ON soundbox_requests(account_number);
CREATE INDEX idx_soundbox_requests_status ON soundbox_requests(status);
CREATE INDEX idx_soundbox_txn_account ON soundbox_transactions(account_number);
CREATE INDEX idx_soundbox_txn_device ON soundbox_transactions(device_id);
CREATE INDEX idx_soundbox_txn_date ON soundbox_transactions(created_at);
