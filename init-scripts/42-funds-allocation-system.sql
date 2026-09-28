-- ============================================================================
-- NeoBank Funds Allocation Feature - Database Initialization Script
-- ============================================================================
-- This script creates the three main tables for the funds allocation system
-- Run this after the base schema is created (or when spring.jpa.hibernate.ddl-auto=validate)
-- Database: springapp
-- MySQL Version: 8.0+
-- ============================================================================

-- ============================================================================
-- 1. FUNDS_ALLOCATION TABLE (Master Record)
-- ============================================================================
-- Stores the main fund allocation records from HOD to branch managers
-- Status: ACTIVE (in use) | PAUSED (temporarily halted) | COMPLETED (expired) | CANCELLED (user cancelled)
-- ============================================================================

CREATE TABLE IF NOT EXISTS funds_allocations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    allocation_id VARCHAR(50) NOT NULL UNIQUE,
    manager_id BIGINT NOT NULL,
    manager_name VARCHAR(255),
    manager_account_number VARCHAR(50),
    branch_name VARCHAR(255) NOT NULL,
    allocated_amount DECIMAL(19, 2) NOT NULL,
    allocation_type VARCHAR(50),
    product_types TEXT COMMENT 'Comma-separated: GOLD_LOAN,DEPOSITS,WITHDRAWALS,LOANS,OVERDRAFT,SALARY_CREDITS',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, PAUSED, COMPLETED, CANCELLED',
    current_balance DECIMAL(19, 2) NOT NULL,
    allocation_account_id BIGINT,
    linked_account_number VARCHAR(50),
    linked_ifsc_code VARCHAR(20),
    linked_account_holder_name VARCHAR(100),
    linked_bank_name VARCHAR(100),
    linked_account_type VARCHAR(30),
    account_status VARCHAR(30) NOT NULL DEFAULT 'NOT_LINKED',
    account_verification_status VARCHAR(20) DEFAULT 'PENDING',
    account_verified_at DATETIME,
    linked_by_admin_id BIGINT,
    linked_by_admin_name VARCHAR(100),
    linked_at DATETIME,
    cheque_verification_status VARCHAR(20) DEFAULT 'PENDING',
    linked_cheque_number VARCHAR(50),
    cheque_holder_name VARCHAR(100),
    cheque_date DATE,
    cheque_bank VARCHAR(100),
    cheque_image_url LONGTEXT,
    cheque_verification_notes VARCHAR(500),
    verified_by_admin_id BIGINT,
    verified_by_admin_name VARCHAR(100),
    verified_at DATETIME,
    charge_management_enabled BOOLEAN DEFAULT FALSE,
    total_charges_collected DECIMAL(19, 2) DEFAULT 0,
    interest_charges DECIMAL(19, 2) DEFAULT 0,
    cibil_charges DECIMAL(19, 2) DEFAULT 0,
    soundbox_charges DECIMAL(19, 2) DEFAULT 0,
    upi_charges DECIMAL(19, 2) DEFAULT 0,
    payment_gateway_charges DECIMAL(19, 2) DEFAULT 0,
    other_charges DECIMAL(19, 2) DEFAULT 0,
    charge_transaction_count INT DEFAULT 0,
    total_debited DECIMAL(19, 2) DEFAULT 0,
    total_credited DECIMAL(19, 2) DEFAULT 0,
    total_utilized DECIMAL(19, 2) DEFAULT 0,
    allocation_date DATE,
    valid_from DATE,
    valid_till DATE NOT NULL,
    description TEXT,
    allocated_by_admin_id BIGINT,
    allocated_by_name VARCHAR(255),
    cancelled_at DATETIME,
    cancelled_reason TEXT,
    cancelled_by_admin_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes for query performance
    INDEX idx_allocation_id (allocation_id),
    INDEX idx_manager_id (manager_id),
    INDEX idx_status (status),
    INDEX idx_manager_status (manager_id, status),
    INDEX idx_allocation_date (allocation_date),
    INDEX idx_valid_till (valid_till),
    INDEX idx_branch_name (branch_name),
    
    -- Foreign key constraints
    CONSTRAINT fk_manager_account FOREIGN KEY (manager_account_number) 
        REFERENCES account(account_number) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Master table for fund allocations from HOD to branch managers';

-- ============================================================================
-- 2. ALLOCATION_UTILIZATION TABLE (Transaction Ledger)
-- ============================================================================
-- Records every debit/credit transaction against an allocation
-- Maintains complete audit trail of fund usage
-- ============================================================================

CREATE TABLE IF NOT EXISTS allocation_utilization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    utilization_id VARCHAR(50) NOT NULL UNIQUE,
    allocation_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL COMMENT 'DEBIT or CREDIT',
    amount DECIMAL(19, 2) NOT NULL,
    remaining_balance DECIMAL(19, 2) NOT NULL,
    linked_transaction_id VARCHAR(100),
    product_type VARCHAR(50) NOT NULL COMMENT 'GOLD_LOAN, DEPOSITS, WITHDRAWALS, LOANS, OVERDRAFT, SALARY_CREDIT',
    user_account_number VARCHAR(50),
    user_name VARCHAR(255),
    description TEXT,
    performed_by_admin_id BIGINT,
    transaction_date DATETIME NOT NULL,
    status VARCHAR(20) DEFAULT 'SUCCESS' COMMENT 'SUCCESS, PENDING, FAILED',
    reference_number VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes for query performance
    INDEX idx_utilization_id (utilization_id),
    INDEX idx_allocation_id (allocation_id),
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_product_type (product_type),
    INDEX idx_transaction_date (transaction_date DESC),
    INDEX idx_allocation_date (allocation_id, transaction_date DESC),
    INDEX idx_user_account (user_account_number),
    
    -- Foreign key constraint
    CONSTRAINT fk_allocation_utilization FOREIGN KEY (allocation_id)
        REFERENCES funds_allocations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Ledger table tracking all debit/credit transactions against allocations';

-- ============================================================================
-- 3. ALLOCATION_METRICS TABLE (Real-Time KPIs)
-- ============================================================================
-- Maintains real-time aggregated metrics for dashboard KPIs
-- Updated on each debit/credit transaction
-- Avoids expensive aggregation queries during report generation
-- ============================================================================

CREATE TABLE IF NOT EXISTS allocation_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    allocation_id BIGINT NOT NULL UNIQUE,
    allocation_id_str VARCHAR(50),
    total_allocated DECIMAL(19, 2) NOT NULL,
    total_debited DECIMAL(19, 2) DEFAULT 0,
    total_credited DECIMAL(19, 2) DEFAULT 0,
    current_balance DECIMAL(19, 2) NOT NULL,
    utilization_percentage DECIMAL(5, 2) DEFAULT 0,
    debit_count INT DEFAULT 0,
    credit_count INT DEFAULT 0,
    total_transactions INT DEFAULT 0,
    last_transaction_date DATETIME,
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metrics_date DATE NOT NULL DEFAULT CURDATE(),
    
    -- Product-specific debit tracking (for dashboard breakdown)
    gold_loan_debited DECIMAL(19, 2) DEFAULT 0,
    deposits_debited DECIMAL(19, 2) DEFAULT 0,
    withdrawals_debited DECIMAL(19, 2) DEFAULT 0,
    loans_debited DECIMAL(19, 2) DEFAULT 0,
    overdraft_debited DECIMAL(19, 2) DEFAULT 0,
    salary_credit_debited DECIMAL(19, 2) DEFAULT 0,
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_allocation_id (allocation_id),
    INDEX idx_metrics_date (metrics_date),
    INDEX idx_allocation_date (allocation_id, metrics_date),
    INDEX idx_utilization (utilization_percentage DESC),
    
    -- Foreign key constraint
    CONSTRAINT fk_allocation_metrics FOREIGN KEY (allocation_id)
        REFERENCES funds_allocations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Real-time metrics and KPI aggregation for dashboard queries';

-- ============================================================================
-- UTILITY VIEWS (Optional - for easier reporting)
-- ============================================================================

-- View: Active Allocations Summary
CREATE OR REPLACE VIEW v_active_allocations AS
SELECT 
    fa.id,
    fa.allocation_id,
    fa.manager_id,
    fa.manager_name,
    fa.branch_name,
    fa.allocated_amount,
    fa.product_types,
    am.total_debited,
    am.current_balance,
    am.utilization_percentage,
    fa.valid_till,
    DATEDIFF(fa.valid_till, CURDATE()) as days_remaining
FROM funds_allocations fa
LEFT JOIN allocation_metrics am ON fa.id = am.allocation_id
WHERE fa.status = 'ACTIVE'
AND fa.valid_till >= CURDATE()
ORDER BY fa.updated_at DESC;

-- View: Manager Allocation Summary
CREATE OR REPLACE VIEW v_manager_allocations AS
SELECT 
    fa.manager_id,
    fa.manager_name,
    COUNT(DISTINCT fa.id) as allocation_count,
    SUM(fa.allocated_amount) as total_allocated,
    SUM(am.total_debited) as total_debited,
    SUM(am.current_balance) as total_balance,
    ROUND(SUM(am.total_debited) / NULLIF(SUM(fa.allocated_amount), 0) * 100, 2) as overall_utilization
FROM funds_allocations fa
LEFT JOIN allocation_metrics am ON fa.id = am.allocation_id
WHERE fa.status IN ('ACTIVE', 'PAUSED')
GROUP BY fa.manager_id, fa.manager_name;

-- View: Branch-wise Fund Status
CREATE OR REPLACE VIEW v_branch_funds_summary AS
SELECT 
    fa.branch_name,
    COUNT(DISTINCT fa.manager_id) as manager_count,
    COUNT(DISTINCT fa.id) as allocation_count,
    SUM(CASE WHEN fa.status = 'ACTIVE' THEN fa.allocated_amount ELSE 0 END) as active_allocation,
    SUM(am.current_balance) as available_balance,
    SUM(am.total_debited) as utilized_amount,
    ROUND(SUM(am.total_debited) / NULLIF(SUM(fa.allocated_amount), 0) * 100, 2) as utilization_percentage
FROM funds_allocations fa
LEFT JOIN allocation_metrics am ON fa.id = am.allocation_id
WHERE fa.status != 'CANCELLED'
GROUP BY fa.branch_name
ORDER BY utilized_amount DESC;

-- ============================================================================
-- STORED PROCEDURES (Optional - for complex operations)
-- ============================================================================

-- Procedure: Update Metrics after Transaction
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS sp_update_allocation_metrics(
    IN p_allocation_id BIGINT,
    IN p_product_type VARCHAR(50)
)
BEGIN
    DECLARE v_total_debited DECIMAL(19, 2);
    DECLARE v_total_credited DECIMAL(19, 2);
    DECLARE v_current_balance DECIMAL(19, 2);
    DECLARE v_allocated DECIMAL(19, 2);
    DECLARE v_product_debited DECIMAL(19, 2);
    
    -- Get allocation amount
    SELECT allocated_amount INTO v_allocated FROM funds_allocations WHERE id = p_allocation_id;
    
    -- Calculate totals
    SELECT COALESCE(SUM(amount), 0) INTO v_total_debited 
        FROM allocation_utilization 
        WHERE allocation_id = p_allocation_id AND transaction_type = 'DEBIT';
    
    SELECT COALESCE(SUM(amount), 0) INTO v_total_credited 
        FROM allocation_utilization 
        WHERE allocation_id = p_allocation_id AND transaction_type = 'CREDIT';
    
    SET v_current_balance = v_allocated - v_total_debited + v_total_credited;
    
    -- Get product-specific debit
    SELECT COALESCE(SUM(amount), 0) INTO v_product_debited 
        FROM allocation_utilization 
        WHERE allocation_id = p_allocation_id 
        AND transaction_type = 'DEBIT' 
        AND product_type = p_product_type;
    
    -- Update or insert metrics
    INSERT INTO allocation_metrics (
        allocation_id, total_allocated, total_debited, total_credited, 
        current_balance, utilization_percentage, debit_count, credit_count,
        total_transactions, last_updated
    ) VALUES (
        p_allocation_id, v_allocated, v_total_debited, v_total_credited,
        v_current_balance, 
        ROUND((v_total_debited / v_allocated) * 100, 2),
        (SELECT COUNT(*) FROM allocation_utilization WHERE allocation_id = p_allocation_id AND transaction_type = 'DEBIT'),
        (SELECT COUNT(*) FROM allocation_utilization WHERE allocation_id = p_allocation_id AND transaction_type = 'CREDIT'),
        (SELECT COUNT(*) FROM allocation_utilization WHERE allocation_id = p_allocation_id),
        NOW()
    )
    ON DUPLICATE KEY UPDATE
        total_debited = v_total_debited,
        total_credited = v_total_credited,
        current_balance = v_current_balance,
        utilization_percentage = ROUND((v_total_debited / v_allocated) * 100, 2),
        debit_count = (SELECT COUNT(*) FROM allocation_utilization WHERE allocation_id = p_allocation_id AND transaction_type = 'DEBIT'),
        credit_count = (SELECT COUNT(*) FROM allocation_utilization WHERE allocation_id = p_allocation_id AND transaction_type = 'CREDIT'),
        total_transactions = (SELECT COUNT(*) FROM allocation_utilization WHERE allocation_id = p_allocation_id),
        last_updated = NOW();
        
    -- Update product-specific debit field
    CASE p_product_type
        WHEN 'GOLD_LOAN' THEN 
            UPDATE allocation_metrics SET gold_loan_debited = v_product_debited WHERE allocation_id = p_allocation_id;
        WHEN 'DEPOSITS' THEN 
            UPDATE allocation_metrics SET deposits_debited = v_product_debited WHERE allocation_id = p_allocation_id;
        WHEN 'WITHDRAWALS' THEN 
            UPDATE allocation_metrics SET withdrawals_debited = v_product_debited WHERE allocation_id = p_allocation_id;
        WHEN 'LOANS' THEN 
            UPDATE allocation_metrics SET loans_debited = v_product_debited WHERE allocation_id = p_allocation_id;
        WHEN 'OVERDRAFT' THEN 
            UPDATE allocation_metrics SET overdraft_debited = v_product_debited WHERE allocation_id = p_allocation_id;
        WHEN 'SALARY_CREDIT' THEN 
            UPDATE allocation_metrics SET salary_credit_debited = v_product_debited WHERE allocation_id = p_allocation_id;
    END CASE;
    
    -- Update fund allocation totals
    UPDATE funds_allocations 
    SET total_debited = v_total_debited,
        total_credited = v_total_credited,
        total_utilized = v_total_debited - v_total_credited,
        current_balance = v_current_balance,
        updated_at = NOW()
    WHERE id = p_allocation_id;
END$$

DELIMITER ;

-- ============================================================================
-- DATA VERIFICATION QUERIES
-- ============================================================================

-- Check total allocations
-- SELECT COUNT(*) as total_allocations FROM funds_allocations;

-- Check manager allocations
-- SELECT manager_id, manager_name, COUNT(*) as allocations, SUM(allocated_amount) as total 
-- FROM funds_allocations WHERE status = 'ACTIVE' GROUP BY manager_id;

-- Check transaction volume
-- SELECT COUNT(*) as total_transactions FROM allocation_utilization;

-- Check metrics consistency
-- SELECT fa.id, fa.allocation_id, am.total_debited, am.current_balance, am.utilization_percentage
-- FROM funds_allocations fa 
-- LEFT JOIN allocation_metrics am ON fa.id = am.allocation_id 
-- WHERE fa.status = 'ACTIVE';

-- ============================================================================
-- SAMPLE DATA (Optional - for testing)
-- ============================================================================

-- Uncomment below to insert sample data for testing

/*
-- Insert sample allocation
INSERT INTO funds_allocations (
    allocation_id, manager_id, manager_name, manager_account_number, branch_name,
    allocated_amount, allocation_type, product_types, status, current_balance,
    allocation_date, valid_from, valid_till, allocated_by_admin_id, allocated_by_name
) VALUES (
    'FA-2025-00001', 5, 'Raj Kumar', 'ACC00005', 'Mumbai Branch',
    1000000, 'GENERAL', 'GOLD_LOAN,DEPOSITS,WITHDRAWALS,LOANS,OVERDRAFT,SALARY_CREDITS', 
    'ACTIVE', 1000000,
    CURDATE(), CURDATE(), DATE_ADD(CURDATE(), INTERVAL 365 DAY), 1, 'HOD Admin'
);

-- Get the allocation ID
SET @alloc_id = LAST_INSERT_ID();

-- Insert sample metrics
INSERT INTO allocation_metrics (
    allocation_id, allocation_id_str, total_allocated, current_balance, utilization_percentage
) VALUES (
    @alloc_id, 'FA-2025-00001', 1000000, 1000000, 0
);

-- Insert sample transaction
INSERT INTO allocation_utilization (
    utilization_id, allocation_id, transaction_type, amount, remaining_balance,
    linked_transaction_id, product_type, user_account_number, user_name,
    transaction_date, status, description, performed_by_admin_id
) VALUES (
    'AU-2025-00001', @alloc_id, 'DEBIT', 50000, 950000,
    'TXN-2025-001', 'GOLD_LOAN', 'ACC00123', 'John Doe',
    NOW(), 'SUCCESS', 'Gold Loan Disbursement', 5
);

-- Update metrics after transaction
CALL sp_update_allocation_metrics(@alloc_id, 'GOLD_LOAN');
*/

-- ============================================================================
-- SCRIPT COMPLETION
-- ============================================================================
-- Tables created successfully!
-- Views created for common reporting queries
-- Stored procedure ready for metrics updates
-- Ready for Spring Boot application integration
-- ============================================================================
