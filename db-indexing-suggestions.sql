-- Optional performance indexes for MySQL (safe to apply once).
-- Run on database: frauddb

CREATE INDEX idx_transactions_status ON transaction (status);
CREATE INDEX idx_transactions_time ON transaction (transaction_time);
CREATE INDEX idx_transactions_account_number ON transaction (account_number);

-- Optional composite index for common admin filter pattern:
CREATE INDEX idx_transactions_status_time_account ON transaction (status, transaction_time, account_number);
