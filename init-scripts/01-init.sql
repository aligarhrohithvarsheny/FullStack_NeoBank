-- NeoBank Database Initialization Script
-- This script runs when the MySQL container starts for the first time

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS springapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the database
USE springapp;

-- Create user if it doesn't exist and grant privileges
CREATE USER IF NOT EXISTS 'neo'@'%' IDENTIFIED BY 'neo123';
GRANT ALL PRIVILEGES ON springapp.* TO 'neo'@'%';
FLUSH PRIVILEGES;

-- Optional: Create some initial data
-- INSERT INTO users (username, email, password) VALUES 
-- ('admin', 'admin@neobank.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi');

-- Show databases to confirm
SHOW DATABASES;

