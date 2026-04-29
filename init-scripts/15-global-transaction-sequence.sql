-- =====================================================================
-- Global Transaction ID Sequence Implementation
-- This script adds support for globally incrementing transaction IDs
-- across all transaction types in the banking system
-- =====================================================================

-- 1. Create the global transaction sequence table
CREATE TABLE IF NOT EXISTS global_transaction_sequence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    current_sequence BIGINT NOT NULL UNIQUE,
    transaction_type VARCHAR(255),
    last_transaction_id VARCHAR(255),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Initialize the sequence with starting value of 1
INSERT INTO global_transaction_sequence (current_sequence, created_at, last_updated) 
VALUES (1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE current_sequence = 1;

-- =====================================================================
-- Add global_transaction_sequence column to all transaction tables
-- =====================================================================

-- 2. Add to transactions table
ALTER TABLE transactions ADD COLUMN global_transaction_sequence BIGINT AFTER id;
ALTER TABLE transactions ADD INDEX idx_global_txn_sequence (global_transaction_sequence);

-- 3. Add to bill_payments table
ALTER TABLE bill_payments ADD COLUMN global_transaction_sequence BIGINT AFTER id;
ALTER TABLE bill_payments ADD INDEX idx_global_bill_payment_sequence (global_transaction_sequence);

-- 4. Add to transfer_records table
ALTER TABLE transfer_records ADD COLUMN global_transaction_sequence BIGINT AFTER id;
ALTER TABLE transfer_records ADD INDEX idx_global_transfer_sequence (global_transaction_sequence);

-- 5. Add to salary_normal_transactions table
ALTER TABLE salary_normal_transactions ADD COLUMN global_transaction_sequence BIGINT AFTER id;
ALTER TABLE salary_normal_transactions ADD INDEX idx_global_salary_normal_sequence (global_transaction_sequence);

-- 6. Add to salary_upi_transactions table
ALTER TABLE salary_upi_transactions ADD COLUMN global_transaction_sequence BIGINT AFTER id;
ALTER TABLE salary_upi_transactions ADD INDEX idx_global_salary_upi_sequence (global_transaction_sequence);

-- 7. Add to credit_card_transactions table
ALTER TABLE credit_card_transactions ADD COLUMN global_transaction_sequence BIGINT AFTER id;
ALTER TABLE credit_card_transactions ADD INDEX idx_global_cc_txn_sequence (global_transaction_sequence);

-- 8. Add to fasttag_transactions table
ALTER TABLE fasttag_transactions ADD COLUMN global_transaction_sequence BIGINT AFTER id;
ALTER TABLE fasttag_transactions ADD INDEX idx_global_fasttag_sequence (global_transaction_sequence);

-- 9. Add to business_transactions table
ALTER TABLE business_transactions ADD COLUMN global_transaction_sequence BIGINT AFTER id;
ALTER TABLE business_transactions ADD INDEX idx_global_business_sequence (global_transaction_sequence);

-- 10. Add to salary_transactions table
ALTER TABLE salary_transactions ADD COLUMN global_transaction_sequence BIGINT AFTER id;
ALTER TABLE salary_transactions ADD INDEX idx_global_salary_sequence (global_transaction_sequence);

-- =====================================================================
-- Optional: Create a view for easy access to all transactions with their global IDs
-- =====================================================================

CREATE OR REPLACE VIEW all_bank_transactions AS
SELECT 
    'NORMAL' AS transaction_type,
    t.id,
    t.global_transaction_sequence,
    t.transaction_id,
    t.user_name,
    t.account_number,
    t.amount,
    t.type,
    t.status,
    t.date AS transaction_date
FROM transactions t
WHERE t.global_transaction_sequence IS NOT NULL

UNION ALL

SELECT 
    'BILL_PAYMENT',
    bp.id,
    bp.global_transaction_sequence,
    bp.transaction_id,
    bp.user_name,
    bp.account_number,
    bp.amount,
    bp.bill_type,
    bp.status,
    bp.payment_date
FROM bill_payments bp
WHERE bp.global_transaction_sequence IS NOT NULL

UNION ALL

SELECT 
    'TRANSFER',
    tr.id,
    tr.global_transaction_sequence,
    tr.transfer_id,
    tr.sender_name,
    tr.sender_account_number,
    tr.amount,
    CONCAT(tr.transfer_type, ' Transfer'),
    tr.status,
    tr.date
FROM transfer_records tr
WHERE tr.global_transaction_sequence IS NOT NULL

UNION ALL

SELECT 
    'SALARY_NORMAL',
    snt.id,
    snt.global_transaction_sequence,
    NULL,
    NULL,
    snt.account_number,
    snt.amount,
    snt.type,
    snt.status,
    snt.created_at
FROM salary_normal_transactions snt
WHERE snt.global_transaction_sequence IS NOT NULL

UNION ALL

SELECT 
    'SALARY_UPI',
    sut.id,
    sut.global_transaction_sequence,
    sut.transaction_ref,
    sut.recipient_name,
    sut.account_number,
    sut.amount,
    sut.type,
    sut.status,
    sut.created_at
FROM salary_upi_transactions sut
WHERE sut.global_transaction_sequence IS NOT NULL

UNION ALL

SELECT 
    'CREDIT_CARD',
    cct.id,
    cct.global_transaction_sequence,
    NULL,
    cct.user_name,
    cct.account_number,
    cct.amount,
    cct.transaction_type,
    cct.status,
    cct.transaction_date
FROM credit_card_transactions cct
WHERE cct.global_transaction_sequence IS NOT NULL

UNION ALL

SELECT 
    'FASTTAG',
    ft.id,
    ft.global_transaction_sequence,
    NULL,
    ft.initiated_by,
    NULL,
    ft.amount,
    ft.type,
    NULL,
    ft.created_at
FROM fasttag_transactions ft
WHERE ft.global_transaction_sequence IS NOT NULL

UNION ALL

SELECT 
    'BUSINESS',
    bt.id,
    bt.global_transaction_sequence,
    bt.txn_id,
    NULL,
    bt.account_number,
    bt.amount,
    bt.txn_type,
    bt.status,
    bt.date
FROM business_transactions bt
WHERE bt.global_transaction_sequence IS NOT NULL

UNION ALL

SELECT 
    'SALARY',
    st.id,
    st.global_transaction_sequence,
    NULL,
    NULL,
    st.account_number,
    st.salary_amount,
    st.type,
    st.status,
    st.credit_date
FROM salary_transactions st
WHERE st.global_transaction_sequence IS NOT NULL

ORDER BY global_transaction_sequence DESC;

-- =====================================================================
-- Create index for faster lookup by global transaction sequence
-- =====================================================================

CREATE INDEX idx_global_txn_lookup ON global_transaction_sequence(current_sequence);
