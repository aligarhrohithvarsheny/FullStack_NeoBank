-- Add account closure columns to salary_accounts table
USE springapp;

ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS closed_at DATETIME NULL;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS closed_reason VARCHAR(500) NULL;
ALTER TABLE salary_accounts ADD COLUMN IF NOT EXISTS closed_by VARCHAR(255) NULL;
