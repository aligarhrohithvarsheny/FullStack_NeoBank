-- Soundbox linked receiving accounts and admin-approved payment settlement.
CREATE TABLE IF NOT EXISTS soundbox_linked_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    soundbox_account_number VARCHAR(30) NOT NULL,
    linked_account_number VARCHAR(30) NOT NULL,
    linked_customer_id VARCHAR(30) NOT NULL,
    linked_account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_by VARCHAR(255),
    admin_remarks VARCHAR(500),
    requested_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME,
    INDEX idx_soundbox_link_status (status),
    INDEX idx_soundbox_link_owner (soundbox_account_number)
);

INSERT INTO soundbox_linked_accounts (
        soundbox_account_number, linked_account_number, linked_customer_id,
        linked_account_name, account_type, status, processed_by, processed_at
)
SELECT d.account_number, d.account_number, a.customer_id,
             COALESCE(a.business_name, a.owner_name, d.account_number), 'CURRENT',
             'APPROVED', 'SYSTEM', CURRENT_TIMESTAMP
FROM soundbox_devices d
JOIN current_accounts a ON a.account_number = d.account_number
LEFT JOIN soundbox_linked_accounts l
        ON l.soundbox_account_number = d.account_number
        AND l.linked_account_number = d.account_number
WHERE a.customer_id IS NOT NULL
    AND l.id IS NULL;

        SET @soundbox_column_exists = (
            SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'soundbox_transactions'
              AND COLUMN_NAME = 'soundbox_account_number'
        );
        SET @soundbox_column_ddl = IF(@soundbox_column_exists = 0,
            'ALTER TABLE soundbox_transactions ADD COLUMN soundbox_account_number VARCHAR(30)', 'SELECT 1');
        PREPARE soundbox_column_stmt FROM @soundbox_column_ddl;
        EXECUTE soundbox_column_stmt;
        DEALLOCATE PREPARE soundbox_column_stmt;

        SET @soundbox_column_exists = (
            SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'soundbox_transactions'
              AND COLUMN_NAME = 'account_type'
        );
        SET @soundbox_column_ddl = IF(@soundbox_column_exists = 0,
            'ALTER TABLE soundbox_transactions ADD COLUMN account_type VARCHAR(20)', 'SELECT 1');
        PREPARE soundbox_column_stmt FROM @soundbox_column_ddl;
        EXECUTE soundbox_column_stmt;
        DEALLOCATE PREPARE soundbox_column_stmt;

        SET @soundbox_column_exists = (
            SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'soundbox_transactions'
              AND COLUMN_NAME = 'approved_by'
        );
        SET @soundbox_column_ddl = IF(@soundbox_column_exists = 0,
            'ALTER TABLE soundbox_transactions ADD COLUMN approved_by VARCHAR(255)', 'SELECT 1');
        PREPARE soundbox_column_stmt FROM @soundbox_column_ddl;
        EXECUTE soundbox_column_stmt;
        DEALLOCATE PREPARE soundbox_column_stmt;

        SET @soundbox_column_exists = (
            SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'soundbox_transactions'
              AND COLUMN_NAME = 'approved_at'
        );
        SET @soundbox_column_ddl = IF(@soundbox_column_exists = 0,
            'ALTER TABLE soundbox_transactions ADD COLUMN approved_at DATETIME', 'SELECT 1');
        PREPARE soundbox_column_stmt FROM @soundbox_column_ddl;
        EXECUTE soundbox_column_stmt;
        DEALLOCATE PREPARE soundbox_column_stmt;

SET @soundbox_account_fk = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'soundbox_transactions'
      AND COLUMN_NAME = 'account_number'
      AND REFERENCED_TABLE_NAME = 'current_accounts'
    LIMIT 1
);
SET @soundbox_drop_fk = IF(
    @soundbox_account_fk IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE soundbox_transactions DROP FOREIGN KEY `', @soundbox_account_fk, '`')
);
PREPARE soundbox_fk_stmt FROM @soundbox_drop_fk;
EXECUTE soundbox_fk_stmt;
DEALLOCATE PREPARE soundbox_fk_stmt;