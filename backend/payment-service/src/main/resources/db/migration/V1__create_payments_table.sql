-- V1: Create payments table for Razorpay integration
-- Payment lifecycle: CREATED -> INITIATED -> PENDING -> SUCCESS/FAILED/EXPIRED

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_id BIGINT,
    amount NUMERIC(18,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    payment_method VARCHAR(20),
    provider VARCHAR(20) NOT NULL DEFAULT 'RAZORPAY',

-- Razorpay specific fields
razorpay_order_id VARCHAR(100),
razorpay_payment_id VARCHAR(100),
razorpay_signature VARCHAR(255),

-- Transaction metadata
receipt VARCHAR(100),
description VARCHAR(500),
failure_reason VARCHAR(500),
error_code VARCHAR(50),

-- Timestamps
expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_payment_user_id ON payments (user_id);

CREATE INDEX idx_payment_status ON payments (status);

CREATE INDEX idx_payment_razorpay_order ON payments (razorpay_order_id);

CREATE INDEX idx_payment_razorpay_payment ON payments (razorpay_payment_id);

CREATE INDEX idx_payment_expires_at ON payments (expires_at);

CREATE INDEX idx_payment_created_at ON payments (created_at);

-- Composite index for expiry scheduler
CREATE INDEX idx_payment_status_expires ON payments (status, expires_at);