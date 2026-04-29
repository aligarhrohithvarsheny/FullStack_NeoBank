-- CIBIL Report Management System
CREATE TABLE IF NOT EXISTS cibil_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pan_number VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    salary DOUBLE,
    cibil_score INT,
    approval_limit DOUBLE,
    status VARCHAR(30) DEFAULT 'PENDING',
    remarks TEXT,

    -- Cross-reference account info
    savings_account_number VARCHAR(50),
    salary_account_number VARCHAR(50),
    current_account_number VARCHAR(50),
    savings_balance DOUBLE,
    salary_balance DOUBLE,
    current_balance DOUBLE,

    -- ML analysis results
    risk_score DOUBLE,
    risk_category VARCHAR(20),
    debt_to_income_ratio DOUBLE,
    recommended_limit DOUBLE,
    eligibility_reason TEXT,

    -- Upload tracking
    uploaded_by VARCHAR(255) NOT NULL,
    upload_batch_id VARCHAR(100),
    upload_file_name VARCHAR(500),
    upload_type VARCHAR(20),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_pan_number (pan_number),
    INDEX idx_uploaded_by (uploaded_by),
    INDEX idx_batch_id (upload_batch_id),
    INDEX idx_status (status),
    INDEX idx_cibil_score (cibil_score)
);
