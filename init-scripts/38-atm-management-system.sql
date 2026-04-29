-- ============================================================
-- 38: ATM Management System
-- Admin-managed ATMs with cash loading, service control,
-- transaction tracking, receipts, and stuck-card management
-- ============================================================

-- ATM Machines table (admin creates and manages ATMs)
CREATE TABLE IF NOT EXISTS atm_machines (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    atm_id              VARCHAR(20) UNIQUE NOT NULL,
    atm_name            VARCHAR(100) NOT NULL,
    location            VARCHAR(255) NOT NULL,
    city                VARCHAR(100),
    state               VARCHAR(100),
    pincode             VARCHAR(10),
    status              VARCHAR(30) DEFAULT 'ACTIVE',
    cash_available      DECIMAL(15,2) DEFAULT 0.00,
    max_capacity        DECIMAL(15,2) DEFAULT 5000000.00,
    min_threshold       DECIMAL(15,2) DEFAULT 50000.00,
    max_withdrawal_limit DECIMAL(15,2) DEFAULT 25000.00,
    daily_limit         DECIMAL(15,2) DEFAULT 100000.00,
    notes_500_count     INT DEFAULT 0,
    notes_200_count     INT DEFAULT 0,
    notes_100_count     INT DEFAULT 0,
    notes_2000_count    INT DEFAULT 0,
    last_cash_loaded    DATETIME,
    last_serviced       DATETIME,
    installed_date      DATETIME DEFAULT CURRENT_TIMESTAMP,
    managed_by          VARCHAR(100),
    contact_number      VARCHAR(15),
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_atm_id (atm_id),
    INDEX idx_atm_status (status),
    INDEX idx_atm_city (city),
    INDEX idx_atm_cash (cash_available)
);

-- ATM Cash Loading History (admin deposits cash into ATM)
CREATE TABLE IF NOT EXISTS atm_cash_loads (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    atm_id          VARCHAR(20) NOT NULL,
    loaded_by       VARCHAR(100) NOT NULL,
    amount          DECIMAL(15,2) NOT NULL,
    notes_500       INT DEFAULT 0,
    notes_200       INT DEFAULT 0,
    notes_100       INT DEFAULT 0,
    notes_2000      INT DEFAULT 0,
    previous_balance DECIMAL(15,2),
    new_balance     DECIMAL(15,2),
    remarks         VARCHAR(500),
    loaded_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cash_load_atm (atm_id),
    INDEX idx_cash_load_by (loaded_by),
    INDEX idx_cash_load_date (loaded_at)
);

-- ATM Transactions (every user withdrawal at ATM)
CREATE TABLE IF NOT EXISTS atm_transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_ref     VARCHAR(50) UNIQUE NOT NULL,
    atm_id              VARCHAR(20) NOT NULL,
    account_number      VARCHAR(50) NOT NULL,
    card_number         VARCHAR(30),
    user_name           VARCHAR(150),
    user_email          VARCHAR(150),
    transaction_type    VARCHAR(30) NOT NULL,
    amount              DECIMAL(15,2) NOT NULL,
    balance_before      DECIMAL(15,2),
    balance_after       DECIMAL(15,2),
    atm_balance_before  DECIMAL(15,2),
    atm_balance_after   DECIMAL(15,2),
    status              VARCHAR(30) DEFAULT 'SUCCESS',
    failure_reason      VARCHAR(500),
    receipt_number      VARCHAR(50),
    receipt_generated   BOOLEAN DEFAULT TRUE,
    notes_dispensed_500  INT DEFAULT 0,
    notes_dispensed_200  INT DEFAULT 0,
    notes_dispensed_100  INT DEFAULT 0,
    notes_dispensed_2000 INT DEFAULT 0,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_atm_txn_ref (transaction_ref),
    INDEX idx_atm_txn_atm (atm_id),
    INDEX idx_atm_txn_account (account_number),
    INDEX idx_atm_txn_status (status),
    INDEX idx_atm_txn_type (transaction_type),
    INDEX idx_atm_txn_date (created_at),
    INDEX idx_atm_txn_card (card_number)
);

-- ATM Stuck Card / Incident Log
CREATE TABLE IF NOT EXISTS atm_incidents (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_ref    VARCHAR(50) UNIQUE NOT NULL,
    atm_id          VARCHAR(20) NOT NULL,
    incident_type   VARCHAR(50) NOT NULL,
    account_number  VARCHAR(50),
    card_number     VARCHAR(30),
    user_name       VARCHAR(150),
    description     VARCHAR(1000),
    amount_involved DECIMAL(15,2) DEFAULT 0.00,
    status          VARCHAR(30) DEFAULT 'OPEN',
    resolution      VARCHAR(1000),
    resolved_by     VARCHAR(100),
    resolved_at     DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_incident_atm (atm_id),
    INDEX idx_incident_type (incident_type),
    INDEX idx_incident_status (status),
    INDEX idx_incident_account (account_number),
    INDEX idx_incident_date (created_at)
);

-- ATM Service Log (maintenance, out of service, etc.)
CREATE TABLE IF NOT EXISTS atm_service_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    atm_id          VARCHAR(20) NOT NULL,
    action          VARCHAR(50) NOT NULL,
    previous_status VARCHAR(30),
    new_status      VARCHAR(30),
    performed_by    VARCHAR(100) NOT NULL,
    reason          VARCHAR(500),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_service_atm (atm_id),
    INDEX idx_service_action (action),
    INDEX idx_service_date (created_at)
);

-- Insert default ATM machines
INSERT IGNORE INTO atm_machines (atm_id, atm_name, location, city, state, pincode, status, cash_available, max_capacity, managed_by)
VALUES
    ('ATM001', 'NeoBank Main Branch ATM', 'MG Road, Sector 14', 'Hyderabad', 'Telangana', '500001', 'ACTIVE', 1000000.00, 5000000.00, 'admin@neobank.com'),
    ('ATM002', 'NeoBank City Center ATM', 'Banjara Hills, Road No 12', 'Hyderabad', 'Telangana', '500034', 'ACTIVE', 500000.00, 5000000.00, 'admin@neobank.com'),
    ('ATM003', 'NeoBank Tech Park ATM', 'Gachibowli IT Park', 'Hyderabad', 'Telangana', '500032', 'ACTIVE', 750000.00, 5000000.00, 'admin@neobank.com'),
    ('ATM004', 'NeoBank Airport ATM', 'RGIA Airport Terminal 1', 'Hyderabad', 'Telangana', '500108', 'MAINTENANCE', 200000.00, 5000000.00, 'admin@neobank.com'),
    ('ATM005', 'NeoBank University ATM', 'JNTU Campus Gate', 'Hyderabad', 'Telangana', '500085', 'OUT_OF_SERVICE', 0.00, 5000000.00, 'admin@neobank.com');
