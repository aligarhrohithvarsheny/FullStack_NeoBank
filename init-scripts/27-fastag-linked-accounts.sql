-- FASTag Linked Bank Accounts
-- Links FASTag users (by gmail) to their NeoBank savings accounts for recharge debits

CREATE TABLE IF NOT EXISTS fastag_linked_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gmail_id VARCHAR(255) NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    account_holder_name VARCHAR(255),
    verified BOOLEAN DEFAULT FALSE,
    linked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    UNIQUE KEY uk_gmail_account (gmail_id, account_number)
);
