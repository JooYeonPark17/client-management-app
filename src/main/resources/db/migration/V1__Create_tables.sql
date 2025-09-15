-- Members
CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    gender VARCHAR(255) NOT NULL,
    birth_date VARCHAR(255) NOT NULL,
    member_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    withdrawn_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_members_user_id UNIQUE (user_id),
    CONSTRAINT ck_members_member_status CHECK (member_status IN ('ACTIVE', 'WITHDRAWN', 'PENDING_WITHDRAWAL'))
);

-- Products
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT ck_products_status CHECK (status IN ('AVAILABLE', 'UNAVAILABLE'))
);

-- Orders
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id),
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    payment_id BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED'))
);

-- Order Items
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL
);

-- Payments
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    amount DECIMAL(10,2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(255),
    transaction_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_payments_payment_status CHECK (payment_status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED'))
);

CREATE TABLE idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

ALTER TABLE orders ADD CONSTRAINT fk_orders_payment_id
    FOREIGN KEY (payment_id) REFERENCES payments(id);

ALTER TABLE payments ADD CONSTRAINT uk_payments_order_id UNIQUE (order_id);
