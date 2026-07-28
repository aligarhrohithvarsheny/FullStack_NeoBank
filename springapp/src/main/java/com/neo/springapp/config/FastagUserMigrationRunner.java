package com.neo.springapp.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds password columns to fastag_users for email + password login (no OTP).
 */
@Component
@Order(21)
public class FastagUserMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public FastagUserMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!tableExists("fastag_users")) {
                return;
            }
            addColumnIfMissing("fastag_users", "password", "VARCHAR(512) NULL");
            addColumnIfMissing("fastag_users", "password_set", "BIT(1) DEFAULT 0");
            addColumnIfMissing("fastag_users", "failed_login_attempts", "INT DEFAULT 0");
            addColumnIfMissing("fastag_users", "account_locked", "BIT(1) DEFAULT 0");
        } catch (Exception e) {
            System.err.println("Fastag user migration warning: " + e.getMessage());
        }
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        if (!columnExists(table, column)) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            System.out.println("Added " + table + "." + column);
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
}
