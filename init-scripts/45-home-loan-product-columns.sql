-- Add the fields used by car, farmer, and crop loan applications.
-- Safe to re-run: existing columns are omitted from the ALTER statement.
SET SESSION group_concat_max_len = 8192;

-- Legacy free-text and file-path fields can otherwise leave too little inline
-- row space for the new columns on older MySQL tables.
SELECT GROUP_CONCAT(
    CONCAT('MODIFY COLUMN `', text_columns.column_name, '` TEXT')
    SEPARATOR ', '
)
INTO @home_loan_legacy_text_columns
FROM (
    SELECT 'purpose' AS column_name
    UNION ALL SELECT 'property_address'
    UNION ALL SELECT 'rejection_reason'
    UNION ALL SELECT 'fd_receipt_path'
    UNION ALL SELECT 'model_doc_path'
    UNION ALL SELECT 'signature_path'
    UNION ALL SELECT 'fd_receipt_remark'
    UNION ALL SELECT 'model_doc_remark'
    UNION ALL SELECT 'signature_remark'
    UNION ALL SELECT 'admin_notes'
) AS text_columns
JOIN information_schema.COLUMNS existing_column
    ON existing_column.TABLE_SCHEMA = DATABASE()
    AND existing_column.TABLE_NAME = 'home_loans'
    AND existing_column.COLUMN_NAME = text_columns.column_name
WHERE existing_column.DATA_TYPE NOT IN ('text', 'mediumtext', 'longtext');

SET @home_loan_legacy_text_ddl = IF(
    @home_loan_legacy_text_columns IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE `home_loans` ', @home_loan_legacy_text_columns)
);

PREPARE home_loan_legacy_text_update FROM @home_loan_legacy_text_ddl;
EXECUTE home_loan_legacy_text_update;
DEALLOCATE PREPARE home_loan_legacy_text_update;

SELECT GROUP_CONCAT(
    CONCAT('ADD COLUMN `', columns_to_add.column_name, '` ', columns_to_add.column_definition)
    SEPARATOR ', '
)
INTO @home_loan_missing_columns
FROM (
    SELECT 'loan_type' AS column_name, 'VARCHAR(32)' AS column_definition
    UNION ALL SELECT 'vehicle_make_model', 'TEXT'
    UNION ALL SELECT 'vehicle_condition', 'VARCHAR(32)'
    UNION ALL SELECT 'vehicle_year', 'INT'
    UNION ALL SELECT 'vehicle_registration_number', 'VARCHAR(64)'
    UNION ALL SELECT 'vehicle_dealer', 'TEXT'
    UNION ALL SELECT 'vehicle_price', 'DOUBLE'
    UNION ALL SELECT 'vehicle_image_paths', 'TEXT'
    UNION ALL SELECT 'vehicle_invoice_path', 'TEXT'
    UNION ALL SELECT 'vehicle_invoice_status', 'VARCHAR(32)'
    UNION ALL SELECT 'vehicle_invoice_remark', 'TEXT'
    UNION ALL SELECT 'vehicle_images_status', 'VARCHAR(32)'
    UNION ALL SELECT 'vehicle_images_remark', 'TEXT'
    UNION ALL SELECT 'farm_location', 'TEXT'
    UNION ALL SELECT 'crop_name', 'TEXT'
    UNION ALL SELECT 'crop_season', 'VARCHAR(64)'
    UNION ALL SELECT 'land_area', 'DOUBLE'
    UNION ALL SELECT 'land_area_unit', 'VARCHAR(32)'
    UNION ALL SELECT 'cultivation_cost_estimate', 'DOUBLE'
    UNION ALL SELECT 'land_record_path', 'TEXT'
    UNION ALL SELECT 'land_record_status', 'VARCHAR(32)'
    UNION ALL SELECT 'land_record_remark', 'TEXT'
    UNION ALL SELECT 'agriculture_estimate_path', 'TEXT'
    UNION ALL SELECT 'agriculture_estimate_status', 'VARCHAR(32)'
    UNION ALL SELECT 'agriculture_estimate_remark', 'TEXT'
    UNION ALL SELECT 'crop_estimate_path', 'TEXT'
    UNION ALL SELECT 'crop_estimate_status', 'VARCHAR(32)'
    UNION ALL SELECT 'crop_estimate_remark', 'TEXT'
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

SELECT GROUP_CONCAT(
    CONCAT('MODIFY COLUMN `', text_columns.column_name, '` TEXT')
    SEPARATOR ', '
)
INTO @home_loan_text_columns
FROM (
    SELECT 'vehicle_make_model' AS column_name
    UNION ALL SELECT 'vehicle_dealer'
    UNION ALL SELECT 'vehicle_image_paths'
    UNION ALL SELECT 'vehicle_invoice_path'
    UNION ALL SELECT 'vehicle_invoice_remark'
    UNION ALL SELECT 'vehicle_images_remark'
    UNION ALL SELECT 'farm_location'
    UNION ALL SELECT 'crop_name'
    UNION ALL SELECT 'land_record_path'
    UNION ALL SELECT 'land_record_remark'
    UNION ALL SELECT 'agriculture_estimate_path'
    UNION ALL SELECT 'agriculture_estimate_remark'
    UNION ALL SELECT 'crop_estimate_path'
    UNION ALL SELECT 'crop_estimate_remark'
) AS text_columns
JOIN information_schema.COLUMNS existing_column
    ON existing_column.TABLE_SCHEMA = DATABASE()
    AND existing_column.TABLE_NAME = 'home_loans'
    AND existing_column.COLUMN_NAME = text_columns.column_name
WHERE existing_column.DATA_TYPE NOT IN ('text', 'mediumtext', 'longtext');

SET @home_loan_text_ddl = IF(
    @home_loan_text_columns IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE `home_loans` ', @home_loan_text_columns)
);

PREPARE home_loan_text_update FROM @home_loan_text_ddl;
EXECUTE home_loan_text_update;
DEALLOCATE PREPARE home_loan_text_update;

UPDATE home_loans
SET loan_type = CASE
    WHEN UPPER(LEFT(application_id, 2)) = 'CL' THEN 'CAR'
    WHEN UPPER(LEFT(application_id, 2)) = 'FL' THEN 'FARMER'
    WHEN UPPER(LEFT(application_id, 2)) = 'CR' THEN 'CROP'
    ELSE 'HOME'
END
WHERE loan_type IS NULL OR loan_type = '' OR
    (UPPER(loan_type) = 'HOME' AND UPPER(LEFT(application_id, 2)) IN ('CL', 'FL', 'CR'));
