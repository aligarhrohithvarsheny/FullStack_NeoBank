-- Payment Links Table for EzyVault Pay
CREATE TABLE IF NOT EXISTS pg_payment_links (
    id BIGSERIAL PRIMARY KEY,
    link_id VARCHAR(50) UNIQUE NOT NULL,
    link_token VARCHAR(100) UNIQUE NOT NULL,
    merchant_id VARCHAR(50) NOT NULL,
    merchant_name VARCHAR(200),
    merchant_upi_id VARCHAR(100),
    recipient_upi_id VARCHAR(100) NOT NULL,
    recipient_name VARCHAR(200),
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    paid_at TIMESTAMP,
    payer_account_number VARCHAR(50),
    payer_name VARCHAR(200),
    txn_ref VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_pg_payment_links_recipient_upi ON pg_payment_links(recipient_upi_id);
CREATE INDEX IF NOT EXISTS idx_pg_payment_links_merchant ON pg_payment_links(merchant_id);
CREATE INDEX IF NOT EXISTS idx_pg_payment_links_status ON pg_payment_links(status);
CREATE INDEX IF NOT EXISTS idx_pg_payment_links_token ON pg_payment_links(link_token);
