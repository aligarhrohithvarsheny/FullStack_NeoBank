-- AI Security System Tables for NeoBank
-- Advanced AI-powered security across all banking channels

-- 1. AI Security Events - Central event log for all AI-detected security events
CREATE TABLE IF NOT EXISTS ai_security_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    risk_score DOUBLE NOT NULL DEFAULT 0.0,
    source_entity_id VARCHAR(100),
    source_entity_type VARCHAR(30),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    details_json TEXT,
    client_ip VARCHAR(50),
    location VARCHAR(255),
    device_fingerprint VARCHAR(255),
    user_agent TEXT,
    session_id VARCHAR(100),
    ai_model_version VARCHAR(50) DEFAULT 'v1.0',
    ai_confidence DOUBLE DEFAULT 0.0,
    status VARCHAR(30) NOT NULL DEFAULT 'DETECTED',
    action_taken VARCHAR(50),
    resolved_by VARCHAR(100),
    resolved_at DATETIME,
    resolution_notes TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ai_event_type (event_type),
    INDEX idx_ai_event_channel (channel),
    INDEX idx_ai_event_severity (severity),
    INDEX idx_ai_event_status (status),
    INDEX idx_ai_event_created (created_at),
    INDEX idx_ai_event_source (source_entity_id, source_entity_type),
    INDEX idx_ai_event_risk (risk_score)
);

-- 2. AI Threat Scores - Real-time risk scoring per user/session
CREATE TABLE IF NOT EXISTS ai_threat_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_id VARCHAR(100) NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    overall_risk_score DOUBLE NOT NULL DEFAULT 0.0,
    login_risk_score DOUBLE DEFAULT 0.0,
    transaction_risk_score DOUBLE DEFAULT 0.0,
    behavioral_risk_score DOUBLE DEFAULT 0.0,
    device_risk_score DOUBLE DEFAULT 0.0,
    network_risk_score DOUBLE DEFAULT 0.0,
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    risk_factors TEXT,
    last_activity VARCHAR(255),
    total_events INT DEFAULT 0,
    false_positives INT DEFAULT 0,
    confirmed_threats INT DEFAULT 0,
    is_watchlisted BOOLEAN DEFAULT FALSE,
    watchlist_reason VARCHAR(255),
    last_evaluated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_threat_entity (entity_id, entity_type),
    INDEX idx_threat_risk_level (risk_level),
    INDEX idx_threat_score (overall_risk_score),
    INDEX idx_threat_watchlist (is_watchlisted)
);

-- 3. AI Device Fingerprints - Known device tracking for anomaly detection
CREATE TABLE IF NOT EXISTS ai_device_fingerprints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_id VARCHAR(100) NOT NULL,
    entity_type VARCHAR(30) NOT NULL DEFAULT 'USER',
    device_hash VARCHAR(255) NOT NULL,
    device_type VARCHAR(50),
    browser VARCHAR(100),
    os VARCHAR(100),
    screen_resolution VARCHAR(30),
    timezone VARCHAR(50),
    language VARCHAR(20),
    ip_address VARCHAR(50),
    geo_location VARCHAR(100),
    is_trusted BOOLEAN DEFAULT FALSE,
    trust_score DOUBLE DEFAULT 50.0,
    login_count INT DEFAULT 0,
    last_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    first_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_entity (entity_id, entity_type),
    INDEX idx_device_hash (device_hash),
    INDEX idx_device_trusted (is_trusted)
);

-- 4. AI Security Rules - Configurable rules engine
CREATE TABLE IF NOT EXISTS ai_security_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL UNIQUE,
    rule_category VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'ALL',
    description TEXT,
    condition_json TEXT NOT NULL,
    action_type VARCHAR(50) NOT NULL DEFAULT 'ALERT',
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    is_active BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 5,
    hit_count BIGINT DEFAULT 0,
    last_triggered_at DATETIME,
    created_by VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rule_category (rule_category),
    INDEX idx_rule_channel (channel),
    INDEX idx_rule_active (is_active)
);

-- 5. AI Security Metrics (aggregated for dashboard)
CREATE TABLE IF NOT EXISTS ai_security_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_date DATE NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'ALL',
    total_events INT DEFAULT 0,
    critical_events INT DEFAULT 0,
    high_events INT DEFAULT 0,
    medium_events INT DEFAULT 0,
    low_events INT DEFAULT 0,
    blocked_threats INT DEFAULT 0,
    false_positives INT DEFAULT 0,
    avg_risk_score DOUBLE DEFAULT 0.0,
    unique_entities INT DEFAULT 0,
    new_devices INT DEFAULT 0,
    suspicious_logins INT DEFAULT 0,
    anomalous_transactions INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_metric_date_channel (metric_date, channel),
    INDEX idx_metric_date (metric_date)
);

-- Insert default AI Security Rules
INSERT INTO ai_security_rules (rule_name, rule_category, channel, description, condition_json, action_type, severity, priority) VALUES
('HIGH_VALUE_TRANSFER', 'TRANSACTION', 'ALL', 'Flag transfers exceeding 50% of account balance or over 1 lakh', '{"type":"threshold","field":"amount","operator":"gt","value":100000}', 'ALERT', 'HIGH', 1),
('RAPID_FIRE_TRANSACTIONS', 'TRANSACTION', 'ALL', 'Detect more than 5 transactions within 10 minutes', '{"type":"velocity","field":"transaction_count","window_minutes":10,"threshold":5}', 'BLOCK', 'CRITICAL', 1),
('NEW_DEVICE_LOGIN', 'AUTHENTICATION', 'ALL', 'Alert on login from unrecognized device', '{"type":"device","condition":"new_device"}', 'ALERT', 'MEDIUM', 3),
('GEO_ANOMALY', 'AUTHENTICATION', 'ALL', 'Login from different geography within short time', '{"type":"geo","condition":"impossible_travel","window_hours":2}', 'BLOCK', 'CRITICAL', 1),
('BRUTE_FORCE_DETECTION', 'AUTHENTICATION', 'ALL', 'More than 3 failed login attempts in 15 minutes', '{"type":"velocity","field":"failed_login","window_minutes":15,"threshold":3}', 'BLOCK', 'HIGH', 1),
('OFF_HOURS_ACTIVITY', 'BEHAVIORAL', 'ALL', 'Unusual activity outside normal banking hours (12AM-5AM)', '{"type":"time","condition":"off_hours","start":0,"end":5}', 'ALERT', 'LOW', 5),
('SESSION_HIJACK_DETECTION', 'SESSION', 'WEB', 'Detect sudden device/IP change mid-session', '{"type":"session","condition":"device_change_mid_session"}', 'BLOCK', 'CRITICAL', 1),
('MASS_DATA_ACCESS', 'DATA_ACCESS', 'ALL', 'Admin accessing more than 50 user records in 5 minutes', '{"type":"velocity","field":"record_access","window_minutes":5,"threshold":50}', 'ALERT', 'HIGH', 2),
('DORMANT_ACCOUNT_ACTIVITY', 'ACCOUNT', 'ALL', 'Transaction on account dormant for more than 90 days', '{"type":"dormancy","field":"last_activity_days","threshold":90}', 'ALERT', 'MEDIUM', 3),
('PHISHING_PATTERN', 'CONTENT', 'ALL', 'Detect phishing keywords in messages/emails', '{"type":"content","patterns":["urgent","verify account","click here","suspended"]}', 'ALERT', 'HIGH', 2);
