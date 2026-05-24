-- Migration Script: Add City and State columns, Remove Location column
-- For: Transaction Service Database
-- Date: 2026-04-10

-- Add new columns (if they don't exist)
ALTER TABLE transactions ADD COLUMN city VARCHAR(100);
ALTER TABLE transactions ADD COLUMN state VARCHAR(2);

-- Drop old column (after verifying data migration)
-- ALTER TABLE transactions DROP COLUMN location;

-- Add NOT NULL constraints if needed
-- ALTER TABLE transactions MODIFY city VARCHAR(100) NOT NULL;
-- ALTER TABLE transactions MODIFY state VARCHAR(2) NOT NULL;

-- Create indexes for better query performance
CREATE INDEX idx_transactions_city ON transactions(city);
CREATE INDEX idx_transactions_state ON transactions(state);
CREATE INDEX idx_transactions_city_state ON transactions(city, state);
