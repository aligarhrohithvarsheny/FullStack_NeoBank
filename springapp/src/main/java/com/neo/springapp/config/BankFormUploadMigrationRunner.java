package com.neo.springapp.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures bank form upload tables support database-backed file storage on hosts
 * with ephemeral disk (e.g. Render). Safe to run on every startup.
 */
@Component
@Order(20)
public class BankFormUploadMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public BankFormUploadMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureUploadFileContentColumn();
            ensureUploadHistoryTable();
        } catch (Exception e) {
            System.err.println("Bank form upload migration warning: " + e.getMessage());
        }
    }

    private void ensureUploadFileContentColumn() {
        if (!tableExists("bank_form_uploads")) {
            return;
        }
        if (columnExists("bank_form_uploads", "file_content")) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE bank_form_uploads ADD COLUMN file_content LONGBLOB NULL");
        System.out.println("Added bank_form_uploads.file_content column for persistent upload storage");
    }

    private void ensureUploadHistoryTable() {
        if (tableExists("bank_form_upload_history")) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE bank_form_upload_history (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    upload_id BIGINT NOT NULL,
                    action VARCHAR(32) NOT NULL,
                    previous_file_name VARCHAR(255),
                    previous_stored_path VARCHAR(512),
                    previous_content_type VARCHAR(128),
                    previous_file_size_bytes BIGINT,
                    new_file_name VARCHAR(255),
                    new_stored_path VARCHAR(512),
                    new_content_type VARCHAR(128),
                    new_file_size_bytes BIGINT,
                    performed_by_admin VARCHAR(128),
                    remarks VARCHAR(1000),
                    performed_at DATETIME(6) NOT NULL,
                    INDEX idx_bfuh_upload (upload_id)
                )
                """);
        System.out.println("Created bank_form_upload_history table");
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """,
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }
}
