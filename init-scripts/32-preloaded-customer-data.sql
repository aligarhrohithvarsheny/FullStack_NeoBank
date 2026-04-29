-- Preloaded Customer Data: Manager uploads Excel with customer details for admin auto-fill
CREATE TABLE IF NOT EXISTS preloaded_customer_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Identity (lookup keys)
    aadhar_number VARCHAR(12) NOT NULL,
    pan_number VARCHAR(10),
    
    -- Account type target
    account_type VARCHAR(20) DEFAULT 'Savings', -- Savings, Current, Salary
    
    -- Personal Details
    full_name VARCHAR(100),
    date_of_birth VARCHAR(20),
    age INT,
    gender VARCHAR(10),
    occupation VARCHAR(100),
    income DOUBLE,
    
    -- Contact Details
    phone VARCHAR(15),
    email VARCHAR(100),
    address TEXT,
    city VARCHAR(50),
    state VARCHAR(50),
    pincode VARCHAR(10),
    
    -- Business Details (Current Account)
    business_name VARCHAR(200),
    business_type VARCHAR(50),
    business_registration_number VARCHAR(50),
    gst_number VARCHAR(20),
    shop_address TEXT,
    
    -- Salary Details (Salary Account)
    company_name VARCHAR(200),
    company_id VARCHAR(50),
    designation VARCHAR(100),
    monthly_salary DOUBLE,
    salary_credit_date INT,
    hr_contact_number VARCHAR(15),
    employer_address TEXT,
    
    -- Bank Details
    branch_name VARCHAR(100) DEFAULT 'NeoBank Main Branch',
    ifsc_code VARCHAR(20) DEFAULT 'EZYV000123',
    
    -- Upload tracking
    uploaded_by VARCHAR(100),
    upload_batch_id VARCHAR(50),
    upload_file_name VARCHAR(200),
    used BOOLEAN DEFAULT FALSE,
    used_by VARCHAR(100),
    used_at DATETIME,
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_aadhar (aadhar_number),
    INDEX idx_pan (pan_number),
    INDEX idx_batch (upload_batch_id),
    INDEX idx_used (used)
);
