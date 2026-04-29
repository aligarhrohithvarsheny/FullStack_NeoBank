-- =====================================================
-- AGENT PROFILE ENHANCEMENTS
-- =====================================================

-- Add new columns to agents table for profile management
ALTER TABLE agents ADD COLUMN IF NOT EXISTS role VARCHAR(50) DEFAULT 'FIELD_AGENT';
ALTER TABLE agents ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE agents ADD COLUMN IF NOT EXISTS profile_photo_path VARCHAR(500);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS id_card_path VARCHAR(500);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS otp VARCHAR(10);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS otp_expiry TIMESTAMP NULL;
ALTER TABLE agents ADD COLUMN IF NOT EXISTS frozen_at TIMESTAMP NULL;
ALTER TABLE agents ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP NULL;

-- Add index on status for faster queries
CREATE INDEX IF NOT EXISTS idx_agent_status ON agents(status);
