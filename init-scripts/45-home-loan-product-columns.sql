-- Add the fields used by car, farmer, and crop loan applications.
-- Safe to re-run: existing columns are omitted from the ALTER statement.
SET SESSION group_concat_max_len = 8192;

SELECT GROUP_CONCAT(
    CONCAT('ADD COLUMN `', columns_to_add.column_name, '` ', columns_to_add.column_definition)
    SEPARATOR ', '
)
INTO @home_loan_missing_columns
FROM (
    SELECT 'loan_type' AS column_name, 'VARCHAR(255)' AS column_definition
    UNION ALL SELECT 'vehicle_make_model', 'VARCHAR(255)'
    UNION ALL SELECT 'vehicle_condition', 'VARCHAR(255)'
    UNION ALL SELECT 'vehicle_year', 'INT'
    UNION ALL SELECT 'vehicle_registration_number', 'VARCHAR(255)'
    UNION ALL SELECT 'vehicle_dealer', 'VARCHAR(255)'
    UNION ALL SELECT 'vehicle_price', 'DOUBLE'
    UNION ALL SELECT 'vehicle_image_paths', 'VARCHAR(4000)'
    UNION ALL SELECT 'vehicle_invoice_path', 'VARCHAR(255)'
    UNION ALL SELECT 'vehicle_invoice_status', 'VARCHAR(255)'
    UNION ALL SELECT 'vehicle_invoice_remark', 'VARCHAR(1000)'
    UNION ALL SELECT 'vehicle_images_status', 'VARCHAR(255)'
    UNION ALL SELECT 'vehicle_images_remark', 'VARCHAR(1000)'
    UNION ALL SELECT 'farm_location', 'VARCHAR(255)'
    UNION ALL SELECT 'crop_name', 'VARCHAR(255)'
    UNION ALL SELECT 'crop_season', 'VARCHAR(255)'
    UNION ALL SELECT 'land_area', 'DOUBLE'
    UNION ALL SELECT 'land_area_unit', 'VARCHAR(255)'
    UNION ALL SELECT 'cultivation_cost_estimate', 'DOUBLE'
    UNION ALL SELECT 'land_record_path', 'VARCHAR(255)'
    UNION ALL SELECT 'land_record_status', 'VARCHAR(255)'
    UNION ALL SELECT 'land_record_remark', 'VARCHAR(1000)'
    UNION ALL SELECT 'agriculture_estimate_path', 'VARCHAR(255)'
    UNION ALL SELECT 'agriculture_estimate_status', 'VARCHAR(255)'
    UNION ALL SELECT 'agriculture_estimate_remark', 'VARCHAR(1000)'
    UNION ALL SELECT 'crop_estimate_path', 'VARCHAR(255)'
    UNION ALL SELECT 'crop_estimate_status', 'VARCHAR(255)'
    UNION ALL SELECT 'crop_estimate_remark', 'VARCHAR(1000)'
) AS columns_to_add
WHERE NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS current_column
    WHERE current_column.TABLE_SCHEMA = DATABASE()
      AND current_column.TABLE_NAME = 'home_loans'
      AND current_column.COLUMN_NAME = columns_to_add.column_name
);

SET @home_loan_ddl = IF(
    @home_loan_missing_columns IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE `home_loans` ', @home_loan_missing_columns)
);

PREPARE home_loan_schema_update FROM @home_loan_ddl;
EXECUTE home_loan_schema_update;
DEALLOCATE PREPARE home_loan_schema_update;

UPDATE home_loans
SET loan_type = CASE
    WHEN UPPER(LEFT(application_id, 2)) = 'CL' THEN 'CAR'
    WHEN UPPER(LEFT(application_id, 2)) = 'FL' THEN 'FARMER'
    WHEN UPPER(LEFT(application_id, 2)) = 'CR' THEN 'CROP'
    ELSE 'HOME'
END
WHERE loan_type IS NULL OR loan_type = '' OR
    (UPPER(loan_type) = 'HOME' AND UPPER(LEFT(application_id, 2)) IN ('CL', 'FL', 'CR'));
