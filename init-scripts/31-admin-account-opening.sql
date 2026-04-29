-- Admin Account Opening System (Without Video KYC)
-- Supports Savings, Current, and Salary account types opened by admin

USE springapp;

CREATE TABLE IF NOT EXISTS admin_account_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Application tracking
    application_number VARCHAR(20) UNIQUE NOT NULL,
    account_type VARCHAR(20) NOT NULL DEFAULT 'Savings', -- Savings, Current, Salary
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, DOCUMENTS_UPLOADED, ADMIN_VERIFIED, MANAGER_APPROVED, MANAGER_REJECTED, ACTIVE, CLOSED
    
    -- Personal / Owner Details
    full_name VARCHAR(100) NOT NULL,
    date_of_birth VARCHAR(20),
    age INT,
    gender VARCHAR(10),
    occupation VARCHAR(100),
    income DOUBLE,
    phone VARCHAR(15) NOT NULL,
    email VARCHAR(100),
    address TEXT,
    city VARCHAR(50),
    state VARCHAR(50),
    pincode VARCHAR(10),
    
    -- Identity Documents
    aadhar_number VARCHAR(12) NOT NULL,
    pan_number VARCHAR(10) NOT NULL,
    
    -- Business Details (for Current accounts)
    business_name VARCHAR(150),
    business_type VARCHAR(50),
    business_registration_number VARCHAR(50),
    gst_number VARCHAR(20),
    shop_address TEXT,
    
    -- Salary Details (for Salary accounts)
    company_name VARCHAR(150),
    company_id VARCHAR(50),
    designation VARCHAR(100),
    monthly_salary DOUBLE,
    salary_credit_date INT,
    employer_address TEXT,
    hr_contact_number VARCHAR(15),
    
    -- Bank Details
    branch_name VARCHAR(100) DEFAULT 'NeoBank Main Branch',
    ifsc_code VARCHAR(20) DEFAULT 'EZYV000123',
    account_number VARCHAR(20) UNIQUE,
    customer_id VARCHAR(20) UNIQUE,
    
    -- Verification
    admin_verified BOOLEAN DEFAULT FALSE,
    admin_verified_by VARCHAR(100),
    admin_verified_date DATETIME,
    admin_remarks TEXT,
    
    -- Manager Approval
    manager_approved BOOLEAN DEFAULT FALSE,
    manager_approved_by VARCHAR(100),
    manager_approved_date DATETIME,
    manager_remarks TEXT,
    
    -- Document Paths
    application_pdf_path VARCHAR(500),
    signed_application_path VARCHAR(500),
    additional_documents_path VARCHAR(500),
    
    -- Signatures & Declaration
    applicant_signature_path VARCHAR(500),
    bank_officer_signature VARCHAR(100),
    declaration_accepted BOOLEAN DEFAULT FALSE,
    declaration_date DATETIME,
    
    -- Audit
    created_by VARCHAR(100) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for faster lookups
CREATE INDEX idx_admin_app_status ON admin_account_applications(status);
CREATE INDEX idx_admin_app_type ON admin_account_applications(account_type);
CREATE INDEX idx_admin_app_aadhar ON admin_account_applications(aadhar_number);
CREATE INDEX idx_admin_app_pan ON admin_account_applications(pan_number);
CREATE INDEX idx_admin_app_created_by ON admin_account_applications(created_by);
