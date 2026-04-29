-- Add cheque download tracking columns to cheque_requests table
ALTER TABLE cheque_requests
  ADD COLUMN IF NOT EXISTS cheque_downloaded BOOLEAN DEFAULT FALSE COMMENT 'Whether user downloaded the cheque',
  ADD COLUMN IF NOT EXISTS cheque_downloaded_at TIMESTAMP NULL COMMENT 'When user downloaded the cheque';
