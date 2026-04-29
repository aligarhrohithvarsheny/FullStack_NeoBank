-- Current Account: Invoice Generation, Business Loans, Multi-User Access

USE springapp;

-- ==================== INVOICE GENERATION ====================
CREATE TABLE IF NOT EXISTS current_account_invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(30) UNIQUE NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    client_email VARCHAR(255),
    client_phone VARCHAR(20),
    client_address TEXT,
    client_gst VARCHAR(20),
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    items_json TEXT NOT NULL,
    subtotal DOUBLE NOT NULL DEFAULT 0,
    tax_rate DOUBLE DEFAULT 18,
    tax_amount DOUBLE NOT NULL DEFAULT 0,
    discount DOUBLE DEFAULT 0,
    total_amount DOUBLE NOT NULL DEFAULT 0,
    notes TEXT,
    terms TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT',
    paid_amount DOUBLE DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_number) REFERENCES current_accounts(account_number) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== BUSINESS LOAN APPLICATION ====================
CREATE TABLE IF NOT EXISTS current_account_business_loans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id VARCHAR(30) UNIQUE NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    loan_type VARCHAR(50) NOT NULL,
    pan_number VARCHAR(15) NOT NULL,
    requested_amount DOUBLE NOT NULL,
    approved_amount DOUBLE,
    interest_rate DOUBLE,
    tenure_months INT NOT NULL,
    monthly_emi DOUBLE,
    purpose TEXT NOT NULL,
    annual_revenue DOUBLE NOT NULL,
    years_in_business INT NOT NULL,
    cibil_score INT,
    cibil_status VARCHAR(20),
    business_name VARCHAR(255),
    owner_name VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING',
    rejection_reason TEXT,
    applied_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME,
    processed_by VARCHAR(255),
    disbursed_at DATETIME,
    FOREIGN KEY (account_number) REFERENCES current_accounts(account_number) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== MULTI-USER ACCESS ====================
CREATE TABLE IF NOT EXISTS current_account_business_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(20) UNIQUE NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    mobile VARCHAR(15) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    password VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_login DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    FOREIGN KEY (account_number) REFERENCES current_accounts(account_number) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
