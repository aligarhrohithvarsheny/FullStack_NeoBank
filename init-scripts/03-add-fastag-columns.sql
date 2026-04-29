-- Add new columns for FASTag sticker generation
ALTER TABLE fasttags
  ADD COLUMN barcode_number VARCHAR(32) DEFAULT NULL,
  ADD COLUMN issue_date DATETIME DEFAULT NULL,
  ADD COLUMN sticker_path VARCHAR(512) DEFAULT NULL;

-- If you're using MySQL and want to check/update existing rows, you can run:
-- UPDATE fasttags SET barcode_number = NULL WHERE barcode_number = '';
