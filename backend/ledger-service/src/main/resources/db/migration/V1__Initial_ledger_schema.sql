-- V1__Initial_ledger_schema.sql
-- IMMUTABLE ledger service schema (PostgreSQL)
-- SOURCE OF TRUTH for all financial transactions

-- =====================================================
-- LEDGER ENTRIES TABLE (IMMUTABLE - INSERT ONLY)
-- =====================================================

CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    balance_before NUMERIC(18, 4) NOT NULL,
    balance_after NUMERIC(18, 4) NOT NULL,
    reference_id VARCHAR(100),
    reference_type VARCHAR(50),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    -- NO updated_at - THIS TABLE IS IMMUTABLE
);

-- =====================================================
-- INDEXES FOR QUERY PERFORMANCE
-- =====================================================

CREATE INDEX idx_ledger_wallet_id ON ledger_entries (wallet_id);

CREATE INDEX idx_ledger_reference ON ledger_entries (reference_id, reference_type);

CREATE INDEX idx_ledger_created_at ON ledger_entries (created_at DESC);

CREATE INDEX idx_ledger_entry_type ON ledger_entries (entry_type);

CREATE INDEX idx_ledger_wallet_created ON ledger_entries (wallet_id, created_at DESC);

-- =====================================================
-- IMMUTABILITY PROTECTION
-- =====================================================

-- Create a trigger function to prevent updates
CREATE OR REPLACE FUNCTION prevent_ledger_update()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'UPDATE not allowed on ledger_entries table - ledger is immutable';
END;
$$ LANGUAGE plpgsql;

-- Create trigger to prevent updates
CREATE TRIGGER tr_prevent_ledger_update
    BEFORE UPDATE ON ledger_entries
    FOR EACH ROW
    EXECUTE FUNCTION prevent_ledger_update();

-- Create a trigger function to prevent deletes
CREATE OR REPLACE FUNCTION prevent_ledger_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'DELETE not allowed on ledger_entries table - ledger is immutable';
END;
$$ LANGUAGE plpgsql;

-- Create trigger to prevent deletes
CREATE TRIGGER tr_prevent_ledger_delete
    BEFORE DELETE ON ledger_entries
    FOR EACH ROW
    EXECUTE FUNCTION prevent_ledger_delete();

-- =====================================================
-- DOCUMENTATION
-- =====================================================

COMMENT ON TABLE ledger_entries IS 'IMMUTABLE audit trail of all financial transactions - SOURCE OF TRUTH';

COMMENT ON COLUMN ledger_entries.wallet_id IS 'Reference to wallet in funds-service';

COMMENT ON COLUMN ledger_entries.entry_type IS 'Type: DEPOSIT, WITHDRAWAL, TRADE_BUY, TRADE_SELL, LOCK, UNLOCK, etc.';

COMMENT ON COLUMN ledger_entries.balance_before IS 'Wallet available balance BEFORE this entry';

COMMENT ON COLUMN ledger_entries.balance_after IS 'Wallet available balance AFTER this entry';

COMMENT ON COLUMN ledger_entries.reference_id IS 'Reference to source: order ID, transaction ID, trade ID';

COMMENT ON COLUMN ledger_entries.reference_type IS 'Type of reference: ORDER, TRANSACTION, TRADE';

COMMENT ON COLUMN ledger_entries.created_at IS 'Immutable timestamp - when entry was created';