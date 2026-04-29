-- Video KYC Slot Booking & Verification Number System

-- Available Time Slots (Admin-created)
CREATE TABLE IF NOT EXISTS video_kyc_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slot_date DATE NOT NULL,
    slot_time TIME NOT NULL,
    slot_end_time TIME NOT NULL,
    max_bookings INT DEFAULT 5,
    current_bookings INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_slot_datetime (slot_date, slot_time),
    INDEX idx_slot_date (slot_date),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add slot and verification fields to video_kyc_sessions
ALTER TABLE video_kyc_sessions
    ADD COLUMN verification_number VARCHAR(30) UNIQUE AFTER ifsc_code,
    ADD COLUMN booked_slot_id BIGINT AFTER verification_number,
    ADD COLUMN slot_date DATE AFTER booked_slot_id,
    ADD COLUMN slot_time TIME AFTER slot_date,
    ADD COLUMN slot_end_time TIME AFTER slot_time,
    ADD INDEX idx_verification_number (verification_number),
    ADD INDEX idx_booked_slot (booked_slot_id);
