-- Create orders table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    side VARCHAR(10) NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    quantity DECIMAL(18, 4) NOT NULL,
    price DECIMAL(18, 4),
    stop_price DECIMAL(18, 4),
    filled_quantity DECIMAL(18, 4) DEFAULT 0,
    average_price DECIMAL(18, 4),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    validity VARCHAR(10) NOT NULL DEFAULT 'DAY',
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT chk_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_filled_quantity_non_negative CHECK (filled_quantity >= 0)
);

-- Indexes for common queries
CREATE INDEX idx_orders_user_id ON orders (user_id);

CREATE INDEX idx_orders_status ON orders (status);

CREATE INDEX idx_orders_symbol ON orders (symbol);

CREATE INDEX idx_orders_expires_at ON orders (expires_at);

CREATE INDEX idx_orders_created_at ON orders (created_at DESC);

-- Composite index for user's active orders
CREATE INDEX idx_orders_user_status ON orders (user_id, status);