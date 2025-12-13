-- V1: Initial notification schema

-- Notification type enum
CREATE TYPE notification_type AS ENUM (
    'ORDER_CREATED',
    'ORDER_VALIDATED',
    'ORDER_CANCELLED',
    'ORDER_REJECTED',
    'ORDER_EXPIRED',
    'ORDER_PARTIALLY_FILLED',
    'ORDER_FILLED',
    'TRADE_EXECUTED',
    'FUNDS_LOCKED',
    'FUNDS_RELEASED',
    'FUNDS_DEPOSITED',
    'FUNDS_WITHDRAWN',
    'USER_LOGIN',
    'USER_PASSWORD_CHANGED',
    'USER_STATUS_CHANGED'
);

-- Notification status enum
CREATE TYPE notification_status AS ENUM (
    'UNREAD',
    'READ',
    'ARCHIVED'
);

-- Notifications table
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type notification_type NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    data JSONB,
    status notification_status NOT NULL DEFAULT 'UNREAD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    CONSTRAINT notifications_user_id_idx UNIQUE (id, user_id)
);

-- Index for fetching user notifications
CREATE INDEX idx_notifications_user_status ON notifications (
    user_id,
    status,
    created_at DESC
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);

-- Notification preferences table
CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    mute_all BOOLEAN NOT NULL DEFAULT FALSE,
    muted_types TEXT [], -- Array of muted notification types
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for fetching user preferences
CREATE INDEX idx_notification_preferences_user ON notification_preferences (user_id);