-- Business Account Cheque Draw System Tables
-- Mirrors the salary account cheque draw system for current/business accounts

-- Create business_cheque_requests table
CREATE TABLE IF NOT EXISTS business_cheque_requests (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  current_account_id BIGINT NOT NULL,
  cheque_number VARCHAR(20) UNIQUE NOT NULL COMMENT 'Auto-generated like BCHQ500123',
  serial_number VARCHAR(50) NOT NULL COMMENT 'Cheque serial number from the book',
  request_date DATE NOT NULL COMMENT 'Date of cheque request',
  cheque_date DATE NOT NULL COMMENT 'Date on cheque',
  amount DECIMAL(15, 2) NOT NULL COMMENT 'Withdrawal amount',
  available_balance DECIMAL(15, 2) NOT NULL COMMENT 'Balance captured at request time',
  payee_name VARCHAR(100) NOT NULL COMMENT 'Name on payee',
  remarks VARCHAR(500) COMMENT 'Additional remarks',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED, CLEARED',
  approved_by VARCHAR(100) COMMENT 'Admin email who approved',
  approved_at TIMESTAMP NULL COMMENT 'Approval timestamp',
  rejection_reason VARCHAR(500) COMMENT 'If rejected, reason for rejection',
  rejected_at TIMESTAMP NULL COMMENT 'Rejection timestamp',
  cheque_picked_up_at TIMESTAMP NULL COMMENT 'When user picked up the cheque',
  cheque_cleared_date DATE COMMENT 'Date cheque was cleared/processed',
  cheque_downloaded BOOLEAN DEFAULT FALSE COMMENT 'Whether user downloaded the cheque',
  cheque_downloaded_at TIMESTAMP NULL COMMENT 'When user downloaded the cheque',
  payee_account_number VARCHAR(50) COMMENT 'Payee account number after verification',
  payee_account_verified BOOLEAN DEFAULT FALSE COMMENT 'Whether payee account was verified',
  payee_account_type VARCHAR(50) COMMENT 'Type of payee account',
  transaction_reference VARCHAR(100) COMMENT 'Transaction reference after approval',
  debited_from_account VARCHAR(50) COMMENT 'Account number debited from',
  credited_to_account VARCHAR(50) COMMENT 'Account number credited to',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  PRIMARY KEY (id),
  UNIQUE KEY unique_business_cheque_number (cheque_number),
  UNIQUE KEY unique_serial_per_business_account (current_account_id, serial_number),
  KEY idx_business_cheque_user_id (user_id),
  KEY idx_business_cheque_current_account_id (current_account_id),
  KEY idx_business_cheque_status (status),
  KEY idx_business_cheque_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Maintains cheque draw requests from business/current account users';

-- Create business_cheque_sequence table
CREATE TABLE IF NOT EXISTS business_cheque_sequence (
  id BIGINT NOT NULL AUTO_INCREMENT,
  current_account_id BIGINT NOT NULL,
  next_sequence INT NOT NULL DEFAULT 1 COMMENT 'Next sequence number to use',
  last_generated VARCHAR(20) COMMENT 'Last generated cheque number',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  PRIMARY KEY (id),
  UNIQUE KEY unique_business_account_sequence (current_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tracks unique cheque number sequence for each business account';

-- Create business_cheque_audit_log table
CREATE TABLE IF NOT EXISTS business_cheque_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  cheque_request_id BIGINT NOT NULL,
  admin_email VARCHAR(100) NOT NULL,
  action VARCHAR(50) NOT NULL COMMENT 'VIEWED, APPROVED, REJECTED, PICKED_UP, CLEARED',
  remarks VARCHAR(500),
  ip_address VARCHAR(50),
  user_agent VARCHAR(300),
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  PRIMARY KEY (id),
  KEY idx_business_audit_cheque_request_id (cheque_request_id),
  KEY idx_business_audit_admin_email (admin_email),
  KEY idx_business_audit_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit trail of admin actions on business cheque requests';

-- Create business_cheque_book_ranges table
CREATE TABLE IF NOT EXISTS business_cheque_book_ranges (
  id BIGINT NOT NULL AUTO_INCREMENT,
  current_account_id BIGINT NOT NULL,
  cheque_book_number VARCHAR(50) NOT NULL COMMENT 'Physical cheque book identifier',
  serial_from VARCHAR(50) NOT NULL COMMENT 'Starting serial number',
  serial_to VARCHAR(50) NOT NULL COMMENT 'Ending serial number',
  issued_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, EXHAUSTED, CANCELLED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  PRIMARY KEY (id),
  KEY idx_business_range_current_account_id (current_account_id),
  KEY idx_business_range_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tracks cheque book ranges issued to business accounts';

-- Create business_cheque_leaves table
CREATE TABLE IF NOT EXISTS business_cheque_leaves (
  id BIGINT NOT NULL AUTO_INCREMENT,
  current_account_id BIGINT NOT NULL,
  leaf_number VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE, USED, CANCELLED',
  allocated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  used_at TIMESTAMP NULL,
  
  PRIMARY KEY (id),
  KEY idx_business_leaf_current_account_id (current_account_id),
  KEY idx_business_leaf_status (status),
  UNIQUE KEY unique_business_leaf (current_account_id, leaf_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tracks individual cheque leaves allocated to business accounts';

-- Add cheque-related columns to business_transactions table if not present
ALTER TABLE business_transactions ADD COLUMN IF NOT EXISTS cheque_number VARCHAR(20) COMMENT 'Associated business cheque number if applicable';
ALTER TABLE business_transactions ADD COLUMN IF NOT EXISTS cheque_request_id BIGINT COMMENT 'Link to business cheque request';
ALTER TABLE business_transactions ADD COLUMN IF NOT EXISTS payee_name VARCHAR(100) COMMENT 'Payee name for cheque withdrawals';

-- Add indexes for cheque-related queries on business_transactions
ALTER TABLE business_transactions ADD KEY IF NOT EXISTS idx_business_txn_cheque_number (cheque_number);
ALTER TABLE business_transactions ADD KEY IF NOT EXISTS idx_business_txn_cheque_request_id (cheque_request_id);

SELECT "✓ Business Account Cheque Draw System tables created successfully" AS message;
