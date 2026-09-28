-- =====================================================
-- ACCOUNT LINKING AND CHARGE MANAGEMENT SYSTEM
-- Migration Script for NeoBank
-- =====================================================

-- Create allocation_account table
CREATE TABLE IF NOT EXISTS allocation_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  allocation_id BIGINT NOT NULL UNIQUE,
  account_number VARCHAR(20) NOT NULL UNIQUE,
  ifsc_code VARCHAR(11) NOT NULL,
  account_holder_name VARCHAR(100) NOT NULL,
  bank_name VARCHAR(100),
  branch_name VARCHAR(100),
  city VARCHAR(50),
  state VARCHAR(50),
  account_type VARCHAR(50) DEFAULT 'CURRENT',
  verification_status VARCHAR(50) DEFAULT 'PENDING',
  cheque_number VARCHAR(20),
  cheque_holder_name VARCHAR(100),
  cheque_date DATE,
  cheque_bank VARCHAR(100),
  cheque_image_url VARCHAR(255),
  cheque_verification_status VARCHAR(50) DEFAULT 'PENDING',
  cheque_status VARCHAR(50) DEFAULT 'PENDING',
  account_status VARCHAR(50) DEFAULT 'ACTIVE',
  total_allocated DECIMAL(15, 2) DEFAULT 0,
  total_debited DECIMAL(15, 2) DEFAULT 0,
  total_credited DECIMAL(15, 2) DEFAULT 0,
  current_balance DECIMAL(15, 2) DEFAULT 0,
  total_charges_collected DECIMAL(15, 2) DEFAULT 0,
  interest_charges DECIMAL(15, 2) DEFAULT 0,
  cibil_charges DECIMAL(15, 2) DEFAULT 0,
  soundbox_charges DECIMAL(15, 2) DEFAULT 0,
  upi_charges DECIMAL(15, 2) DEFAULT 0,
  payment_gateway_charges DECIMAL(15, 2) DEFAULT 0,
  other_charges DECIMAL(15, 2) DEFAULT 0,
  linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  verified_at TIMESTAMP NULL,
  linked_by_admin_id BIGINT,
  linked_by_admin_name VARCHAR(100),
  verified_by_admin_id BIGINT,
  verified_by_admin_name VARCHAR(100),
  verification_notes TEXT,
  charge_management_enabled BOOLEAN DEFAULT TRUE,
  blocked_reason VARCHAR(255),
  blocked_at TIMESTAMP NULL,
  blocked_by_admin_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (allocation_id) REFERENCES funds_allocations(id),
  FOREIGN KEY (linked_by_admin_id) REFERENCES admins(id),
  FOREIGN KEY (verified_by_admin_id) REFERENCES admins(id),
  FOREIGN KEY (blocked_by_admin_id) REFERENCES admins(id)
);

-- Create charge_transaction table
CREATE TABLE IF NOT EXISTS charge_transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  charge_transaction_id VARCHAR(50) NOT NULL UNIQUE,
  allocation_id BIGINT NOT NULL,
  allocation_account_id BIGINT,
  charge_type VARCHAR(50) NOT NULL,
  charge_amount DECIMAL(15, 2) NOT NULL,
  user_account_number VARCHAR(20),
  user_name VARCHAR(100),
  user_id BIGINT,
  product_type VARCHAR(50),
  transaction_reference VARCHAR(100),
  collection_status VARCHAR(50) DEFAULT 'COLLECTED',
  credit_status VARCHAR(50) DEFAULT 'CREDITED',
  status VARCHAR(50) DEFAULT 'SUCCESS',
  reversal_status VARCHAR(50) DEFAULT 'NOT_REVERSED',
  reversal_reason VARCHAR(255),
  reversal_at TIMESTAMP NULL,
  reversed_by_admin_id BIGINT,
  reversed_by_admin_name VARCHAR(100),
  batch_id VARCHAR(50),
  reconciliation_status VARCHAR(50) DEFAULT 'RECONCILED',
  tax_collected DECIMAL(15, 2) DEFAULT 0,
  tax_rate DECIMAL(5, 2) DEFAULT 18,
  net_charge DECIMAL(15, 2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (allocation_id) REFERENCES funds_allocations(id),
  FOREIGN KEY (allocation_account_id) REFERENCES allocation_account(id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (reversed_by_admin_id) REFERENCES admins(id)
);

-- Create charge_config table for charge rates
CREATE TABLE IF NOT EXISTS charge_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  charge_type VARCHAR(50) NOT NULL UNIQUE,
  charge_name VARCHAR(100),
  charge_rate DECIMAL(15, 2),
  charge_frequency VARCHAR(50),
  min_charge DECIMAL(15, 2) DEFAULT 0,
  max_charge DECIMAL(15, 2),
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create charge_summary_daily view for daily reporting
CREATE OR REPLACE VIEW charge_summary_daily AS
SELECT
  DATE(ct.created_at) as charge_date,
  ct.charge_type,
  COUNT(*) as transaction_count,
  SUM(ct.charge_amount) as total_charge,
  SUM(ct.tax_collected) as total_tax,
  SUM(ct.net_charge) as net_total,
  COUNT(CASE WHEN ct.status = 'SUCCESS' THEN 1 END) as successful_count,
  COUNT(CASE WHEN ct.status = 'FAILED' THEN 1 END) as failed_count,
  COUNT(CASE WHEN ct.status = 'PENDING' THEN 1 END) as pending_count
FROM charge_transaction ct
GROUP BY DATE(ct.created_at), ct.charge_type;

-- Create allocation_charge_summary view
CREATE OR REPLACE VIEW allocation_charge_summary AS
SELECT
  aa.allocation_id,
  aa.account_number,
  aa.account_holder_name,
  SUM(CASE WHEN ct.charge_type = 'INTEREST' THEN ct.charge_amount ELSE 0 END) as interest_total,
  SUM(CASE WHEN ct.charge_type = 'CIBIL' THEN ct.charge_amount ELSE 0 END) as cibil_total,
  SUM(CASE WHEN ct.charge_type = 'SOUNDBOX' THEN ct.charge_amount ELSE 0 END) as soundbox_total,
  SUM(CASE WHEN ct.charge_type = 'UPI' THEN ct.charge_amount ELSE 0 END) as upi_total,
  SUM(CASE WHEN ct.charge_type = 'PAYMENT_GATEWAY' THEN ct.charge_amount ELSE 0 END) as payment_gateway_total,
  SUM(ct.charge_amount) as total_charges,
  COUNT(*) as transaction_count
FROM allocation_account aa
LEFT JOIN charge_transaction ct ON aa.id = ct.allocation_account_id
GROUP BY aa.allocation_id, aa.account_number, aa.account_holder_name;

-- Create verified_accounts view
CREATE OR REPLACE VIEW verified_accounts AS
SELECT
  id,
  allocation_id,
  account_number,
  account_holder_name,
  bank_name,
  city,
  branch_name,
  account_status,
  verified_at,
  total_allocated,
  current_balance,
  total_charges_collected
FROM allocation_account
WHERE verification_status = 'VERIFIED' AND account_status = 'ACTIVE';

-- Create pending_cheques view
CREATE OR REPLACE VIEW pending_cheques AS
SELECT
  id,
  allocation_id,
  account_number,
  account_holder_name,
  ifsc_code,
  cheque_number,
  cheque_holder_name,
  cheque_date,
  cheque_bank,
  cheque_image_url,
  city,
  branch_name,
  linked_at,
  linked_by_admin_name
FROM allocation_account
WHERE cheque_verification_status = 'PENDING' AND cheque_status = 'PENDING';

-- Create charge_transactions_failed view
CREATE OR REPLACE VIEW charge_transactions_failed AS
SELECT
  charge_transaction_id,
  allocation_id,
  allocation_account_id,
  charge_type,
  charge_amount,
  user_account_number,
  status,
  created_at
FROM charge_transaction
WHERE status = 'FAILED' OR credit_status = 'FAILED';

-- Index for performance optimization
CREATE INDEX idx_allocation_account_allocation_id ON allocation_account(allocation_id);
CREATE INDEX idx_allocation_account_account_number ON allocation_account(account_number);
CREATE INDEX idx_allocation_account_cheque_number ON allocation_account(cheque_number);
CREATE INDEX idx_allocation_account_verification_status ON allocation_account(verification_status);
CREATE INDEX idx_allocation_account_account_status ON allocation_account(account_status);
CREATE INDEX idx_allocation_account_city_branch ON allocation_account(city, branch_name);

CREATE INDEX idx_charge_transaction_allocation_id ON charge_transaction(allocation_id);
CREATE INDEX idx_charge_transaction_account_id ON charge_transaction(allocation_account_id);
CREATE INDEX idx_charge_transaction_charge_type ON charge_transaction(charge_type);
CREATE INDEX idx_charge_transaction_status ON charge_transaction(status);
CREATE INDEX idx_charge_transaction_created_at ON charge_transaction(created_at);
CREATE INDEX idx_charge_transaction_user_account ON charge_transaction(user_account_number);
CREATE INDEX idx_charge_transaction_batch_id ON charge_transaction(batch_id);

-- Stored Procedure: Update Charge Tracking
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS sp_update_charge_tracking(
  IN p_allocation_account_id BIGINT,
  IN p_charge_type VARCHAR(50),
  IN p_charge_amount DECIMAL(15, 2)
)
BEGIN
  UPDATE allocation_account
  SET
    total_charges_collected = total_charges_collected + p_charge_amount,
    interest_charges = CASE WHEN p_charge_type = 'INTEREST' THEN interest_charges + p_charge_amount ELSE interest_charges END,
    cibil_charges = CASE WHEN p_charge_type = 'CIBIL' THEN cibil_charges + p_charge_amount ELSE cibil_charges END,
    soundbox_charges = CASE WHEN p_charge_type = 'SOUNDBOX' THEN soundbox_charges + p_charge_amount ELSE soundbox_charges END,
    upi_charges = CASE WHEN p_charge_type = 'UPI' THEN upi_charges + p_charge_amount ELSE upi_charges END,
    payment_gateway_charges = CASE WHEN p_charge_type = 'PAYMENT_GATEWAY' THEN payment_gateway_charges + p_charge_amount ELSE payment_gateway_charges END,
    other_charges = CASE WHEN p_charge_type = 'OTHER' THEN other_charges + p_charge_amount ELSE other_charges END,
    updated_at = CURRENT_TIMESTAMP
  WHERE id = p_allocation_account_id;
END //

-- Stored Procedure: Verify Account
CREATE PROCEDURE IF NOT EXISTS sp_verify_account(
  IN p_allocation_id BIGINT,
  IN p_verified_by_admin_id BIGINT,
  IN p_verified_by_admin_name VARCHAR(100)
)
BEGIN
  UPDATE allocation_account
  SET
    verification_status = 'VERIFIED',
    cheque_verification_status = 'CLEARED',
    cheque_status = 'CLEARED',
    account_status = 'ACTIVE',
    verified_at = CURRENT_TIMESTAMP,
    verified_by_admin_id = p_verified_by_admin_id,
    verified_by_admin_name = p_verified_by_admin_name,
    updated_at = CURRENT_TIMESTAMP
  WHERE allocation_id = p_allocation_id;
END //

-- Stored Procedure: Reject Cheque
CREATE PROCEDURE IF NOT EXISTS sp_reject_cheque(
  IN p_allocation_id BIGINT,
  IN p_rejection_reason VARCHAR(255)
)
BEGIN
  UPDATE allocation_account
  SET
    cheque_verification_status = 'REJECTED',
    cheque_status = 'BOUNCED',
    verification_notes = p_rejection_reason,
    updated_at = CURRENT_TIMESTAMP
  WHERE allocation_id = p_allocation_id;
END //

-- Stored Procedure: Block Account
CREATE PROCEDURE IF NOT EXISTS sp_block_account(
  IN p_account_id BIGINT,
  IN p_reason VARCHAR(255),
  IN p_admin_id BIGINT
)
BEGIN
  UPDATE allocation_account
  SET
    account_status = 'BLOCKED',
    blocked_reason = p_reason,
    blocked_at = CURRENT_TIMESTAMP,
    blocked_by_admin_id = p_admin_id,
    updated_at = CURRENT_TIMESTAMP
  WHERE id = p_account_id;
END //

DELIMITER ;

-- Insert default charge configurations
INSERT IGNORE INTO charge_config (charge_type, charge_name, charge_rate, charge_frequency, is_active)
VALUES
  ('INTEREST', 'Interest Charges', 0.10, 'MONTHLY', TRUE),
  ('CIBIL', 'CIBIL Report Charges', 99.00, 'ONE_TIME', TRUE),
  ('SOUNDBOX', 'Soundbox Notification Charges', 5.00, 'MONTHLY', TRUE),
  ('UPI', 'UPI Transaction Charges', 0.50, 'PER_TRANSACTION', TRUE),
  ('PAYMENT_GATEWAY', 'Payment Gateway Charges', 1.00, 'PER_TRANSACTION', TRUE);

-- Create audit log table for compliance
CREATE TABLE IF NOT EXISTS allocation_account_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  allocation_account_id BIGINT NOT NULL,
  action VARCHAR(50),
  old_value TEXT,
  new_value TEXT,
  changed_by_admin_id BIGINT,
  changed_by_admin_name VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (allocation_account_id) REFERENCES allocation_account(id),
  FOREIGN KEY (changed_by_admin_id) REFERENCES admins(id)
);

CREATE INDEX idx_allocation_account_audit_account_id ON allocation_account_audit(allocation_account_id);
CREATE INDEX idx_allocation_account_audit_created_at ON allocation_account_audit(created_at);

-- Confirm migration completion
SELECT 'Migration 43: Account Linking & Charge Management System - COMPLETED' as status;
