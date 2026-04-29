-- Salary Accounts & Salary Transactions tables
USE springapp;

CREATE TABLE IF NOT EXISTS salary_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_name VARCHAR(255) NOT NULL,
    dob VARCHAR(20),
    mobile_number VARCHAR(20),
    email VARCHAR(255),
    aadhar_number VARCHAR(20),
    pan_number VARCHAR(20),
    company_name VARCHAR(255),
    company_id VARCHAR(100),
    employer_address VARCHAR(500),
    hr_contact_number VARCHAR(20),
    monthly_salary DOUBLE DEFAULT 0,
    salary_credit_date INT DEFAULT 1,
    designation VARCHAR(255),
    account_number VARCHAR(30) UNIQUE,
    customer_id VARCHAR(30) UNIQUE,
    debit_card_number VARCHAR(30),
    net_banking_enabled BOOLEAN DEFAULT TRUE,
    branch_name VARCHAR(255),
    ifsc_code VARCHAR(20),
    balance DOUBLE DEFAULT 0,
    status VARCHAR(20) DEFAULT 'Active',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS salary_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    salary_account_id BIGINT,
    account_number VARCHAR(30),
    salary_amount DOUBLE,
    credit_date DATETIME,
    company_name VARCHAR(255),
    description VARCHAR(500),
    type VARCHAR(20) DEFAULT 'Credit',
    previous_balance DOUBLE DEFAULT 0,
    new_balance DOUBLE DEFAULT 0,
    status VARCHAR(20) DEFAULT 'Success',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (salary_account_id) REFERENCES salary_accounts(id)
);
