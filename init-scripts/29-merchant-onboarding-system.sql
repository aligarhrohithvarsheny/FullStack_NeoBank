-- =====================================================
-- MERCHANT ONBOARDING SYSTEM
-- =====================================================

-- 1. Agents table
CREATE TABLE IF NOT EXISTS agents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    mobile VARCHAR(15) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    region VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Merchants table
CREATE TABLE IF NOT EXISTS merchants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id VARCHAR(50) UNIQUE NOT NULL,
    business_name VARCHAR(200) NOT NULL,
    owner_name VARCHAR(100) NOT NULL,
    mobile VARCHAR(15) NOT NULL,
    email VARCHAR(100),
    business_type VARCHAR(50) NOT NULL,
    gst_number VARCHAR(20),
    shop_address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    ifsc_code VARCHAR(15) NOT NULL,
    account_holder_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    agent_id VARCHAR(50) NOT NULL,
    shop_photo_path VARCHAR(500),
    owner_id_proof_path VARCHAR(500),
    bank_proof_path VARCHAR(500),
    rejection_reason TEXT,
    activated_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant_status (status),
    INDEX idx_merchant_agent (agent_id),
    INDEX idx_merchant_mobile (mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Merchant Applications table
CREATE TABLE IF NOT EXISTS merchant_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id VARCHAR(50) UNIQUE NOT NULL,
    merchant_id VARCHAR(50) NOT NULL,
    device_type VARCHAR(30) NOT NULL,
    device_quantity INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'PENDING',
    agent_id VARCHAR(50) NOT NULL,
    admin_remarks TEXT,
    processed_by VARCHAR(100),
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_app_status (status),
    INDEX idx_app_merchant (merchant_id),
    INDEX idx_app_agent (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Merchant Devices table
CREATE TABLE IF NOT EXISTS merchant_devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(50) UNIQUE NOT NULL,
    device_type VARCHAR(30) NOT NULL,
    merchant_id VARCHAR(50) NOT NULL,
    application_id VARCHAR(50),
    status VARCHAR(20) DEFAULT 'INACTIVE',
    activated_at TIMESTAMP NULL,
    last_active_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_device_merchant (merchant_id),
    INDEX idx_device_type (device_type),
    INDEX idx_device_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Merchant Transactions table
CREATE TABLE IF NOT EXISTS merchant_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(50) UNIQUE NOT NULL,
    merchant_id VARCHAR(50) NOT NULL,
    device_id VARCHAR(50),
    amount DECIMAL(15,2) NOT NULL,
    payment_mode VARCHAR(30),
    payer_name VARCHAR(100),
    payer_upi VARCHAR(100),
    status VARCHAR(20) DEFAULT 'SUCCESS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_txn_merchant (merchant_id),
    INDEX idx_txn_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert a default agent for testing
INSERT IGNORE INTO agents (agent_id, name, email, mobile, password, status, region)
VALUES ('AGT1000001', 'Demo Agent', 'agent@neobank.com', '9876543210', 'agent123', 'ACTIVE', 'Mumbai');
