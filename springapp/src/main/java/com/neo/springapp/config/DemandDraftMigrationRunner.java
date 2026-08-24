package com.neo.springapp.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(26)
public class DemandDraftMigrationRunner implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    public DemandDraftMigrationRunner(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    @Override public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS savings_demand_drafts (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, account_number VARCHAR(100), user_name VARCHAR(255), user_email VARCHAR(255),
                  cheque_number VARCHAR(100), payee_name VARCHAR(255), payee_account_number VARCHAR(100), amount DECIMAL(15,2),
                  draft_date DATE, reason VARCHAR(1000), available_balance DECIMAL(15,2), locked_amount DECIMAL(15,2),
                  status VARCHAR(30), dd_number VARCHAR(100), approved_by VARCHAR(255), approved_at DATETIME(6), created_at DATETIME(6),
                  updated_at DATETIME(6), edit_history TEXT, INDEX idx_dd_account(account_number), INDEX idx_dd_status(status)
                )
                """);
        } catch (Exception e) { System.err.println("Demand draft migration warning: " + e.getMessage()); }
    }
}
