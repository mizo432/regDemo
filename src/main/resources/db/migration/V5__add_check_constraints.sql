-- Add CHECK constraints for data integrity
-- Inventory quantity must be non-negative
ALTER TABLE inventory ADD CONSTRAINT chk_inventory_quantity CHECK (quantity >= 0);
ALTER TABLE inventory ADD CONSTRAINT chk_inventory_reserved_quantity CHECK (reserved_quantity >= 0);

-- Time sale: end date must be after start date, sale price must be positive
ALTER TABLE time_sale ADD CONSTRAINT chk_timesale_dates CHECK (end_date > start_date);
ALTER TABLE time_sale ADD CONSTRAINT chk_timesale_price CHECK (sale_price > 0);

-- Transaction item: quantity must be positive, prices must be non-negative
ALTER TABLE transaction_item ADD CONSTRAINT chk_item_quantity CHECK (quantity > 0);
ALTER TABLE transaction_item ADD CONSTRAINT chk_item_unit_price CHECK (unit_price >= 0);
ALTER TABLE transaction_item ADD CONSTRAINT chk_item_total_amount CHECK (total_amount >= 0);

-- Transaction: totals must be non-negative
ALTER TABLE transaction ADD CONSTRAINT chk_transaction_subtotal CHECK (subtotal >= 0);
ALTER TABLE transaction ADD CONSTRAINT chk_transaction_tax CHECK (tax_total >= 0);
ALTER TABLE transaction ADD CONSTRAINT chk_transaction_grand_total CHECK (grand_total >= 0);

-- Product: price must be non-negative
ALTER TABLE product ADD CONSTRAINT chk_product_price CHECK (price >= 0);

-- Commission: rate must be between 0 and 1
ALTER TABLE commission_product ADD CONSTRAINT chk_commission_rate CHECK (commission_rate IS NULL OR (commission_rate >= 0 AND commission_rate <= 1));
ALTER TABLE commission_product ADD CONSTRAINT chk_fixed_commission CHECK (fixed_commission IS NULL OR fixed_commission >= 0);

-- Sync: retry count must be non-negative
ALTER TABLE sync_outbox ADD CONSTRAINT chk_outbox_retry CHECK (retry_count >= 0);
