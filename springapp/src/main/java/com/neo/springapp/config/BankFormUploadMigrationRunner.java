package com.neo.springapp.config;

import com.neo.springapp.model.BankFormUpload;
import com.neo.springapp.repository.BankFormUploadRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Ensures bank form upload tables support database-backed file storage on hosts
 * with ephemeral disk (e.g. Render). Safe to run on every startup.
 */
@Component
@Order(20)
public class BankFormUploadMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final BankFormUploadRepository bankFormUploadRepository;

    public BankFormUploadMigrationRunner(
            JdbcTemplate jdbcTemplate,
            BankFormUploadRepository bankFormUploadRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.bankFormUploadRepository = bankFormUploadRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureUploadFileContentColumn();
            ensureUploadHistoryTable();
            ensureAdminAuditFileBase64Column();
            backfillFileContentFromDisk();
        } catch (Exception e) {
            System.err.println("Bank form upload migration warning: " + e.getMessage());
        }
    }

    private void ensureUploadFileContentColumn() {
        if (!tableExists("bank_form_uploads")) {
            return;
        }
        if (!columnExists("bank_form_uploads", "file_content")) {
            jdbcTemplate.execute("ALTER TABLE bank_form_uploads ADD COLUMN file_content LONGBLOB NULL");
            System.out.println("Added bank_form_uploads.file_content column for persistent upload storage");
            return;
        }
        String dataType = getColumnDataType("bank_form_uploads", "file_content");
        if (dataType != null && !"longblob".equalsIgnoreCase(dataType)) {
            jdbcTemplate.execute("ALTER TABLE bank_form_uploads MODIFY COLUMN file_content LONGBLOB NULL");
            System.out.println("Upgraded bank_form_uploads.file_content from " + dataType + " to LONGBLOB");
        }
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

    private void ensureAdminAuditFileBase64Column() {
        if (!tableExists("admin_audit_documents") || !columnExists("admin_audit_documents", "file_base64")) {
            return;
        }
        String dataType = getColumnDataType("admin_audit_documents", "file_base64");
        if (dataType != null && !"longtext".equalsIgnoreCase(dataType)) {
            jdbcTemplate.execute("ALTER TABLE admin_audit_documents MODIFY COLUMN file_base64 LONGTEXT NULL");
            System.out.println("Upgraded admin_audit_documents.file_base64 from " + dataType + " to LONGTEXT");
        }
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

    private String getColumnDataType(String tableName, String columnName) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT DATA_TYPE
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = ?
                      AND column_name = ?
                    """,
                    String.class,
                    tableName,
                    columnName);
        } catch (Exception e) {
            return null;
        }
    }

    /** Copy disk files into DB for uploads created before database-backed storage. */
    private void backfillFileContentFromDisk() {
        if (!tableExists("bank_form_uploads") || !columnExists("bank_form_uploads", "file_content")) {
            return;
        }
        List<BankFormUpload> uploads = bankFormUploadRepository.findAll();
        int backfilled = 0;
        for (BankFormUpload upload : uploads) {
            if (upload.getFileContent() != null && upload.getFileContent().length > 0) {
                continue;
            }
            String storedPath = upload.getStoredFilePath();
            if (storedPath == null || storedPath.isBlank() || storedPath.startsWith("db://")) {
                continue;
            }
            try {
                Path path = Paths.get(storedPath).normalize();
                if (!path.isAbsolute()) {
                    path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
                }
                if (!Files.exists(path)) {
                    continue;
                }
                upload.setFileContent(Files.readAllBytes(path));
                bankFormUploadRepository.save(upload);
                backfilled++;
            } catch (Exception e) {
                System.err.println("Bank form backfill skipped id=" + upload.getId() + ": " + e.getMessage());
            }
        }
        if (backfilled > 0) {
            System.out.println("Backfilled file_content for " + backfilled + " bank form upload(s)");
        }
    }
}
