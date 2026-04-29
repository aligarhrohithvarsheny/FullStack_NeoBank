-- Add browser_name and account_type columns to session_history table for login activity tracking
ALTER TABLE session_history ADD COLUMN IF NOT EXISTS browser_name VARCHAR(50);
ALTER TABLE session_history ADD COLUMN IF NOT EXISTS account_type VARCHAR(50);

-- Update existing records: set account_type based on user_type
UPDATE session_history SET account_type = 'ADMIN' WHERE user_type = 'ADMIN' AND account_type IS NULL;
UPDATE session_history SET account_type = 'MANAGER' WHERE user_type = 'MANAGER' AND account_type IS NULL;
UPDATE session_history SET account_type = 'SAVINGS' WHERE user_type = 'USER' AND account_type IS NULL;
