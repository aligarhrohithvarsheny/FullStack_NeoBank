-- Current Account Module Tables
-- This script creates tables for Current Account Management

USE springapp;

-- Current Accounts Table
CREATE TABLE IF NOT EXISTS current_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    customer_id VARCHAR(20) UNIQUE,
    business_name VARCHAR(255) NOT NULL,
    business_type VARCHAR(50) NOT NULL,
    business_registration_number VARCHAR(100),
    gst_number VARCHAR(20),
    owner_name VARCHAR(255) NOT NULL,
    mobile VARCHAR(15) NOT NULL,
    email VARCHAR(255) NOT NULL,
    aadhar_number VARCHAR(20) NOT NULL,
    pan_number VARCHAR(15) NOT NULL,
    shop_address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    branch_name VARCHAR(255) DEFAULT 'NeoBank Main Branch',
    ifsc_code VARCHAR(20) DEFAULT 'EZYV000123',
    balance DOUBLE DEFAULT 0.0,
    overdraft_limit DOUBLE DEFAULT 0.0,
    overdraft_enabled BOOLEAN DEFAULT FALSE,
    minimum_balance DOUBLE DEFAULT 10000.0,
    status VARCHAR(20) DEFAULT 'PENDING',
    kyc_verified BOOLEAN DEFAULT FALSE,
    kyc_verified_date DATETIME,
    kyc_verified_by VARCHAR(255),
    gst_certificate_path VARCHAR(512),
    business_registration_certificate_path VARCHAR(512),
    pan_card_path VARCHAR(512),
    address_proof_path VARCHAR(512),
    account_frozen BOOLEAN DEFAULT FALSE,
    frozen_reason VARCHAR(500),
    frozen_by VARCHAR(255),
    frozen_date DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    approved_at DATETIME,
    approved_by VARCHAR(255),
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Business Transactions Table
CREATE TABLE IF NOT EXISTS business_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    txn_id VARCHAR(30) UNIQUE NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    txn_type VARCHAR(20) NOT NULL,
    amount DOUBLE NOT NULL,
    description VARCHAR(500),
    balance DOUBLE,
    status VARCHAR(20) DEFAULT 'Completed',
    recipient_account VARCHAR(20),
    recipient_name VARCHAR(255),
    transfer_type VARCHAR(10),
    charge_amount DOUBLE DEFAULT 0.0,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_number) REFERENCES current_accounts(account_number) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
