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
                date_of_birth DATE NOT NULL, status VARCHAR(20) NOT NULL, monthly_limit DECIMAL(19,2) NOT NULL,
                daily_limit DECIMAL(19,2) NOT NULL, created_at DATETIME(6) NOT NULL, reviewed_at DATETIME(6) NULL,
                reviewed_by VARCHAR(255), rejection_reason VARCHAR(500), INDEX idx_minor_guardian (guardian_user_id), INDEX idx_minor_status (status)
                """);
            create("guardian_links", """
                id BIGINT AUTO_INCREMENT PRIMARY KEY, guardian_user_id BIGINT NOT NULL, child_user_id BIGINT NOT NULL,
                status VARCHAR(20) NOT NULL, created_at DATETIME(6) NOT NULL,
                UNIQUE KEY uk_guardian_child (guardian_user_id, child_user_id)
                """);
        } catch (Exception ex) {
            System.err.println("Family Banking migration warning: " + ex.getMessage());
        }
    }
    private void create(String table, String definition) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?", Integer.class, table);
        if (count == null || count == 0) jdbc.execute("CREATE TABLE " + table + " (" + definition + ")");
    }
}
