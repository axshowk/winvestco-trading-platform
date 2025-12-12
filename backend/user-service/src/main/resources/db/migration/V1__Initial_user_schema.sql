-- V1__Initial_user_schema.sql
-- Initial schema for user service (PostgreSQL)

-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    client_id VARCHAR(20) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    last_login_at TIMESTAMP NULL
);

-- Create indexes for users table
CREATE INDEX idx_users_email ON users (email);

CREATE INDEX idx_users_client_id ON users (client_id);

CREATE INDEX idx_users_status ON users (status);

CREATE INDEX idx_users_created_at ON users (created_at);

-- Create users_roles table for many-to-many relationship between users and roles
CREATE TABLE users_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Create indexes for users_roles table
CREATE INDEX idx_users_roles_user_id ON users_roles (user_id);

CREATE INDEX idx_users_roles_role ON users_roles (role);