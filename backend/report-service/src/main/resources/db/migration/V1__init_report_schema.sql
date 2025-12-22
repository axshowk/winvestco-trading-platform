-- =====================================================
-- Report Service Database Schema
-- V1: Initial schema for reports and event projections
-- =====================================================

-- =====================================================
-- REPORTS TABLE
-- Stores metadata about generated reports
-- =====================================================
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    report_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    report_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    format VARCHAR(10) NOT NULL,
    from_date TIMESTAMP,
    to_date TIMESTAMP,
    file_path VARCHAR(500),
    file_size_bytes BIGINT,
    error_message TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT uk_reports_report_id UNIQUE (report_id)
);

CREATE INDEX idx_reports_user_id ON reports (user_id);

CREATE INDEX idx_reports_status ON reports (status);

CREATE INDEX idx_reports_requested_at ON reports (requested_at);

CREATE INDEX idx_reports_type_user ON reports (report_type, user_id);

-- =====================================================
-- EVENT PROJECTION TABLES (Event Sourcing)
-- Local projections built from domain events
-- =====================================================

-- Trade Projection - populated from TradeExecutedEvent, TradeClosedEvent
CREATE TABLE trade_projections (
    id BIGSERIAL PRIMARY KEY,
    trade_id VARCHAR(36) NOT NULL,
    order_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    side VARCHAR(10) NOT NULL,
    quantity DECIMAL(18, 4) NOT NULL,
    price DECIMAL(18, 4) NOT NULL,
    executed_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_trade_projections_trade_id UNIQUE (trade_id)
);

CREATE INDEX idx_trade_proj_user_id ON trade_projections (user_id);

CREATE INDEX idx_trade_proj_symbol ON trade_projections (symbol);

CREATE INDEX idx_trade_proj_executed_at ON trade_projections (executed_at);

CREATE INDEX idx_trade_proj_user_date ON trade_projections (user_id, executed_at);

-- Holding Projection - populated from TradeExecutedEvent, user.created (portfolio init)
CREATE TABLE holding_projections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    quantity DECIMAL(18, 4) NOT NULL DEFAULT 0,
    average_price DECIMAL(18, 4) NOT NULL DEFAULT 0,
    total_invested DECIMAL(18, 4) NOT NULL DEFAULT 0,
    last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_holding_proj_user_symbol UNIQUE (user_id, symbol)
);

CREATE INDEX idx_holding_proj_user_id ON holding_projections (user_id);

-- Ledger Projection - populated from FundsDepositedEvent, FundsWithdrawnEvent, etc.
CREATE TABLE ledger_projections (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    entry_type VARCHAR(30) NOT NULL,
    amount DECIMAL(18, 4) NOT NULL,
    balance_before DECIMAL(18, 4) NOT NULL,
    balance_after DECIMAL(18, 4) NOT NULL,
    reference_id VARCHAR(100),
    reference_type VARCHAR(50),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ledger_proj_user_id ON ledger_projections (user_id);

CREATE INDEX idx_ledger_proj_wallet_id ON ledger_projections (wallet_id);

CREATE INDEX idx_ledger_proj_created_at ON ledger_projections (created_at);

CREATE INDEX idx_ledger_proj_entry_type ON ledger_projections (entry_type);

CREATE INDEX idx_ledger_proj_user_date ON ledger_projections (user_id, created_at);

-- User Wallet Projection - current wallet state
CREATE TABLE wallet_projections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT NOT NULL,
    available_balance DECIMAL(18, 4) NOT NULL DEFAULT 0,
    locked_balance DECIMAL(18, 4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_wallet_proj_user_id UNIQUE (user_id)
);

-- Event Processing Log - tracks processed events for idempotency
CREATE TABLE processed_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_processed_events_id UNIQUE (event_id)
);

CREATE INDEX idx_processed_events_type ON processed_events (event_type);

CREATE INDEX idx_processed_events_processed_at ON processed_events (processed_at);