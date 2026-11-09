-- Add version column to inventory table for optimistic locking
ALTER TABLE inventory ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
