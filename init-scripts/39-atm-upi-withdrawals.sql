-- UPI-authorized, cardless ATM withdrawal requests
CREATE TABLE IF NOT EXISTS atm_upi_withdrawal_requests (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_token       VARCHAR(36) NOT NULL UNIQUE,
    atm_id              VARCHAR(20) NOT NULL,
    account_number      VARCHAR(50) NOT NULL,
    account_type        VARCHAR(30) NOT NULL,
    amount              DECIMAL(15,2) NOT NULL,
    user_name           VARCHAR(150),
    user_email          VARCHAR(150),
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    failure_reason      VARCHAR(500),
    atm_transaction_ref VARCHAR(50),
    upi_transaction_ref VARCHAR(50),
    pin_attempts        INT NOT NULL DEFAULT 0,
    expires_at          DATETIME NOT NULL,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_atm_upi_request_account (account_number),
    INDEX idx_atm_upi_request_status_expiry (status, expires_at)
);

ALTER TABLE atm_transactions
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30) DEFAULT 'CARD',
    ADD COLUMN IF NOT EXISTS upi_transaction_ref VARCHAR(50);
