package com.neo.springapp.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(25)
public class PgMerchantChangeLogMigrationRunner implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public PgMerchantChangeLogMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS pg_merchant_change_logs (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        merchant_id VARCHAR(255),
                        merchant_name VARCHAR(255),
                        changed_by VARCHAR(255),
                        changed_at DATETIME(6),
                        changed_fields TEXT,
                        previous_details TEXT,
                        updated_details TEXT,
                        INDEX idx_pg_change_log_merchant (merchant_id),
                        INDEX idx_pg_change_log_changed_at (changed_at)
                    )
                    """);
            System.out.println("Verified pg_merchant_change_logs table");
        } catch (Exception e) {
            System.err.println("Payment gateway change-log migration warning: " + e.getMessage());
        }
    }
}
