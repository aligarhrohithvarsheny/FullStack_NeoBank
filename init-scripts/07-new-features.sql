-- 07-new-features.sql
-- New feature tables: Scheduled Payments, Support Tickets, Bank Messages, Virtual Cards, Document Verification, Subscription Payments

-- =============================================
-- Scheduled Payments
-- =============================================
CREATE TABLE IF NOT EXISTS scheduled_payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL,
    recipient_account_number VARCHAR(50) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    recipient_ifsc VARCHAR(20),
    amount DOUBLE NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    frequency VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    next_payment_date DATE,
    last_payment_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_payments INT,
    completed_payments INT DEFAULT 0,
    total_amount_paid DOUBLE DEFAULT 0.0,
    failure_reason VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sp_account (account_number),
    INDEX idx_sp_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Support Tickets
-- =============================================
CREATE TABLE IF NOT EXISTS support_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id VARCHAR(50) NOT NULL UNIQUE,
    account_number VARCHAR(50) NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    user_email VARCHAR(255),
    category VARCHAR(50) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    admin_response TEXT,
    assigned_to VARCHAR(255),
    transaction_id VARCHAR(100),
    resolved_at DATETIME,
    closed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_st_account (account_number),
    INDEX idx_st_status (status),
    INDEX idx_st_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Bank Messages (Internal Messaging)
-- =============================================
CREATE TABLE IF NOT EXISTS bank_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_account_number VARCHAR(50) NOT NULL,
    recipient_email VARCHAR(255),
    message_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    is_read BOOLEAN DEFAULT FALSE,
    sender VARCHAR(50) DEFAULT 'SYSTEM',
    action_url VARCHAR(500),
    read_at DATETIME,
    expires_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_bm_account (recipient_account_number),
    INDEX idx_bm_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Virtual Debit Cards
-- =============================================
CREATE TABLE IF NOT EXISTS virtual_cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL,
    card_number VARCHAR(20) NOT NULL UNIQUE,
    cardholder_name VARCHAR(255) NOT NULL,
    cvv VARCHAR(5) NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    daily_limit DOUBLE DEFAULT 50000.0,
    monthly_limit DOUBLE DEFAULT 200000.0,
    total_spent DOUBLE DEFAULT 0.0,
    online_payments_enabled BOOLEAN DEFAULT TRUE,
    international_payments_enabled BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vc_account (account_number),
    INDEX idx_vc_card (card_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Document Verification
-- =============================================
CREATE TABLE IF NOT EXISTS document_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    user_email VARCHAR(255),
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    document_file_path VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verified_by VARCHAR(255),
    rejection_reason VARCHAR(1000),
    remarks VARCHAR(1000),
    verified_at DATETIME,
    submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_dv_account (account_number),
    INDEX idx_dv_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Subscription Payments (Employee Dashboard)
-- =============================================
CREATE TABLE IF NOT EXISTS subscription_payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    employee_name VARCHAR(255) NOT NULL,
    employee_email VARCHAR(255),
    salary_account_number VARCHAR(50) NOT NULL,
    subscription_name VARCHAR(255) NOT NULL,
    subscription_category VARCHAR(50) NOT NULL,
    amount DOUBLE NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    next_billing_date DATE,
    last_billing_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    billing_cycles_completed INT DEFAULT 0,
    total_amount_paid DOUBLE DEFAULT 0.0,
    auto_debit BOOLEAN DEFAULT TRUE,
    merchant_id VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sub_employee (employee_id),
    INDEX idx_sub_account (salary_account_number),
    INDEX idx_sub_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
