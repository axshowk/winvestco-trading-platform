-- V1__create_trades_table.sql
-- Trade entity table for tracking trade lifecycle

CREATE TABLE trades (
    id BIGSERIAL PRIMARY KEY,
    trade_id VARCHAR(36) NOT NULL UNIQUE,
    order_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    side VARCHAR(10) NOT NULL,
    trade_type VARCHAR(20) NOT NULL,
    quantity DECIMAL(18, 4) NOT NULL,
    price DECIMAL(18, 4),
    executed_quantity DECIMAL(18, 4) DEFAULT 0,
    average_price DECIMAL(18, 4),
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    validated_at TIMESTAMP,
    placed_at TIMESTAMP,
    executed_at TIMESTAMP,
    closed_at TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_trades_trade_id ON trades (trade_id);

CREATE INDEX idx_trades_order_id ON trades (order_id);

CREATE INDEX idx_trades_user_id ON trades (user_id);

CREATE INDEX idx_trades_status ON trades (status);

CREATE INDEX idx_trades_symbol ON trades (symbol);

CREATE INDEX idx_trades_user_status ON trades (user_id, status);

CREATE INDEX idx_trades_created_at ON trades (created_at DESC);