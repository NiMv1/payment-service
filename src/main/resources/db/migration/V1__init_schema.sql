-- Таблица платежей
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    order_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64),
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    external_transaction_id VARCHAR(100),
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    refunded_amount DECIMAL(19, 4),
    metadata TEXT,
    client_ip VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    expires_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Индексы для платежей
CREATE INDEX idx_payment_idempotency_key ON payments(idempotency_key);
CREATE INDEX idx_payment_order_id ON payments(order_id);
CREATE INDEX idx_payment_user_id ON payments(user_id);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_created_at ON payments(created_at);

-- Таблица транзакций
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id),
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    external_id VARCHAR(100),
    authorization_code VARCHAR(50),
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    raw_response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

-- Индексы для транзакций
CREATE INDEX idx_transaction_payment_id ON transactions(payment_id);
CREATE INDEX idx_transaction_external_id ON transactions(external_id);
CREATE INDEX idx_transaction_type ON transactions(type);
CREATE INDEX idx_transaction_created_at ON transactions(created_at);

-- Таблица записей идемпотентности
CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    payment_id UUID,
    response_status INTEGER NOT NULL,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

-- Индексы для идемпотентности
CREATE INDEX idx_idempotency_key ON idempotency_records(idempotency_key);
CREATE INDEX idx_idempotency_expires_at ON idempotency_records(expires_at);

-- Таблица кошельков
CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0,
    blocked_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    UNIQUE(user_id, currency)
);

-- Индексы для кошельков
CREATE INDEX idx_wallet_user_id ON wallets(user_id);
CREATE INDEX idx_wallet_user_currency ON wallets(user_id, currency);

-- Тестовые данные: кошельки
INSERT INTO wallets (id, user_id, currency, balance, blocked_amount, active)
VALUES 
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'user1', 'RUB', 100000.0000, 0, true),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'user1', 'USD', 1000.0000, 0, true),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'user2', 'RUB', 50000.0000, 0, true),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'user2', 'USD', 500.0000, 0, true);
