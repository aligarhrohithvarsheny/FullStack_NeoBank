# Database Schema Reference - Railway MySQL

This document provides the complete database schema for the NeoBank application.

---

## Tables

### 1. admins

```sql
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255) NOT NULL,  -- BCrypt: 60 characters, starts with $2a$
    role VARCHAR(50) NOT NULL,  -- 'ADMIN' or 'MANAGER'
    pan VARCHAR(255),
    employee_id VARCHAR(255),
    address TEXT,
    aadhar_number VARCHAR(255),
    mobile_number VARCHAR(255),
    qualifications VARCHAR(255),
    profile_complete BOOLEAN DEFAULT FALSE,
    failed_login_attempts INT DEFAULT 0,
    account_locked BOOLEAN DEFAULT FALSE,
    last_failed_login_time DATETIME,
    created_at DATETIME,
    last_updated DATETIME
);
```

### 2. users

```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    account_number VARCHAR(255) UNIQUE,
    status VARCHAR(50) DEFAULT 'PENDING',  -- PENDING, APPROVED, CLOSED
    failed_login_attempts INT DEFAULT 0,
    account_locked BOOLEAN DEFAULT FALSE,
    last_failed_login_time DATETIME,
    join_date DATETIME,
    profile_photo LONGBLOB,
    profile_photo_type VARCHAR(255),
    profile_photo_name VARCHAR(255),
    signature LONGBLOB,
    signature_type VARCHAR(255),
    signature_name VARCHAR(255),
    signature_status VARCHAR(50) DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED
    signature_submitted_date DATETIME,
    signature_reviewed_date DATETIME,
    signature_reviewed_by VARCHAR(255),
    signature_rejection_reason TEXT,
    graphical_password TEXT,  -- JSON array of image IDs
    account_id BIGINT,
    parent_user_id BIGINT,
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (parent_user_id) REFERENCES users(id)
);
```

### 3. accounts

```sql
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    phone VARCHAR(255) UNIQUE,
    email VARCHAR(255),
    balance DECIMAL(19,2) DEFAULT 0.00,
    account_type VARCHAR(50),  -- SAVINGS, CURRENT, etc.
    aadhar_number VARCHAR(255),
    pan VARCHAR(255),
    address TEXT,
    dob DATE,
    occupation VARCHAR(255),
    income DECIMAL(19,2),
    created_at DATETIME,
    last_updated DATETIME
);
```

### 4. loans

```sql
CREATE TABLE IF NOT EXISTS loans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(255),  -- Personal Loan, Education Loan, Home Loan, Car Loan
    amount DECIMAL(19,2),
    tenure INT,  -- in months
    interest_rate DECIMAL(5,2),
    purpose VARCHAR(255),
    status VARCHAR(50) DEFAULT 'Pending',  -- Pending, Approved, Rejected, Foreclosed, Paid
    
    -- User information
    user_name VARCHAR(255),
    user_email VARCHAR(255),
    account_number VARCHAR(255),
    current_balance DECIMAL(19,2),
    pan VARCHAR(255),
    child_account_number VARCHAR(255),  -- For education loans
    
    -- Personal loan form
    personal_loan_form_path VARCHAR(500),
    
    -- CIBIL information
    cibil_score INT,
    credit_limit DECIMAL(19,2),
    
    -- Loan account information
    loan_account_number VARCHAR(255) UNIQUE,
    application_date DATETIME,
    approval_date DATETIME,
    emi_start_date DATE,
    approved_by VARCHAR(255),
    
    -- Foreclosure information
    foreclosure_date DATETIME,
    foreclosure_amount DECIMAL(19,2),
    foreclosure_charges DECIMAL(19,2),
    foreclosure_gst DECIMAL(19,2),
    principal_paid DECIMAL(19,2),
    interest_paid DECIMAL(19,2),
    remaining_principal DECIMAL(19,2),
    remaining_interest DECIMAL(19,2),
    foreclosed_by VARCHAR(255)
);
```

---

## Indexes (Recommended)

```sql
-- Admin indexes
CREATE INDEX idx_admin_email ON admins(email);
CREATE INDEX idx_admin_role ON admins(role);

-- User indexes
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_account_number ON users(account_number);

-- Loan indexes
CREATE INDEX idx_loan_account_number ON loans(account_number);
CREATE INDEX idx_loan_status ON loans(status);
CREATE INDEX idx_loan_type ON loans(type);
CREATE INDEX idx_loan_application_date ON loans(application_date);
```

---

## Sample Data Insert

### Insert Manager (with BCrypt password)

**First, generate BCrypt hash using Java:**

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
String hash = encoder.encode("manager123");
System.out.println(hash);
```

**Then insert:**

```sql
INSERT INTO admins (
    name, email, password, role, employee_id,
    profile_complete, account_locked, failed_login_attempts,
    created_at, last_updated
) VALUES (
    'Manager',
    'manager@neobank.com',
    '$2a$10$YOUR_GENERATED_BCRYPT_HASH_HERE',  -- Replace with generated hash
    'MANAGER',
    'MGR001',
    false,
    false,
    0,
    NOW(),
    NOW()
);
```

### Insert Admin (with BCrypt password)

```sql
INSERT INTO admins (
    name, email, password, role, employee_id,
    profile_complete, account_locked, failed_login_attempts,
    created_at, last_updated
) VALUES (
    'Admin',
    'admin@neobank.com',
    '$2a$10$YOUR_GENERATED_BCRYPT_HASH_HERE',  -- Replace with generated hash
    'ADMIN',
    'ADM001',
    false,
    false,
    0,
    NOW(),
    NOW()
);
```

---

## Verification Queries

### Check Admin/Manager Exists

```sql
SELECT id, name, email, role, account_locked, failed_login_attempts
FROM admins
WHERE role = 'MANAGER' OR role = 'ADMIN';
```

### Check Password Format (BCrypt)

```sql
SELECT 
    email,
    password,
    LENGTH(password) as password_length,
    CASE 
        WHEN password LIKE '$2a$%' THEN 'BCrypt'
        WHEN password LIKE '$2b$%' THEN 'BCrypt'
        WHEN password LIKE '$2y$%' THEN 'BCrypt'
        WHEN password LIKE '%:%' THEN 'Legacy SHA-256'
        ELSE 'Unknown'
    END as password_format
FROM admins;
```

### Check Loans

```sql
SELECT 
    id,
    type,
    amount,
    status,
    account_number,
    application_date
FROM loans
ORDER BY application_date DESC
LIMIT 10;
```

---

## Migration Notes

### If Migrating from Local to Railway

1. Export data from local MySQL:
```bash
mysqldump -u root -p springapp > backup.sql
```

2. Import to Railway MySQL:
```bash
mysql -h mysql.railway.app -u root -p railway < backup.sql
```

3. Update passwords to BCrypt:
   - Old passwords will work (backward compatible)
   - Update passwords when users change them
   - Or reset all passwords and let users reset

### Column Name Mapping

JPA/Hibernate automatically converts:
- Java camelCase → SQL snake_case
- Example: `accountLocked` → `account_locked`

Ensure your Railway schema uses snake_case for consistency.

---

**Last Updated:** 2024

