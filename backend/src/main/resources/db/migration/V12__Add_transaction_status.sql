-- Add new ENUM for transaction status
CREATE TYPE "transaction_status" AS ENUM (
    'PENDING',
    'SUCCEEDED',
    'FAILED'
);

-- Add 'status' column to the 'transaction' table
ALTER TABLE transactions
ADD COLUMN status transaction_status NOT NULL DEFAULT 'PENDING';

-- Add an index for transaction status for faster lookup
CREATE INDEX idx_transaction_status ON transactions (status);