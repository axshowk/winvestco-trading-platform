-- V1__Initial_funds_schema.sql
-- Initial schema for funds service (PostgreSQL)
-- Manages wallets, funds locks, and transactions
-- Note: Ledger entries are stored in ledger-service (SOURCE OF TRUTH)

-- =====================================================
-- WALLETS TABLE (one wallet per user)
-- =====================================================
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    available_balance NUMERIC(18, 4) NOT NULL DEFAULT 0,
    locked_balance NUMERIC(18, 4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT chk_wallets_available_balance CHECK (available_balance >= 0),
    CONSTRAINT chk_wallets_locked_balance CHECK (locked_balance >= 0)
);

CREATE UNIQUE INDEX idx_wallets_user_id ON wallets (user_id);

CREATE INDEX idx_wallets_status ON wallets (status);

-- =====================================================
-- FUNDS LOCKS TABLE (tracks locked funds for orders)
-- =====================================================
CREATE TABLE funds_locks (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    order_id VARCHAR(100) NOT NULL UNIQUE,
    amount NUMERIC(18, 4) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT chk_locks_amount CHECK (amount > 0)
);

CREATE INDEX idx_locks_wallet_id ON funds_locks (wallet_id);

CREATE UNIQUE INDEX idx_locks_order_id ON funds_locks (order_id);

CREATE INDEX idx_locks_status ON funds_locks (status);

-- =====================================================
-- TRANSACTIONS TABLE (deposits/withdrawals)
-- =====================================================
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    external_reference VARCHAR(200),
    description VARCHAR(500),
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT chk_tx_amount CHECK (amount > 0)
);

CREATE INDEX idx_tx_wallet_id ON transactions (wallet_id);

CREATE INDEX idx_tx_external_ref ON transactions (external_reference);

CREATE INDEX idx_tx_status ON transactions (status);

CREATE INDEX idx_tx_created_at ON transactions (created_at DESC);

CREATE INDEX idx_tx_type ON transactions (transaction_type);

-- =====================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================
COMMENT ON TABLE wallets IS 'User cash balances with available and locked amounts';

COMMENT ON TABLE funds_locks IS 'Tracks funds locked for pending orders';

COMMENT ON TABLE transactions IS 'Deposit and withdrawal transactions';

-- NOTE: Ledger entries are stored in the ledger-service (SOURCE OF TRUTH)
-- All financial transactions are recorded via Feign client to ledger-service