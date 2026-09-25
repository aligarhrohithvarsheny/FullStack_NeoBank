-- Performance Optimization Indexes
-- This migration adds indexes on frequently searched columns to improve query performance

-- Account indexes
CREATE INDEX IF NOT EXISTS idx_accounts_phone ON accounts(phone);
CREATE INDEX IF NOT EXISTS idx_accounts_aadhar ON accounts(aadhar_number);
CREATE INDEX IF NOT EXISTS idx_accounts_pan ON accounts(pan);
CREATE INDEX IF NOT EXISTS idx_accounts_account_number ON accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_accounts_barcode ON accounts(barcode_number);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts(status);
CREATE INDEX IF NOT EXISTS idx_accounts_customer_id ON accounts(customer_id);

-- User indexes
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_account_number ON users(account_number);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Loan indexes
CREATE INDEX IF NOT EXISTS idx_loans_account_number ON loans(account_number);
CREATE INDEX IF NOT EXISTS idx_loans_status ON loans(status);

-- Transaction indexes
CREATE INDEX IF NOT EXISTS idx_transactions_account_number ON transactions(account_number);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(date);

-- Cheque indexes
CREATE INDEX IF NOT EXISTS idx_cheques_account_number ON cheques(account_number);
CREATE INDEX IF NOT EXISTS idx_cheques_cheque_number ON cheques(cheque_number);
CREATE INDEX IF NOT EXISTS idx_cheques_status ON cheques(status);

-- Salary Account indexes
CREATE INDEX IF NOT EXISTS idx_salary_accounts_account_number ON salary_accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_salary_accounts_phone ON salary_accounts(mobile_number);
CREATE INDEX IF NOT EXISTS idx_salary_accounts_email ON salary_accounts(email);

-- Current Account indexes
CREATE INDEX IF NOT EXISTS idx_current_accounts_account_number ON current_accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_current_accounts_mobile ON current_accounts(mobile);

-- Card indexes
CREATE INDEX IF NOT EXISTS idx_cards_account_number ON cards(account_number);

-- Fixed Deposit indexes
CREATE INDEX IF NOT EXISTS idx_fixed_deposits_account_number ON fixed_deposits(account_number);

-- Gold Loan indexes
CREATE INDEX IF NOT EXISTS idx_gold_loans_account_number ON gold_loans(account_number);

-- Demand Draft indexes
CREATE INDEX IF NOT EXISTS idx_demand_drafts_account_number ON savings_demand_drafts(account_number);

-- Composite indexes for common search patterns
CREATE INDEX IF NOT EXISTS idx_accounts_status_phone ON accounts(status, phone);
CREATE INDEX IF NOT EXISTS idx_accounts_status_aadhar ON accounts(status, aadhar_number);
