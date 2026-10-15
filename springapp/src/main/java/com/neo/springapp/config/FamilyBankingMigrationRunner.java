package com.neo.springapp.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class FamilyBankingMigrationRunner implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public FamilyBankingMigrationRunner(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        try {
            create("joint_account_invitations", """
                id BIGINT AUTO_INCREMENT PRIMARY KEY, account_number VARCHAR(32) NOT NULL,
                inviter_user_id BIGINT NOT NULL, invitee_user_id BIGINT NOT NULL, relationship VARCHAR(40), operating_mode VARCHAR(30), joint_account_number VARCHAR(32), status VARCHAR(20) NOT NULL,
                created_at DATETIME(6) NOT NULL, responded_at DATETIME(6) NULL,
                INDEX idx_joint_invitee (invitee_user_id), INDEX idx_joint_account (account_number)
                """);
            create("joint_transfer_approvals", """
                id BIGINT AUTO_INCREMENT PRIMARY KEY, account_number VARCHAR(32) NOT NULL,
                from_user_id BIGINT NOT NULL, approver_user_id BIGINT NOT NULL, to_account_number VARCHAR(32) NOT NULL,
                amount DECIMAL(19,2) NOT NULL, note VARCHAR(500), status VARCHAR(20) NOT NULL,
                created_at DATETIME(6) NOT NULL, decided_at DATETIME(6) NULL, transaction_reference VARCHAR(64),
                INDEX idx_joint_transfer_account (account_number), INDEX idx_joint_transfer_status (status)
                """);
            create("joint_account_profiles", """
                id BIGINT AUTO_INCREMENT PRIMARY KEY, joint_account_number VARCHAR(32) NOT NULL UNIQUE,
                primary_holder_user_id BIGINT NOT NULL, joint_holder_user_id BIGINT NOT NULL,
                operating_mode VARCHAR(30) NOT NULL, status VARCHAR(20) NOT NULL, created_at DATETIME(6) NOT NULL
                """);
            create("minor_account_applications", """
                id BIGINT AUTO_INCREMENT PRIMARY KEY, guardian_user_id BIGINT NOT NULL, minor_name VARCHAR(100) NOT NULL,
                date_of_birth DATE NOT NULL, form60 VARCHAR(2000), status VARCHAR(20) NOT NULL, monthly_limit DECIMAL(19,2) NOT NULL,
                daily_limit DECIMAL(19,2) NOT NULL, created_at DATETIME(6) NOT NULL, reviewed_at DATETIME(6) NULL,
                reviewed_by VARCHAR(255), rejection_reason VARCHAR(500), assigned_account_number VARCHAR(255),
                assigned_customer_id VARCHAR(255), account_status VARCHAR(20),
                INDEX idx_minor_guardian (guardian_user_id), INDEX idx_minor_status (status)
                """);
            ensureColumn("minor_account_applications", "form60", "VARCHAR(2000) NULL");
            ensureColumn("minor_account_applications", "assigned_account_number", "VARCHAR(255) NULL");
            ensureColumn("minor_account_applications", "assigned_customer_id", "VARCHAR(255) NULL");
            ensureColumn("minor_account_applications", "account_status", "VARCHAR(20) NULL");
            makeAccountPanOptional();
            expandAccountForm60();
            create("guardian_links", """
                id BIGINT AUTO_INCREMENT PRIMARY KEY, guardian_user_id BIGINT NOT NULL, child_user_id BIGINT NOT NULL,
                status VARCHAR(20) NOT NULL, created_at DATETIME(6) NOT NULL,
                UNIQUE KEY uk_guardian_child (guardian_user_id, child_user_id)
                """);
            create("family_banking_notifications", """
                id BIGINT AUTO_INCREMENT PRIMARY KEY, recipient_user_id BIGINT NOT NULL,
                type VARCHAR(40) NOT NULL, title VARCHAR(120) NOT NULL, message VARCHAR(1000) NOT NULL,
                created_at DATETIME(6) NOT NULL, read_at DATETIME(6) NULL,
                INDEX idx_family_notification_recipient (recipient_user_id, read_at)
                """);
            create("family_banking_audit_logs", """
                id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_user_id BIGINT NOT NULL,
                action VARCHAR(80) NOT NULL, resource_type VARCHAR(80), resource_id VARCHAR(80),
                details VARCHAR(1000), created_at DATETIME(6) NOT NULL,
                INDEX idx_family_audit_actor (actor_user_id, created_at)
                """);
        } catch (Exception ex) {
            System.err.println("Family Banking migration warning: " + ex.getMessage());
        }
    }
    private void create(String table, String definition) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?", Integer.class, table);
        if (count == null || count == 0) jdbc.execute("CREATE TABLE " + table + " (" + definition + ")");
    }

    private void ensureColumn(String table, String column, String definition) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, table, column);
        if (count == null || count == 0) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void makeAccountPanOptional() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'accounts' AND column_name = 'pan'
                """, Integer.class);
        if (count == null || count == 0) return;
        String nullable = jdbc.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'accounts' AND column_name = 'pan'
                """, String.class);
        if ("NO".equalsIgnoreCase(nullable)) {
            jdbc.execute("ALTER TABLE accounts MODIFY COLUMN pan VARCHAR(255) NULL");
        }
    }

    private void expandAccountForm60() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'accounts' AND column_name = 'form60'
                """, Integer.class);
        if (count == null || count == 0) return;
        Integer maxLength = jdbc.queryForObject("""
                SELECT character_maximum_length FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'accounts' AND column_name = 'form60'
                """, Integer.class);
        if (maxLength != null && maxLength < 2000) {
            jdbc.execute("ALTER TABLE accounts MODIFY COLUMN form60 VARCHAR(2000) NULL");
        }
    }
}
