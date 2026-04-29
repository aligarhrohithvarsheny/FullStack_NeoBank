-- Linked Accounts Table
-- Links savings accounts to current accounts for admin-managed account switching

USE springapp;

CREATE TABLE IF NOT EXISTS linked_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    current_account_number VARCHAR(20) NOT NULL,
    savings_account_number VARCHAR(20) NOT NULL,
    savings_customer_id VARCHAR(20) NOT NULL,
    linked_by VARCHAR(255) NOT NULL,
    linked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    switch_pin VARCHAR(255) DEFAULT NULL,
    pin_created BOOLEAN DEFAULT FALSE,
    UNIQUE KEY uk_current_account (current_account_number),
    UNIQUE KEY uk_savings_account (savings_account_number),
    FOREIGN KEY (current_account_number) REFERENCES current_accounts(account_number) ON DELETE CASCADE,
    FOREIGN KEY (savings_account_number) REFERENCES accounts(account_number) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
