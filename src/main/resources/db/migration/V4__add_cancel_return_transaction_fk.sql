-- Add transaction_id FK and return_amount to cancel_return table for better audit trail
ALTER TABLE cancel_return ADD COLUMN transaction_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cancel_return ADD CONSTRAINT fk_cancel_return_transaction FOREIGN KEY (transaction_id) REFERENCES transaction(id);
ALTER TABLE cancel_return ADD COLUMN return_amount DECIMAL(12, 2);

CREATE INDEX idx_cancel_return_transaction ON cancel_return(transaction_id);
