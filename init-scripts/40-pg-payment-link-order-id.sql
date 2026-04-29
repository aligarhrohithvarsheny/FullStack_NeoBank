-- Add order_id column to pg_payment_links for linking payment requests to PG orders
ALTER TABLE pg_payment_links ADD COLUMN IF NOT EXISTS order_id VARCHAR(50);
CREATE INDEX IF NOT EXISTS idx_pg_payment_links_order_id ON pg_payment_links(order_id);
