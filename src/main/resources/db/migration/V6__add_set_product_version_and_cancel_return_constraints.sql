-- Add version column to set_product for optimistic locking (M-1)
ALTER TABLE set_product ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add NOT NULL constraint to cancel_return.return_amount for audit integrity (M-2)
-- Set existing NULL values to 0 first, then add the constraint
UPDATE cancel_return SET return_amount = 0 WHERE return_amount IS NULL;
ALTER TABLE cancel_return ALTER COLUMN return_amount SET NOT NULL;
