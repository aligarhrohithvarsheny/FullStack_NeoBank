package com.neo.springapp.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(0)
public class HomeLoanProductSchemaMigrationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(HomeLoanProductSchemaMigrationRunner.class);
    private static final Map<String, String> COLUMNS = new LinkedHashMap<>();

    static {
        COLUMNS.put("loan_type", "VARCHAR(255)");
        COLUMNS.put("vehicle_make_model", "VARCHAR(255)");
        COLUMNS.put("vehicle_condition", "VARCHAR(255)");
        COLUMNS.put("vehicle_year", "INT");
        COLUMNS.put("vehicle_registration_number", "VARCHAR(255)");
        COLUMNS.put("vehicle_dealer", "VARCHAR(255)");
        COLUMNS.put("vehicle_price", "DOUBLE");
        COLUMNS.put("vehicle_image_paths", "VARCHAR(4000)");
        COLUMNS.put("vehicle_invoice_path", "VARCHAR(255)");
        COLUMNS.put("vehicle_invoice_status", "VARCHAR(255)");
        COLUMNS.put("vehicle_invoice_remark", "VARCHAR(1000)");
        COLUMNS.put("vehicle_images_status", "VARCHAR(255)");
        COLUMNS.put("vehicle_images_remark", "VARCHAR(1000)");
        COLUMNS.put("farm_location", "VARCHAR(255)");
        COLUMNS.put("crop_name", "VARCHAR(255)");
        COLUMNS.put("crop_season", "VARCHAR(255)");
        COLUMNS.put("land_area", "DOUBLE");
        COLUMNS.put("land_area_unit", "VARCHAR(255)");
        COLUMNS.put("cultivation_cost_estimate", "DOUBLE");
        COLUMNS.put("land_record_path", "VARCHAR(255)");
        COLUMNS.put("land_record_status", "VARCHAR(255)");
        COLUMNS.put("land_record_remark", "VARCHAR(1000)");
        COLUMNS.put("agriculture_estimate_path", "VARCHAR(255)");
        COLUMNS.put("agriculture_estimate_status", "VARCHAR(255)");
        COLUMNS.put("agriculture_estimate_remark", "VARCHAR(1000)");
        COLUMNS.put("crop_estimate_path", "VARCHAR(255)");
        COLUMNS.put("crop_estimate_status", "VARCHAR(255)");
        COLUMNS.put("crop_estimate_remark", "VARCHAR(1000)");
    }

    private final JdbcTemplate jdbcTemplate;

    public HomeLoanProductSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer tableExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = UPPER(?)",
                Integer.class, "home_loans");
        if (tableExists == null || tableExists == 0) {
            log.warn("Skipping home-loan product schema migration because the home_loans table does not exist");
            return;
        }

        for (Map.Entry<String, String> column : COLUMNS.entrySet()) {
            ensureColumn(column.getKey(), column.getValue());
        }

        int repairedLoanTypes = jdbcTemplate.update("""
                UPDATE home_loans
                SET loan_type = CASE
                    WHEN UPPER(LEFT(application_id, 2)) = 'CL' THEN 'CAR'
                    WHEN UPPER(LEFT(application_id, 2)) = 'FL' THEN 'FARMER'
                    WHEN UPPER(LEFT(application_id, 2)) = 'CR' THEN 'CROP'
                    ELSE 'HOME'
                END
                WHERE loan_type IS NULL OR loan_type = '' OR
                    (UPPER(loan_type) = 'HOME' AND UPPER(LEFT(application_id, 2)) IN ('CL', 'FL', 'CR'))
                """);
        log.info("Home-loan product schema is ready; repaired {} legacy loan type value(s)", repairedLoanTypes);
    }

    private void ensureColumn(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)",
                Integer.class, "home_loans", columnName);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE home_loans ADD COLUMN " + columnName + " " + definition);
        }
    }
}
