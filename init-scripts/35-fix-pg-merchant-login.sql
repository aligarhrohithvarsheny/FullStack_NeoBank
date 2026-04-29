-- ================================================
-- 35: FIX PG MERCHANT LOGIN - ENABLE FOR ALL EXISTING
-- ================================================

USE springapp;

-- Enable login for all existing merchants
UPDATE pg_merchants SET login_enabled = TRUE WHERE login_enabled = FALSE;
