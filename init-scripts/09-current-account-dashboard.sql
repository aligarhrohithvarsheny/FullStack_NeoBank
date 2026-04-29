-- Current Account Dashboard Module Tables
-- Beneficiaries, Cheque Book Requests, Vendor Payments

USE springapp;

-- Current Account Beneficiaries
CREATE TABLE IF NOT EXISTS current_account_beneficiaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL,
    beneficiary_name VARCHAR(255) NOT NULL,
    beneficiary_account VARCHAR(20) NOT NULL,
    beneficiary_ifsc VARCHAR(20) NOT NULL,
    beneficiary_bank VARCHAR(255),
    nick_name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_number) REFERENCES current_accounts(account_number) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Current Account Cheque Book Requests
CREATE TABLE IF NOT EXISTS current_account_cheque_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(20) UNIQUE NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    leaves INT DEFAULT 25,
    delivery_address TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    requested_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    approved_at DATETIME,
    approved_by VARCHAR(255),
    FOREIGN KEY (account_number) REFERENCES current_accounts(account_number) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Current Account Vendor Payments
CREATE TABLE IF NOT EXISTS current_account_vendor_payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL,
    vendor_name VARCHAR(255) NOT NULL,
    vendor_account VARCHAR(20) NOT NULL,
    vendor_ifsc VARCHAR(20),
    amount DOUBLE NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',
    paid_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_number) REFERENCES current_accounts(account_number) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
