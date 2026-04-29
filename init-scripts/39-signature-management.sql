-- Signature Management: Add signature fields to all account tables

ALTER TABLE accounts ADD COLUMN IF NOT EXISTS signature_copy_path VARCHAR(500);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS signature_uploaded_at TIMESTAMP;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS signature_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS signature_verified_by VARCHAR(255);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS signature_verified_at TIMESTAMP;

ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS signature_copy_path VARCHAR(500);
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS signature_uploaded_at TIMESTAMP;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS signature_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS signature_verified_by VARCHAR(255);
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS signature_verified_at TIMESTAMP;

ALTER TABLE current_accounts ADD COLUMN IF NOT EXISTS signature_copy_path VARCHAR(500);
ALTER TABLE current_accounts ADD COLUMN IF NOT EXISTS signature_uploaded_at TIMESTAMP;
ALTER TABLE current_accounts ADD COLUMN IF NOT EXISTS signature_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE current_accounts ADD COLUMN IF NOT EXISTS signature_verified_by VARCHAR(255);
ALTER TABLE current_accounts ADD COLUMN IF NOT EXISTS signature_verified_at TIMESTAMP;
