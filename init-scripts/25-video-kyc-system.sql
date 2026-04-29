-- Video KYC System Tables
-- Supports account opening with Video KYC verification

-- Video KYC Sessions
CREATE TABLE IF NOT EXISTS video_kyc_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- User/Account info
    customer_id VARCHAR(50),
    full_name VARCHAR(255) NOT NULL,
    mobile_number VARCHAR(15) NOT NULL,
    email VARCHAR(255) NOT NULL,
    address_city VARCHAR(100),
    address_state VARCHAR(100),
    account_type VARCHAR(20) DEFAULT 'Savings',
    temporary_account_number VARCHAR(50) UNIQUE,
    final_account_number VARCHAR(50),
    ifsc_code VARCHAR(20) DEFAULT 'NEOB0000001',
    
    -- Document info
    aadhar_document LONGBLOB,
    aadhar_document_name VARCHAR(255),
    aadhar_document_type VARCHAR(100),
    pan_document LONGBLOB,
    pan_document_name VARCHAR(255),
    pan_document_type VARCHAR(100),
    aadhar_number VARCHAR(20),
    pan_number VARCHAR(15),
    
    -- Video KYC specifics
    room_id VARCHAR(100) UNIQUE,
    kyc_status VARCHAR(30) DEFAULT 'Pending',
    otp_code VARCHAR(10),
    otp_verified BOOLEAN DEFAULT FALSE,
    
    -- Snapshots
    face_snapshot LONGBLOB,
    face_snapshot_type VARCHAR(100),
    id_snapshot LONGBLOB,
    id_snapshot_type VARCHAR(100),
    
    -- Liveness check
    liveness_check_passed BOOLEAN DEFAULT FALSE,
    liveness_check_type VARCHAR(50),
    
    -- Rejection info
    rejection_reason TEXT,
    
    -- Admin info
    assigned_admin_id BIGINT,
    assigned_admin_name VARCHAR(255),
    
    -- Attempt tracking
    kyc_attempt_count INT DEFAULT 1,
    max_attempts INT DEFAULT 3,
    
    -- Session control
    session_active BOOLEAN DEFAULT FALSE,
    session_started_at DATETIME,
    session_ended_at DATETIME,
    session_duration_seconds INT,
    
    -- Timestamps
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    approved_at DATETIME,
    
    -- Link to main user account (after approval)
    user_id BIGINT,
    account_id BIGINT,
    
    INDEX idx_kyc_status (kyc_status),
    INDEX idx_mobile (mobile_number),
    INDEX idx_email (email),
    INDEX idx_room_id (room_id),
    INDEX idx_temp_account (temporary_account_number),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Video KYC Audit Logs
CREATE TABLE IF NOT EXISTS video_kyc_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    admin_id BIGINT,
    admin_name VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (session_id) REFERENCES video_kyc_sessions(id) ON DELETE CASCADE,
    INDEX idx_session_id (session_id),
    INDEX idx_admin_id (admin_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
