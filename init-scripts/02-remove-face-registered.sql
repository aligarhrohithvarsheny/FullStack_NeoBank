-- Remove face_registered column from users table
-- This script updates the database to handle the removal of face registration

USE springapp;

-- Option 1: Add a default value to the column (if you want to keep the column)
ALTER TABLE users MODIFY COLUMN face_registered BOOLEAN DEFAULT FALSE;

-- Option 2: Make the column nullable (if you want to keep the column but allow NULL)
-- ALTER TABLE users MODIFY COLUMN face_registered BOOLEAN NULL DEFAULT FALSE;

-- Option 3: Drop the column completely (if you want to remove it entirely)
-- ALTER TABLE users DROP COLUMN face_registered;




