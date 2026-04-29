-- Cheque Draw System Tables
-- Execute this script to create the cheque request and related management tables

-- Create cheque_requests table
CREATE TABLE IF NOT EXISTS cheque_requests (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  salary_account_id BIGINT NOT NULL,
  cheque_number VARCHAR(20) UNIQUE NOT NULL COMMENT 'Auto-generated like CHQ000123',
  serial_number VARCHAR(50) NOT NULL COMMENT 'Cheque serial number from the book',
  request_date DATE NOT NULL COMMENT 'Date of cheque request',
  cheque_date DATE NOT NULL COMMENT 'Date on cheque',
  amount DECIMAL(15, 2) NOT NULL COMMENT 'Withdrawal amount',
  available_balance DECIMAL(15, 2) NOT NULL COMMENT 'Balance captured at request time',
  payee_name VARCHAR(100) NOT NULL COMMENT 'Name on payee',
  remarks VARCHAR(500) COMMENT 'Additional remarks',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED',
  approved_by VARCHAR(100) COMMENT 'Admin email who approved',
  approved_at TIMESTAMP COMMENT 'Approval timestamp',
  rejection_reason VARCHAR(500) COMMENT 'If rejected, reason for rejection',
  rejected_at TIMESTAMP COMMENT 'Rejection timestamp',
  cheque_picked_up_at TIMESTAMP COMMENT 'When user picked up the cheque',
  cheque_cleared_date DATE COMMENT 'Date cheque was cleared/processed',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  PRIMARY KEY (id),
  UNIQUE KEY unique_cheque_number (cheque_number),
  UNIQUE KEY unique_serial_per_account (salary_account_id, serial_number),
  KEY idx_user_id (user_id),
  KEY idx_salary_account_id (salary_account_id),
  KEY idx_status (status),
  KEY idx_created_at (created_at),
  FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE RESTRICT,
  FOREIGN KEY (salary_account_id) REFERENCES salary_account(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Maintains cheque draw requests from salary account users';

-- Create cheque_sequence table to track sequence numbers for unique cheque number generation
CREATE TABLE IF NOT EXISTS cheque_sequence (
  id BIGINT NOT NULL AUTO_INCREMENT,
  salary_account_id BIGINT NOT NULL,
  next_sequence INT NOT NULL DEFAULT 1 COMMENT 'Next sequence number to use',
  last_generated VARCHAR(20) COMMENT 'Last generated cheque number',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  PRIMARY KEY (id),
  UNIQUE KEY unique_account_sequence (salary_account_id),
  FOREIGN KEY (salary_account_id) REFERENCES salary_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tracks unique cheque number sequence for each salary account';

-- Create cheque_audit_log table for tracking admin actions
CREATE TABLE IF NOT EXISTS cheque_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  cheque_request_id BIGINT NOT NULL,
  admin_email VARCHAR(100) NOT NULL,
  action VARCHAR(50) NOT NULL COMMENT 'VIEWED, APPROVED, REJECTED, PICKED_UP, CLEARED',
  remarks VARCHAR(500),
  ip_address VARCHAR(50),
  user_agent VARCHAR(300),
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  PRIMARY KEY (id),
  KEY idx_cheque_request_id (cheque_request_id),
  KEY idx_admin_email (admin_email),
  KEY idx_timestamp (timestamp),
  FOREIGN KEY (cheque_request_id) REFERENCES cheque_requests(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit trail of admin actions on cheque requests';

-- Create cheque_book_ranges table for validation
CREATE TABLE IF NOT EXISTS cheque_book_ranges (
  id BIGINT NOT NULL AUTO_INCREMENT,
  salary_account_id BIGINT NOT NULL,
  cheque_book_number VARCHAR(50) NOT NULL COMMENT 'Physical cheque book identifier',
  serial_from VARCHAR(50) NOT NULL COMMENT 'Starting serial number',
  serial_to VARCHAR(50) NOT NULL COMMENT 'Ending serial number',
  issued_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, EXHAUSTED, CANCELLED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  PRIMARY KEY (id),
  KEY idx_salary_account_id (salary_account_id),
  KEY idx_status (status),
  FOREIGN KEY (salary_account_id) REFERENCES salary_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tracks cheque book ranges issued to salary accounts';

-- Alter transactions table to include cheque-related fields (if not already present)
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS cheque_number VARCHAR(20) COMMENT 'Associated cheque number if applicable';
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS cheque_request_id BIGINT COMMENT 'Link to cheque request';
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payee_name VARCHAR(100) COMMENT 'Payee name for cheque withdrawals';

-- Add indexes for cheque-related queries
ALTER TABLE transactions ADD KEY IF NOT EXISTS idx_cheque_number (cheque_number);
ALTER TABLE transactions ADD KEY IF NOT EXISTS idx_cheque_request_id (cheque_request_id);

-- Create views for easy querying
CREATE OR REPLACE VIEW cheque_requests_with_user AS
SELECT 
  cr.id,
  cr.cheque_number,
  cr.serial_number,
  cr.request_date,
  cr.cheque_date,
  cr.amount,
  cr.available_balance,
  cr.payee_name,
  cr.status,
  cr.approved_by,
  cr.approved_at,
  cr.created_at,
  u.username AS user_name,
  u.email AS user_email,
  sa.account_number,
  sa.balance AS current_balance
FROM cheque_requests cr
LEFT JOIN user u ON cr.user_id = u.id
LEFT JOIN salary_account sa ON cr.salary_account_id = sa.id;

-- Insert initial cheque book range sample (for testing)
-- Uncomment and modify as needed
-- INSERT INTO cheque_book_ranges (salary_account_id, cheque_book_number, serial_from, serial_to, issued_date)
-- SELECT sa.id, 'BOOK-001', '000001', '000050', CURDATE()
-- FROM salary_account sa LIMIT 1;

-- Log of execution
SELECT "✓ Cheque Draw System tables created successfully" AS message;
