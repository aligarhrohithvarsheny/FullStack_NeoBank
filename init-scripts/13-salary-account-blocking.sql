-- Add account blocking columns to salary_accounts table for 3-strike login blocking
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS failed_login_attempts INT DEFAULT 0;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS account_locked BOOLEAN DEFAULT FALSE;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS last_failed_login_time DATETIME NULL;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS lock_reason VARCHAR(255) DEFAULT NULL;
