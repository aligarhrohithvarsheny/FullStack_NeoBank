-- Migration 17: Add Employee Time Tracking Tables
-- Script to create time tracking and time management policy tables

-- Create employee_time_tracking table
CREATE TABLE IF NOT EXISTS employee_time_tracking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id VARCHAR(50) NOT NULL,
    admin_name VARCHAR(100),
    admin_email VARCHAR(100),
    id_card_number VARCHAR(50),
    tracking_date DATE NOT NULL,
    check_in_time DATETIME,
    check_out_time DATETIME,
    total_working_hours DECIMAL(5, 2),
    overtime_hours DECIMAL(5, 2),
    status ENUM('ACTIVE', 'CHECKED_OUT', 'ABSENT', 'ON_LEAVE') NOT NULL DEFAULT 'ACTIVE',
    remarks TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes for better query performance
    KEY idx_admin_date (admin_id, tracking_date),
    KEY idx_status (status),
    KEY idx_tracking_date (tracking_date),
    KEY idx_admin_id (admin_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create time_management_policy table
CREATE TABLE IF NOT EXISTS time_management_policy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id VARCHAR(50) NOT NULL,
    policy_name VARCHAR(100) NOT NULL,
    working_hours_per_day INT NOT NULL DEFAULT 8,
    check_in_time TIME NOT NULL,
    check_out_time TIME NOT NULL,
    grace_period_minutes INT DEFAULT 5,
    max_working_hours INT NOT NULL DEFAULT 10,
    overtime_multiplier DECIMAL(3, 2) DEFAULT 1.5,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes for better query performance
    KEY idx_admin_id (admin_id),
    KEY idx_is_active (is_active),
    KEY idx_created_at (created_at),
    
    -- Unique constraint on admin_id and policy_name
    UNIQUE KEY uk_admin_policy (admin_id, policy_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add audit log for time tracking changes
CREATE TABLE IF NOT EXISTS time_tracking_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    time_tracking_id BIGINT,
    admin_id VARCHAR(50),
    action VARCHAR(50),
    old_value JSON,
    new_value JSON,
    modified_by VARCHAR(100),
    modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    KEY idx_time_tracking_id (time_tracking_id),
    KEY idx_admin_id (admin_id),
    KEY idx_modified_at (modified_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default time management policies for system
INSERT INTO time_management_policy (admin_id, policy_name, working_hours_per_day, check_in_time, check_out_time, grace_period_minutes, max_working_hours, overtime_multiplier, is_active, description)
VALUES 
('SYSTEM', 'Standard 8-Hour Day', 8, '09:00:00', '17:00:00', 5, 10, 1.5, TRUE, 'Standard 8-hour working day from 9 AM to 5 PM with 1 hour lunch break'),
('SYSTEM', 'Flexible Schedule', 8, '08:00:00', '18:00:00', 15, 10, 1.5, TRUE, 'Flexible schedule allowing 8 hours of work between 8 AM and 6 PM'),
('SYSTEM', 'Extended Hours', 10, '08:00:00', '19:00:00', 10, 12, 2.0, TRUE, 'Extended working hours - 10 hours per day')
ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);

-- Create stored procedure to calculate daily attendance statistics
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS sp_get_daily_attendance_stats(IN p_date DATE)
BEGIN
    SELECT 
        p_date as attendance_date,
        COUNT(DISTINCT admin_id) as total_employees,
        SUM(CASE WHEN status IN ('ACTIVE', 'CHECKED_OUT') THEN 1 ELSE 0 END) as present_count,
        SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) as absent_count,
        SUM(CASE WHEN status = 'ON_LEAVE' THEN 1 ELSE 0 END) as on_leave_count,
        ROUND(AVG(CASE WHEN status = 'CHECKED_OUT' THEN total_working_hours ELSE NULL END), 2) as avg_working_hours,
        SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as overdue_checkouts
    FROM employee_time_tracking
    WHERE tracking_date = p_date;
END $$

CREATE PROCEDURE IF NOT EXISTS sp_get_employee_monthly_stats(IN p_admin_id VARCHAR(50), IN p_year INT, IN p_month INT)
BEGIN
    SELECT 
        admin_id,
        admin_name,
        YEAR(tracking_date) as year,
        MONTH(tracking_date) as month,
        COUNT(*) as total_days,
        SUM(CASE WHEN status IN ('ACTIVE', 'CHECKED_OUT') THEN 1 ELSE 0 END) as present_days,
        SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) as absent_days,
        SUM(CASE WHEN status = 'ON_LEAVE' THEN 1 ELSE 0 END) as leave_days,
        ROUND(SUM(CASE WHEN status = 'CHECKED_OUT' THEN total_working_hours ELSE 0 END), 2) as total_hours,
        ROUND(SUM(CASE WHEN status = 'CHECKED_OUT' THEN overtime_hours ELSE 0 END), 2) as total_overtime,
        ROUND((SUM(CASE WHEN status IN ('ACTIVE', 'CHECKED_OUT') THEN 1 ELSE 0 END) * 100.0) / COUNT(*), 2) as attendance_percentage
    FROM employee_time_tracking
    WHERE admin_id = p_admin_id 
    AND YEAR(tracking_date) = p_year 
    AND MONTH(tracking_date) = p_month
    GROUP BY admin_id, admin_name, YEAR(tracking_date), MONTH(tracking_date);
END $$

DELIMITER ;

-- Create views for quick access to statistics
CREATE OR REPLACE VIEW v_daily_attendance_summary AS
SELECT 
    tracking_date,
    COUNT(DISTINCT admin_id) as total_employees,
    SUM(CASE WHEN status IN ('ACTIVE', 'CHECKED_OUT') THEN 1 ELSE 0 END) as present_count,
    SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) as absent_count,
    SUM(CASE WHEN status = 'ON_LEAVE' THEN 1 ELSE 0 END) as on_leave_count,
    ROUND(AVG(CASE WHEN status = 'CHECKED_OUT' THEN total_working_hours ELSE NULL END), 2) as avg_working_hours
FROM employee_time_tracking
GROUP BY tracking_date
ORDER BY tracking_date DESC;

CREATE OR REPLACE VIEW v_employee_monthly_summary AS
SELECT 
    admin_id,
    admin_name,
    YEAR(tracking_date) as year,
    MONTH(tracking_date) as month,
    ROUND(SUM(CASE WHEN status = 'CHECKED_OUT' THEN total_working_hours ELSE 0 END), 2) as total_hours,
    ROUND(SUM(CASE WHEN status = 'CHECKED_OUT' THEN overtime_hours ELSE 0 END), 2) as total_overtime,
    COUNT(CASE WHEN status IN ('ACTIVE', 'CHECKED_OUT') THEN 1 END) as present_days,
    ROUND((COUNT(CASE WHEN status IN ('ACTIVE', 'CHECKED_OUT') THEN 1 END) * 100.0) / COUNT(*), 2) as attendance_percentage
FROM employee_time_tracking
GROUP BY admin_id, admin_name, YEAR(tracking_date), MONTH(tracking_date);

-- Add comments to tables for documentation
ALTER TABLE employee_time_tracking COMMENT = 'Stores daily check-in/check-out records for employees with calculated working hours';
ALTER TABLE time_management_policy COMMENT = 'Stores customizable time management policies for different admins/departments';
ALTER TABLE time_tracking_audit COMMENT = 'Audit trail for tracking changes to time records';

COMMIT;
