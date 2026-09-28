-- ================================================================
-- FUNDS ALLOCATION ACCOUNT LINKING SYSTEM
-- ACCOUNT VERIFICATION VIA CHEQUE
-- CHARGE MANAGEMENT (Interest, CIBIL, Soundbox, UPI, Payment Gateway)
-- ================================================================

USE springapp;

-- ================================================================
-- TABLE 1: allocation_account
-- Represents the CURRENT ACCOUNT linked to fund allocations
-- All debit/credit operations MUST use this account only
-- Verified via Cheque mechanism by HOD/Admin
-- ================================================================

CREATE TABLE IF NOT EXISTS allocation_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    allocation_id BIGINT NOT NULL UNIQUE COMMENT 'FK to funds_allocation',
    
    -- Account Details
    account_number VARCHAR(50) NOT NULL UNIQUE COMMENT 'Bank account number',
    ifsc_code VARCHAR(50) NOT NULL COMMENT 'Bank IFSC code',
    account_holder_name VARCHAR(100) NOT NULL COMMENT 'Account owner name',
    bank_name VARCHAR(100) COMMENT 'Bank name',
    account_type VARCHAR(50) COMMENT 'CURRENT, SAVINGS',
    
    -- Location Details
    branch_name VARCHAR(100) COMMENT 'Branch name',
    city VARCHAR(100) COMMENT 'City',
    location VARCHAR(100) COMMENT 'Location/Area',
    state VARCHAR(50) COMMENT 'State',
    
    -- Account Balance & Tracking
    account_balance DECIMAL(19, 2) COMMENT 'Current account balance',
    total_allocated DECIMAL(19, 2) COMMENT 'Total fund allocated',
    total_debited DECIMAL(19, 2) COMMENT 'Total debited',
    total_credited DECIMAL(19, 2) COMMENT 'Total credited',
    current_balance DECIMAL(19, 2) COMMENT 'Current available balance',
    
    -- Verification Status
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, VERIFIED, REJECTED',
    cheque_number VARCHAR(50) COMMENT 'Cheque number for verification',
    cheque_holder_name VARCHAR(100) COMMENT 'Cheque holder name',
    cheque_status VARCHAR(20) COMMENT 'PENDING, CLEARED, BOUNCED',
    verification_documents LONGTEXT COMMENT 'JSON: cheque_image_url, bank_statement_url, etc',
    verification_notes VARCHAR(500) COMMENT 'Verification notes',
    
    -- Account Status
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, BLOCKED, INACTIVE',
    
    -- Charge Tracking
    total_charges_collected DECIMAL(19, 2) DEFAULT 0 COMMENT 'Total charges debited from users',
    interest_charges DECIMAL(19, 2) DEFAULT 0 COMMENT 'Interest charges collected',
    cibil_charges DECIMAL(19, 2) DEFAULT 0 COMMENT 'CIBIL report charges',
    soundbox_charges DECIMAL(19, 2) DEFAULT 0 COMMENT 'Soundbox system charges',
    upi_charges DECIMAL(19, 2) DEFAULT 0 COMMENT 'UPI transaction charges',
    payment_gateway_charges DECIMAL(19, 2) DEFAULT 0 COMMENT 'Payment gateway charges',
    other_charges DECIMAL(19, 2) DEFAULT 0 COMMENT 'Other miscellaneous charges',
    
    -- Audit
    linked_by_admin_id BIGINT NOT NULL COMMENT 'HOD admin who linked account',
    linked_by_admin_name VARCHAR(100) NOT NULL COMMENT 'HOD admin name',
    linked_at TIMESTAMP NOT NULL COMMENT 'Account linking timestamp',
    verified_at TIMESTAMP COMMENT 'Verification timestamp',
    verified_by_admin_id BIGINT COMMENT 'Admin who verified',
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    KEY idx_allocation_id (allocation_id),
    KEY idx_account_number (account_number),
    KEY idx_ifsc_code (ifsc_code),
    KEY idx_cheque_number (cheque_number),
    KEY idx_verification_status (verification_status),
    KEY idx_account_status (account_status),
    KEY idx_city_branch (city, branch_name),
    KEY idx_linked_at (linked_at),
    KEY idx_total_charges (total_charges_collected DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bank accounts linked to fund allocations for charge management';

-- ================================================================
-- TABLE 2: charge_transaction
-- Tracks all charges (Interest, CIBIL, Soundbox, UPI, Payment Gateway)
-- DEBITED from users and CREDITED to allocation account
-- ================================================================

CREATE TABLE IF NOT EXISTS charge_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    charge_transaction_id VARCHAR(50) NOT NULL UNIQUE COMMENT 'CT-2025-XXXXX',
    
    -- Allocation & Account References
    allocation_id BIGINT NOT NULL COMMENT 'FK to funds_allocation',
    allocation_account_id BIGINT NOT NULL COMMENT 'FK to allocation_account (credit destination)',
    
    -- Charge Details
    charge_type VARCHAR(50) NOT NULL COMMENT 'INTEREST, CIBIL, SOUNDBOX, UPI, PAYMENT_GATEWAY, OTHER',
    charge_description VARCHAR(100) NOT NULL,
    charge_amount DECIMAL(19, 2) NOT NULL COMMENT 'Amount debited from user',
    
    -- User Details (from whom charge is collected)
    user_account_number VARCHAR(50) NOT NULL COMMENT 'User account number',
    user_name VARCHAR(100) COMMENT 'User name',
    user_product_type VARCHAR(50) COMMENT 'GOLD_LOAN, DEPOSITS, etc',
    linked_transaction_id VARCHAR(100) COMMENT 'TXN-2025-XXXXX (original transaction)',
    linked_loan_id VARCHAR(100) COMMENT 'Loan ID if loan-specific charge',
    linked_deposit_id VARCHAR(100) COMMENT 'Deposit ID if deposit-specific charge',
    
    -- Credit Details (to allocation account)
    credit_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_CREDIT' COMMENT 'PENDING_CREDIT, CREDITED, FAILED',
    credited_at TIMESTAMP COMMENT 'When credited to allocation account',
    credit_reference_number VARCHAR(100) COMMENT 'Auto-generated credit reference',
    
    -- Collection Details
    collection_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'COLLECTED, PENDING, REVERSED',
    collection_date TIMESTAMP COMMENT 'When collected from user',
    collection_details LONGTEXT COMMENT 'JSON: mode, gateway, ref_id, etc',
    
    -- Tax/Withholding
    tax_amount DECIMAL(19, 2) COMMENT 'TDS or withholding amount',
    net_credit_amount DECIMAL(19, 2) COMMENT 'chargeAmount - taxAmount',
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'SUCCESS, PENDING, FAILED, REVERSED',
    failure_reason VARCHAR(500) COMMENT 'Reason if failed',
    
    -- Processing Audit
    processed_by_admin_id BIGINT COMMENT 'Admin who processed',
    processed_by_admin_name VARCHAR(100) COMMENT 'Admin name',
    processed_at TIMESTAMP COMMENT 'Processing timestamp',
    
    -- Reconciliation
    reconciliation_status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING, MATCHED, UNMATCHED',
    reconciliation_date TIMESTAMP COMMENT 'Reconciliation timestamp',
    reconciliation_notes VARCHAR(100) COMMENT 'Reconciliation notes',
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    KEY idx_allocation_id (allocation_id),
    KEY idx_allocation_account_id (allocation_account_id),
    KEY idx_charge_type (charge_type),
    KEY idx_user_account (user_account_number),
    KEY idx_transaction_date (created_at DESC),
    KEY idx_credit_status (credit_status),
    KEY idx_status (status),
    KEY idx_charge_transaction_id (charge_transaction_id),
    KEY idx_linked_transaction (linked_transaction_id),
    KEY idx_reconciliation (reconciliation_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Charge transactions - Interest, CIBIL, Soundbox, UPI, Payment Gateway charges';

-- ================================================================
-- TABLE 3: funds_allocation (ENHANCED)
-- Extended to include account linking and charge management
-- ================================================================

CREATE TABLE IF NOT EXISTS funds_allocation_enhanced (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    allocation_id VARCHAR(50) NOT NULL UNIQUE COMMENT 'FA-2025-XXXXX',
    
    -- Manager & Location
    manager_id BIGINT NOT NULL,
    manager_name VARCHAR(100) NOT NULL,
    manager_account_number VARCHAR(50),
    branch_name VARCHAR(100) NOT NULL,
    city VARCHAR(100),
    location VARCHAR(100),
    state VARCHAR(50),
    
    -- Fund Allocation
    allocated_amount DECIMAL(19, 2) NOT NULL,
    allocation_type VARCHAR(50) COMMENT 'GENERAL, CAMPAIGN, SEASONAL',
    product_types VARCHAR(255) COMMENT 'GOLD_LOAN,DEPOSITS,WITHDRAWALS,LOANS,OVERDRAFT,SALARY_CREDITS',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, PAUSED, COMPLETED, CANCELLED',
    current_balance DECIMAL(19, 2),
    total_debited DECIMAL(19, 2),
    total_credited DECIMAL(19, 2),
    total_utilized DECIMAL(19, 2),
    
    -- Validity
    allocation_date DATE,
    valid_from DATE,
    valid_till DATE NOT NULL,
    description TEXT,
    
    -- ==================== ACCOUNT LINKING ====================
    allocation_account_id BIGINT NOT NULL UNIQUE COMMENT 'FK to allocation_account',
    linked_account_number VARCHAR(50) NOT NULL COMMENT 'Denormalized account number',
    linked_ifsc_code VARCHAR(50) NOT NULL,
    linked_account_holder_name VARCHAR(100) NOT NULL,
    linked_bank_name VARCHAR(100),
    linked_account_type VARCHAR(50) COMMENT 'CURRENT, SAVINGS',
    account_verification_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, VERIFIED, REJECTED',
    account_verified_at TIMESTAMP,
    
    -- ==================== CHEQUE VERIFICATION ====================
    cheque_verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, CLEARED, BOUNCED',
    linked_cheque_number VARCHAR(50),
    cheque_holder_name VARCHAR(100),
    cheque_date DATE,
    cheque_bank VARCHAR(50),
    cheque_image_url LONGTEXT,
    cheque_verification_notes VARCHAR(500),
    cheque_verified_at TIMESTAMP,
    
    -- ==================== CHARGE MANAGEMENT ====================
    charge_management_enabled BOOLEAN DEFAULT FALSE,
    total_charges_collected DECIMAL(19, 2) DEFAULT 0,
    interest_charges DECIMAL(19, 2) DEFAULT 0,
    cibil_charges DECIMAL(19, 2) DEFAULT 0,
    soundbox_charges DECIMAL(19, 2) DEFAULT 0,
    upi_charges DECIMAL(19, 2) DEFAULT 0,
    payment_gateway_charges DECIMAL(19, 2) DEFAULT 0,
    other_charges DECIMAL(19, 2) DEFAULT 0,
    charge_transaction_count INT DEFAULT 0,
    charge_policy LONGTEXT COMMENT 'JSON: interest_rate, cibil_charges, etc',
    
    -- Account Status (KEY: Controls whether account can be used)
    account_status VARCHAR(20) NOT NULL DEFAULT 'NOT_LINKED' 
        COMMENT 'NOT_LINKED, LINKING_PENDING, VERIFIED, REJECTED, BLOCKED',
    
    -- Audit
    allocated_by_admin_id BIGINT,
    allocated_by_admin_name VARCHAR(100),
    linked_by_admin_id BIGINT,
    linked_by_admin_name VARCHAR(100),
    linked_at TIMESTAMP,
    verified_by_admin_id BIGINT,
    verified_by_admin_name VARCHAR(100),
    verified_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancelled_reason TEXT,
    cancelled_by_admin_id BIGINT,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    KEY idx_allocation_id (allocation_id),
    KEY idx_manager_id (manager_id),
    KEY idx_status (status),
    KEY idx_account_status (account_status),
    KEY idx_manager_status (manager_id, status),
    KEY idx_valid_till (valid_till),
    KEY idx_branch_name (branch_name),
    KEY idx_allocation_account_id (allocation_account_id),
    KEY idx_account_verification (account_verification_status),
    KEY idx_cheque_verification (cheque_verification_status),
    KEY idx_total_charges (total_charges_collected DESC),
    KEY idx_linked_at (linked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Enhanced fund allocations with account linking and charge management';

-- ================================================================
-- VIEWS FOR DASHBOARD QUERIES
-- ================================================================

-- View 1: Allocations with linked accounts (HOD Dashboard)
CREATE OR REPLACE VIEW v_allocations_with_linked_accounts AS
SELECT 
    fa.id,
    fa.allocation_id,
    fa.manager_id,
    fa.manager_name,
    fa.branch_name,
    fa.city,
    fa.location,
    fa.allocated_amount,
    fa.current_balance,
    fa.status,
    fa.account_status,
    aa.account_number,
    aa.ifsc_code,
    aa.account_holder_name,
    aa.bank_name,
    aa.verification_status,
    aa.cheque_status,
    aa.total_charges_collected,
    aa.linked_at,
    aa.verified_at,
    fa.created_at
FROM funds_allocation_enhanced fa
LEFT JOIN allocation_account aa ON fa.allocation_account_id = aa.id
WHERE fa.status = 'ACTIVE'
ORDER BY fa.created_at DESC;

-- View 2: Charge summary by allocation (Admin Dashboard)
CREATE OR REPLACE VIEW v_charge_summary_by_allocation AS
SELECT 
    fa.allocation_id,
    fa.manager_name,
    fa.branch_name,
    COUNT(ct.id) as total_charge_transactions,
    SUM(ct.charge_amount) as total_charges_debited,
    SUM(ct.net_credit_amount) as total_charges_credited,
    SUM(CASE WHEN ct.charge_type = 'INTEREST' THEN ct.charge_amount ELSE 0 END) as interest_total,
    SUM(CASE WHEN ct.charge_type = 'CIBIL' THEN ct.charge_amount ELSE 0 END) as cibil_total,
    SUM(CASE WHEN ct.charge_type = 'SOUNDBOX' THEN ct.charge_amount ELSE 0 END) as soundbox_total,
    SUM(CASE WHEN ct.charge_type = 'UPI' THEN ct.charge_amount ELSE 0 END) as upi_total,
    SUM(CASE WHEN ct.charge_type = 'PAYMENT_GATEWAY' THEN ct.charge_amount ELSE 0 END) as payment_gateway_total,
    COUNT(CASE WHEN ct.credit_status = 'CREDITED' THEN 1 END) as credited_count,
    COUNT(CASE WHEN ct.credit_status = 'PENDING_CREDIT' THEN 1 END) as pending_credit_count,
    MAX(ct.created_at) as last_charge_at
FROM funds_allocation_enhanced fa
LEFT JOIN charge_transaction ct ON fa.id = ct.allocation_id
GROUP BY fa.id, fa.allocation_id, fa.manager_name, fa.branch_name;

-- View 3: Pending account verifications (Admin Dashboard)
CREATE OR REPLACE VIEW v_pending_account_verifications AS
SELECT 
    aa.id,
    aa.allocation_id,
    aa.account_number,
    aa.ifsc_code,
    aa.account_holder_name,
    aa.bank_name,
    aa.branch_name,
    aa.city,
    aa.location,
    aa.cheque_number,
    aa.cheque_holder_name,
    aa.verification_status,
    aa.cheque_status,
    aa.linked_at,
    aa.linked_by_admin_name,
    fa.manager_name,
    fa.allocated_amount,
    fa.status as allocation_status
FROM allocation_account aa
LEFT JOIN funds_allocation_enhanced fa ON aa.allocation_id = fa.id
WHERE aa.verification_status = 'PENDING'
ORDER BY aa.linked_at ASC;

-- View 4: Account-wise charge collection summary
CREATE OR REPLACE VIEW v_account_charge_collection_summary AS
SELECT 
    aa.id as account_id,
    aa.allocation_id,
    aa.account_number,
    aa.account_holder_name,
    aa.city,
    aa.branch_name,
    aa.total_charges_collected,
    aa.interest_charges,
    aa.cibil_charges,
    aa.soundbox_charges,
    aa.upi_charges,
    aa.payment_gateway_charges,
    aa.current_balance,
    fa.manager_name,
    fa.allocated_amount,
    fa.status as allocation_status,
    aa.verification_status
FROM allocation_account aa
LEFT JOIN funds_allocation_enhanced fa ON aa.allocation_id = fa.id
ORDER BY aa.total_charges_collected DESC;

-- ================================================================
-- STORED PROCEDURE: Update allocation charges
-- Triggered after charge processing
-- ================================================================

DELIMITER //

CREATE PROCEDURE IF NOT EXISTS sp_update_allocation_after_charge(
    IN p_allocation_id BIGINT,
    IN p_charge_amount DECIMAL(19, 2),
    IN p_charge_type VARCHAR(50)
)
BEGIN
    UPDATE funds_allocation_enhanced
    SET 
        total_charges_collected = total_charges_collected + p_charge_amount,
        charge_transaction_count = charge_transaction_count + 1,
        updated_at = NOW()
    WHERE id = p_allocation_id;
    
    UPDATE allocation_account
    SET 
        total_charges_collected = total_charges_collected + p_charge_amount,
        updated_at = NOW()
    WHERE allocation_id = p_allocation_id;
END //

DELIMITER ;

-- ================================================================
-- FOREIGN KEY CONSTRAINTS
-- ================================================================

ALTER TABLE allocation_account 
ADD CONSTRAINT fk_allocation_account_allocation 
FOREIGN KEY (allocation_id) 
REFERENCES funds_allocation_enhanced(id) ON DELETE CASCADE;

ALTER TABLE charge_transaction 
ADD CONSTRAINT fk_charge_allocation 
FOREIGN KEY (allocation_id) 
REFERENCES funds_allocation_enhanced(id) ON DELETE CASCADE;

ALTER TABLE charge_transaction 
ADD CONSTRAINT fk_charge_account 
FOREIGN KEY (allocation_account_id) 
REFERENCES allocation_account(id) ON DELETE CASCADE;

-- ================================================================
-- SAMPLE DATA FOR TESTING
-- ================================================================

-- Insert sample allocation account
INSERT INTO allocation_account (
    allocation_id, account_number, ifsc_code, account_holder_name,
    bank_name, account_type, branch_name, city, location, state,
    account_balance, total_allocated, verification_status, account_status,
    linked_by_admin_id, linked_by_admin_name, linked_at, created_at, updated_at
) VALUES (
    1, 'ACC-12345678901', 'HDFC0001234', 'Branch Manager Account',
    'HDFC Bank', 'CURRENT', 'Marine Lines', 'Mumbai', 'Downtown', 'Maharashtra',
    1000000.00, 5000000.00, 'VERIFIED', 'ACTIVE',
    1, 'HOD Admin', NOW(), NOW(), NOW()
);

-- ================================================================
-- INDEXES FOR PERFORMANCE
-- ================================================================

CREATE INDEX idx_charge_allocation_date ON charge_transaction(allocation_id, created_at DESC);
CREATE INDEX idx_charge_status_type ON charge_transaction(status, charge_type);
CREATE INDEX idx_allocation_account_status ON allocation_account(account_status, verification_status);

-- ================================================================
-- END OF SCHEMA
-- ================================================================
